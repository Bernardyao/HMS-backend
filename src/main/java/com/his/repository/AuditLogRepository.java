package com.his.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.his.entity.AuditLogEntity;

/**
 * 审计日志数据访问接口
 *
 * <p>提供审计日志的CRUD操作和常用查询方法</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>基础操作</b>：增删改查(CRUD)</li>
 *   <li><b>按操作人查询</b>：查询某个用户的所有审计日志</li>
 *   <li><b>按时间范围查询</b>：查询指定时间段的审计日志</li>
 *   <li><b>按模块查询</b>：查询某个业务模块的审计日志</li>
 *   <li><b>按TraceId查询</b>：查询整个请求链路的审计日志</li>
 *   <li><b>按审计类型查询</b>：查询敏感操作、业务操作等</li>
 *   <li><b>批量删除</b>：删除指定时间之前的审计日志(用于归档)</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 查询某个用户最近30天的操作记录
 * LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
 * List&lt;AuditLogEntity&gt; logs = auditLogRepository.findByOperatorIdOrderByCreateTimeDesc(userId);
 *
 * // 根据TraceId查询整个请求链路的审计日志
 * List&lt;AuditLogEntity&gt; logs = auditLogRepository.findByTraceIdOrderByCreateTimeDesc(traceId);
 *
 * // 清理180天前的审计日志
 * LocalDateTime cutoffDate = LocalDateTime.now().minusDays(180);
 * auditLogRepository.deleteByCreateTimeBefore(cutoffDate);
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.entity.AuditLogEntity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    /**
     * 查询指定操作人的审计日志(按创建时间倒序)
     *
     * <p>用于审计某个用户的所有操作记录,包括敏感操作和业务操作</p>
     *
     * @param operatorId 操作人ID(sys_user.id)
     * @return 审计日志列表,按创建时间倒序排列
     * @since 1.0
     */
    List<AuditLogEntity> findByOperatorIdOrderByCreateTimeDesc(Long operatorId);

    /**
     * 查询指定时间范围内的审计日志(按创建时间倒序)
     *
     * <p>用于生成审计报表,统计某个时间段内的所有操作</p>
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计日志列表,按创建时间倒序排列
     * @since 1.0
     */
    List<AuditLogEntity> findByCreateTimeBetweenOrderByCreateTimeDesc(
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 查询指定模块的审计日志(按创建时间倒序)
     *
     * <p>用于审计某个业务模块的操作记录,如"认证管理"、"挂号管理"等</p>
     *
     * @param module 模块名称
     * @return 审计日志列表,按创建时间倒序排列
     * @since 1.0
     */
    List<AuditLogEntity> findByModuleOrderByCreateTimeDesc(String module);

    /**
     * 根据TraceId查询审计日志(按创建时间倒序)
     *
     * <p>用于查询整个请求链路的审计日志,通过TraceId关联所有相关操作</p>
     * <p>TraceId由MDC自动生成,在同一个请求链路中保持一致</p>
     *
     * @param traceId 链路追踪ID(32位十六进制字符串)
     * @return 审计日志列表,按创建时间倒序排列
     * @since 1.0
     */
    List<AuditLogEntity> findByTraceIdOrderByCreateTimeDesc(String traceId);

    /**
     * 根据审计类型查询审计日志(按创建时间倒序)
     *
     * <p>用于查询特定类型的操作记录</p>
     * <p>审计类型包括：SENSITIVE_OPERATION、BUSINESS、DATA_ACCESS</p>
     *
     * @param auditType 审计类型
     * @return 审计日志列表,按创建时间倒序排列
     * @since 1.0
     */
    List<AuditLogEntity> findByAuditTypeOrderByCreateTimeDesc(String auditType);

    /**
     * 删除指定时间之前的审计日志
     *
     * <p>用于定期清理过期数据,释放存储空间</p>
     * <p>配合定时任务使用,每天凌晨2点清理180天前的数据</p>
     *
     * <p><b>注意：</b>此操作不可逆,删除前建议先备份</p>
     *
     * @param time 截止时间(此时间之前的所有日志将被删除)
     * @since 1.0
     * @see com.his.scheduled.AuditLogCleanupTask
     */
    void deleteByCreateTimeBefore(LocalDateTime time);
}
