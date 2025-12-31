package com.his.log.aspect;

import com.his.log.annotation.AuditLog;
import com.his.log.utils.LogUtils;
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
@Order(20)  // 在 ApiLogAspect 之后执行
public class AuditLogAspect {

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
    }

    /**
     * 保存审计日志到数据库（可选功能）
     *
     * <p><b>实现优先级：</b>P1 - 应该实现（用于合规审计）
     *
     * <p><b>实现步骤：</b>
     * <ol>
     *   <li>创建 {@code AuditLogEntity} 实体类</li>
     *   <li>创建 {@code AuditLogRepository} 接口</li>
     *   <li>注入 Repository 并实现持久化逻辑</li>
     *   <li>在 {@link #logSuccess} 和 {@link #logFailure} 中调用此方法</li>
     * </ol>
     *
     * <p><b>实体类设计建议：</b>
     * <pre>{@code
     * @Entity
     * @Table(name = "sys_audit_log")
     * public class AuditLogEntity {
     *     @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
     *     private Long id;
     *
     *     private String module;           // 模块名
     *     private String action;           // 操作名
     *     private String auditType;        // 审计类型
     *     private String description;      // 描述
     *
     *     // 操作人信息
     *     private Long operatorId;
     *     private String operatorUsername;
     *
     *     // 请求信息
     *     private String traceId;
     *     private String requestIp;
     *     private String userAgent;
     *
     *     // 执行结果
     *     private String status;           // SUCCESS / FAILURE
     *     private Long executionTime;      // 执行时间(ms)
     *
     *     // 异常信息（仅失败时）
     *     private String exceptionType;
     *     private String exceptionMessage;
     *
     *     // 时间戳
     *     @CreationTimestamp
     *     private LocalDateTime createTime;
     * }
     * }</pre>
     *
     * <p><b>示例实现：</b>
     * <pre>{@code
     * @Autowired
     * private AuditLogRepository auditLogRepository;
     *
     * private void saveAuditLogToDatabase(AuditLog auditLog, long startTime, Throwable exception) {
     *     // 1. 构建实体
     *     AuditLogEntity entity = new AuditLogEntity();
     *     entity.setModule(auditLog.module());
     *     entity.setAction(auditLog.action());
     *     entity.setAuditType(auditLog.auditType().name());
     *     entity.setDescription(auditLog.description());
     *
     *     // 2. 设置操作人信息（从 SecurityContext 获取）
     *     SecurityContext context = SecurityContextHolder.getContext();
     *     if (context.getAuthentication() != null) {
     *         UserDetails userDetails = (UserDetails) context.getAuthentication().getPrincipal();
     *         entity.setOperatorId(userDetails.getId());
     *         entity.setOperatorUsername(userDetails.getUsername());
     *     }
     *
     *     // 3. 设置请求信息
     *     entity.setTraceId(MDC.get("traceId"));
     *     HttpServletRequest request =
     *         ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
     *     entity.setRequestIp(request.getRemoteAddr());
     *     entity.setUserAgent(request.getHeader("User-Agent"));
     *
     *     // 4. 设置执行结果
     *     entity.setStatus(exception == null ? "SUCCESS" : "FAILURE");
     *     entity.setExecutionTime(System.currentTimeMillis() - startTime);
     *
     *     // 5. 记录异常信息（仅失败时）
     *     if (exception != null) {
     *         entity.setExceptionType(exception.getClass().getSimpleName());
     *         // 限制异常消息长度，避免过长
     *         String message = exception.getMessage();
     *         entity.setExceptionMessage(message != null && message.length() > 1000
     *             ? message.substring(0, 1000) + "..." : message);
     *     }
     *
     *     // 6. 异步保存（避免影响业务性能）
     *     CompletableFuture.runAsync(() -> auditLogRepository.save(entity));
     * }
     * }</pre>
     *
     * <p><b>调用位置：</b>
     * <ul>
     *   <li>在 {@link #logSuccess(AuditLog, long)} 末尾调用</li>
     *   <li>在 {@link #logFailure(AuditLog, long, Throwable)} 末尾调用</li>
     * </ul>
     *
     * @see AuditLog
     *see org.springframework.scheduling.annotation.Async
     */
    @Deprecated
    @SuppressWarnings("unused")
    private void saveAuditLogToDatabase(AuditLog auditLog, long startTime, Throwable exception) {
        // TODO: P1 优先级 - 实现审计日志持久化
        //
        // 实现步骤：
        // 1. 创建 AuditLogEntity 实体类（参考方法注释中的设计）
        // 2. 创建 AuditLogRepository 接口
        // 3. 注入 Repository 并实现持久化逻辑
        // 4. 在 logSuccess 和 logFailure 中调用此方法
        // 5. 配置 @EnableAsync 支持异步保存
        //
        // 注意事项：
        // - 使用异步保存避免影响业务性能
        // - 限制异常消息长度避免数据库字段过长
        // - 考虑定期归档历史数据（如保留6个月）
        //
        // 目前只记录到日志文件，后续实现数据库存储以支持：
        // - HIPAA 合规审计要求
        // - 等保三级审计要求
        // - 运营审计报表
    }
}
