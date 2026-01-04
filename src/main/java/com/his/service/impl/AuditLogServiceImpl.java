package com.his.service.impl;

import com.his.common.SecurityUtils;
import com.his.entity.AuditLogEntity;
import com.his.repository.AuditLogRepository;
import com.his.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 审计日志服务实现类
 *
 * <p>提供审计日志的异步保存和构建功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>异步保存</b>：使用@Async注解实现异步保存,不阻塞业务线程</li>
 *   <li><b>构建实体</b>：自动提取操作人信息、请求信息、异常信息等</li>
 *   <li><b>容错处理</b>：保存失败时记录日志,不影响业务操作</li>
 * </ul>
 *
 * <h3>线程池配置</h3>
 * <ul>
 *   <li><b>线程池名称</b>：auditLogExecutor</li>
 *   <li><b>核心线程数</b>：2</li>
 *   <li><b>最大线程数</b>：5</li>
 *   <li><b>队列容量</b>：100</li>
 *   <li><b>拒绝策略</b>：CallerRunsPolicy(调用者线程执行)</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.AuditLogService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 异步保存审计日志
     *
     * <p>使用auditLogExecutor线程池异步保存,避免阻塞业务线程</p>
     * <p>即使保存失败也不抛出异常,只记录错误日志</p>
     *
     * @param entity 审计日志实体
     * @return CompletableFuture<Void>,可用于等待保存完成或添加回调
     */
    @Async("auditLogExecutor")
    @Override
    public CompletableFuture<Void> saveAuditLogAsync(AuditLogEntity entity) {
        try {
            auditLogRepository.save(entity);
            log.debug("审计日志保存成功: id={}, module={}, action={}",
                    entity.getId(), entity.getModule(), entity.getAction());
        } catch (Exception e) {
            log.error("审计日志保存失败: module={}, action={}, error={}",
                    entity.getModule(), entity.getAction(), e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 构建审计日志实体
     *
     * <p>自动填充以下信息：</p>
     * <ul>
     *   <li><b>操作人信息</b>：从SecurityContextHolder获取当前用户ID和用户名</li>
     *   <li><b>请求信息</b>：从RequestContextHolder获取TraceId、IP、User-Agent</li>
     *   <li><b>异常信息</b>：如果有异常,记录异常类型和消息(限制1000字符)</li>
     * </ul>
     *
     * @param module 模块名称
     * @param action 操作描述
     * @param auditType 审计类型
     * @param description 操作描述详情
     * @param status 执行状态
     * @param executionTime 执行时间(毫秒)
     * @param exception 异常对象
     * @return 构建完成的AuditLogEntity实体
     */
    @Override
    public AuditLogEntity buildAuditLogEntity(
            String module, String action, String auditType, String description,
            String status, long executionTime, Throwable exception) {

        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule(module);
        entity.setAction(action);
        entity.setAuditType(auditType);
        entity.setDescription(description);
        entity.setStatus(status);
        entity.setExecutionTime(executionTime);

        // 获取操作人信息(如果用户已登录)
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String username = SecurityUtils.getCurrentUsername();
            entity.setOperatorId(userId);
            entity.setOperatorUsername(username);
            log.debug("获取操作人信息成功: userId={}, username={}", userId, username);
        } catch (IllegalStateException e) {
            // 用户未登录(如登录操作本身),设置operatorUsername为"anonymous"
            log.debug("获取操作人信息失败,可能是登录操作: {}", e.getMessage());
            entity.setOperatorUsername("anonymous");
        }

        // 获取请求信息(如果在Web请求上下文中)
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setRequestIp(getClientIp(request));
            entity.setUserAgent(request.getHeader("User-Agent"));
            log.debug("获取请求信息成功: ip={}, userAgent={}",
                    entity.getRequestIp(),
                    entity.getUserAgent() != null
                            ? entity.getUserAgent().substring(0, Math.min(50, entity.getUserAgent().length()))
                            : null);
        }

        // 获取TraceId(如果MDC中存在)
        String traceId = org.slf4j.MDC.get("traceId");
        entity.setTraceId(traceId);

        // 记录异常信息(如果操作失败)
        if (exception != null) {
            entity.setExceptionType(exception.getClass().getSimpleName());
            String message = exception.getMessage();
            // 限制异常消息长度为1000字符
            if (message != null && message.length() > 1000) {
                entity.setExceptionMessage(message.substring(0, 1000) + "...");
            } else {
                entity.setExceptionMessage(message);
            }
            log.debug("记录异常信息: type={}, message={}",
                    entity.getExceptionType(), entity.getExceptionMessage());
        }

        return entity;
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>支持反向代理,按以下优先级获取：</p>
     * <ol>
     *   <li>X-Forwarded-For请求头(第一个IP)</li>
     *   <li>Proxy-Client-IP请求头</li>
     *   <li>RemoteAddr(远程地址)</li>
     * </ol>
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址,如果无法获取返回"unknown"
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况(取第一个)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}
