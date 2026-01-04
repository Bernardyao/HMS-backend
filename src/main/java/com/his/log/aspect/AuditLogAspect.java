package com.his.log.aspect;

import com.his.entity.AuditLogEntity;
import com.his.log.annotation.AuditLog;
import com.his.log.utils.LogUtils;
import com.his.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 审计日志切面
 *
 * 功能：
 * 1. 拦截所有标记了 @AuditLog 的方法
 * 2. 记录业务操作的审计信息
 * 3. 支持成功/失败分别记录
 * 4. 自动记录操作人信息
 *
 * 日志示例：
 * <pre>
 * [审计日志] ✅ 模块: 患者管理 | 操作: 创建患者 | 操作人: 用户(123, doctor001) | 耗时: 45ms
 * [审计日志] ❌ 模块: 处方管理 | 操作: 开具处方 | 操作人: 用户(456, doctor002) | 耗时: 120ms | 异常: IllegalArgumentException
 * </pre>
 *
 * 使用场景：
 * - 满足合规要求（HIPAA、等保三级）
 * - 关键业务操作追溯
 * - 安全审计
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(20)  // 在 ApiLogAspect 之后执行
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * 拦截所有标记了 @AuditLog 的方法
     */
    @Pointcut("@annotation(com.his.log.annotation.AuditLog)")
    public void auditLogPointcut() {
        // Pointcut 定义
    }

    /**
     * 环绕通知：记录审计日志
     */
    @Around("auditLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog auditLog = signature.getMethod().getAnnotation(AuditLog.class);

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        Object result = null;

        try {
            // 3. 执行目标方法
            result = joinPoint.proceed();

            // 4. 记录成功操作日志
            if (auditLog.recordSuccess()) {
                long executionTime = System.currentTimeMillis() - startTime;
                logSuccess(auditLog, executionTime);
            }

            return result;

        } catch (Throwable e) {
            // 5. 捕获异常

            // 6. 记录失败操作日志
            if (auditLog.recordFailure()) {
                long executionTime = System.currentTimeMillis() - startTime;
                logFailure(auditLog, executionTime, e);
            }

            throw e;
        }
    }

    /**
     * 记录成功的审计日志
     */
    private void logSuccess(AuditLog auditLog, long executionTime) {
        log.info("[审计日志] ✅ 模块: {} | 操作: {} | 描述: {} | 类型: {} | 耗时: {}ms",
                auditLog.module(),
                auditLog.action(),
                auditLog.description(),
                auditLog.auditType().getDescription(),
                executionTime);

        // 同时使用 LogUtils 记录（统一格式）
        LogUtils.logBusinessOperation(
                auditLog.module(),
                auditLog.action(),
                auditLog.description().isEmpty() ?
                    String.format("类型: %s, 耗时: %dms", auditLog.auditType().getDescription(), executionTime) :
                    auditLog.description()
        );

        // 持久化到数据库
        saveAuditLogToDatabase(auditLog, executionTime, null);
    }

    /**
     * 记录失败的审计日志
     */
    private void logFailure(AuditLog auditLog, long executionTime, Throwable exception) {
        log.error("[审计日志] ❌ 模块: {} | 操作: {} | 描述: {} | 类型: {} | 耗时: {}ms | 异常: {} | 消息: {}",
                auditLog.module(),
                auditLog.action(),
                auditLog.description(),
                auditLog.auditType().getDescription(),
                executionTime,
                exception.getClass().getSimpleName(),
                exception.getMessage());

        // 同时使用 LogUtils 记录（统一格式）
        LogUtils.logSystemError(
                auditLog.module(),
                String.format("%s - %s", auditLog.action(), auditLog.description()),
                exception
        );

        // 持久化到数据库
        saveAuditLogToDatabase(auditLog, executionTime, exception);
    }

    /**
     * 保存审计日志到数据库
     *
     * <p>使用AuditLogService构建审计日志实体并异步保存到数据库</p>
     *
     * <p><b>实现说明：</b></p>
     * <ul>
     *   <li>自动提取操作人信息（用户ID、用户名）</li>
     *   <li>自动提取请求信息（TraceId、IP、User-Agent）</li>
     *   <li>记录执行状态和耗时</li>
     *   <li>失败时记录异常类型和消息</li>
     *   <li>使用auditLogExecutor线程池异步保存</li>
     * </ul>
     *
     * @param auditLog 审计日志注解
     * @param executionTime 执行时间(毫秒)
     * @param exception 异常对象(成功时为null)
     * @since 1.0
     */
    private void saveAuditLogToDatabase(AuditLog auditLog, long executionTime, Throwable exception) {
        try {
            // 构建审计日志实体
            AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                    auditLog.module(),
                    auditLog.action(),
                    auditLog.auditType().name(),
                    auditLog.description(),
                    exception == null ? "SUCCESS" : "FAILURE",
                    executionTime,
                    exception
            );

            // 异步保存到数据库
            auditLogService.saveAuditLogAsync(entity);

        } catch (Exception e) {
            log.error("构建或保存审计日志实体失败: module={}, action={}, error={}",
                    auditLog.module(), auditLog.action(), e.getMessage(), e);
        }
    }
}
