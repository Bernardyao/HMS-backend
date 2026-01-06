package com.his.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.his.entity.AuditLogEntity;

/**
 * 审计日志查询服务接口
 *
 * <p>提供审计日志的查询功能,支持分页、过滤、排序等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>综合查询</b>：支持多条件组合查询（模块、操作、操作人、类型、时间范围）</li>
 *   <li><b>TraceId查询</b>：根据TraceId查询整个请求链路的审计日志</li>
 *   <li><b>操作人查询</b>：查询某个用户的所有审计日志</li>
 *   <li><b>分页支持</b>：所有查询都支持分页和排序</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 综合查询：查询某个时间范围内、特定模块的审计日志
 * Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
 * Page&lt;AuditLogEntity&gt; logs = auditLogQueryService.searchAuditLogs(
 *     "挂号管理", null, null, null,
 *     LocalDateTime.now().minusDays(7), LocalDateTime.now(),
 *     pageable
 * );
 *
 * // 根据TraceId查询
 * List&lt;AuditLogEntity&gt; logs = auditLogQueryService.getAuditLogsByTraceId(traceId);
 *
 * // 查询某个用户的所有审计日志
 * List&lt;AuditLogEntity&gt; logs = auditLogQueryService.getAuditLogsByOperator(userId);
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.entity.AuditLogEntity
 */
public interface AuditLogQueryService {

    /**
     * 综合查询审计日志（支持分页）
     *
     * <p>支持多条件组合查询,所有参数都是可选的</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li><b>module</b>：按模块查询（如"认证管理"、"挂号管理"）</li>
     *   <li><b>action</b>：按操作查询（如"用户登录"、"患者挂号"）</li>
     *   <li><b>operatorUsername</b>：按操作人用户名查询</li>
     *   <li><b>auditType</b>：按审计类型查询（SENSITIVE_OPERATION、BUSINESS、DATA_ACCESS）</li>
     *   <li><b>startTime</b>：查询开始时间</li>
     *   <li><b>endTime</b>：查询结束时间</li>
     * </ul>
     *
     * <p><b>分页和排序：</b></p>
     * <ul>
     *   <li>使用Pageable指定页码、每页大小、排序规则</li>
     *   <li>建议按createTime倒序排列（最新的在前）</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 查询最近7天的所有审计日志（分页）
     * Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
     * Page&lt;AuditLogEntity&gt; logs = auditLogQueryService.searchAuditLogs(
     *     null, null, null, null,
     *     LocalDateTime.now().minusDays(7), LocalDateTime.now(),
     *     pageable
     * );
     *
     * // 查询特定模块的敏感操作日志
     * Page&lt;AuditLogEntity&gt; logs = auditLogQueryService.searchAuditLogs(
     *     "认证管理", null, null, "SENSITIVE_OPERATION",
     *     null, null, pageable
     * );
     * </pre>
     *
     * @param module 模块名称(可选)
     * @param action 操作描述(可选)
     * @param operatorUsername 操作人用户名(可选)
     * @param auditType 审计类型(可选)
     * @param startTime 查询开始时间(可选)
     * @param endTime 查询结束时间(可选)
     * @param pageable 分页和排序参数
     * @return 分页查询结果
     * @since 1.0
     */
    Page<AuditLogEntity> searchAuditLogs(
        String module,
        String action,
        String operatorUsername,
        String auditType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * 根据TraceId查询审计日志
     *
     * <p>用于查询整个请求链路的所有审计日志</p>
     * <p>TraceId由MDC自动生成,在同一个请求链路中保持一致</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>问题排查：查看某个请求的完整执行过程</li>
     *   <li>性能分析：分析某个请求的各阶段耗时</li>
     *   <li>审计追溯：追溯某个操作的所有相关日志</li>
     * </ul>
     *
     * @param traceId 链路追踪ID(32位十六进制字符串)
     * @return 该TraceId对应的所有审计日志(按创建时间倒序)
     * @since 1.0
     */
    List<AuditLogEntity> getAuditLogsByTraceId(String traceId);

    /**
     * 查询操作人的审计日志
     *
     * <p>查询某个用户的所有审计日志,包括敏感操作和业务操作</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>用户行为审计：审计某个用户的所有操作</li>
     *   <li>安全调查：调查可疑用户的行为</li>
     *   <li>工作量统计：统计用户的操作次数</li>
     * </ul>
     *
     * @param operatorId 操作人ID(sys_user.id)
     * @return 该操作人的所有审计日志(按创建时间倒序)
     * @since 1.0
     */
    List<AuditLogEntity> getAuditLogsByOperator(Long operatorId);
}
