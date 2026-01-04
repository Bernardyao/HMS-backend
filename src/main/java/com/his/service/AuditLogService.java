package com.his.service;

import com.his.entity.AuditLogEntity;

import java.util.concurrent.CompletableFuture;

/**
 * 审计日志服务接口
 *
 * <p>提供审计日志的保存和构建功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>异步保存</b>：使用CompletableFuture异步保存审计日志,不阻塞业务线程</li>
 *   <li><b>构建实体</b>：根据操作信息构建完整的AuditLogEntity实体</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 异步保存审计日志
 * AuditLogEntity entity = auditLogService.buildAuditLogEntity(
 *     "认证管理", "用户登录", "SENSITIVE_OPERATION",
 *     "用户登录系统", "SUCCESS", 100L, null
 * );
 * auditLogService.saveAuditLogAsync(entity);
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.entity.AuditLogEntity
 */
public interface AuditLogService {

    /**
     * 异步保存审计日志
     *
     * <p>使用异步方式保存审计日志到数据库,避免阻塞业务线程</p>
     * <p>即使保存失败也不影响业务操作,只记录错误日志</p>
     *
     * <p><b>实现说明：</b></p>
     * <ul>
     *   <li>使用@Async注解实现异步执行</li>
     *   <li>使用auditLogExecutor线程池(核心2,最大5,队列100)</li>
     *   <li>保存失败时记录error日志,不抛出异常</li>
     * </ul>
     *
     * @param entity 审计日志实体
     * @return CompletableFuture<Void>,可用于等待保存完成或添加回调
     * @since 1.0
     */
    CompletableFuture<Void> saveAuditLogAsync(AuditLogEntity entity);

    /**
     * 构建审计日志实体
     *
     * <p>根据操作信息自动构建完整的AuditLogEntity实体,包括：</p>
     * <ul>
     *   <li>操作人信息：从SecurityContextHolder获取当前用户ID和用户名</li>
     *   <li>请求信息：从RequestContextHolder获取TraceId、IP、User-Agent</li>
     *   <li>异常信息：如果有异常,记录异常类型和消息(限制1000字符)</li>
     * </ul>
     *
     * <p><b>自动填充的字段：</b></p>
     * <ul>
     *   <li>operatorId - 当前登录用户ID</li>
     *   <li>operatorUsername - 当前登录用户名</li>
     *   <li>traceId - MDC中的traceId</li>
     *   <li>requestIp - 客户端真实IP(支持反向代理)</li>
     *   <li>userAgent - User-Agent请求头</li>
     *   <li>createTime - 自动设置为当前时间</li>
     * </ul>
     *
     * @param module 模块名称(如"认证管理"、"挂号管理")
     * @param action 操作描述(如"用户登录"、"患者挂号")
     * @param auditType 审计类型(如"SENSITIVE_OPERATION"、"BUSINESS")
     * @param description 操作描述详情
     * @param status 执行状态("SUCCESS"或"FAILURE")
     * @param executionTime 执行时间(毫秒)
     * @param exception 异常对象(如果操作失败),成功时传null
     * @return 构建完成的AuditLogEntity实体
     * @since 1.0
     */
    AuditLogEntity buildAuditLogEntity(
        String module,
        String action,
        String auditType,
        String description,
        String status,
        long executionTime,
        Throwable exception
    );
}
