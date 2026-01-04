package com.his.scheduled;

import com.his.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志定期清理任务
 *
 * <p>定时清理过期的审计日志,释放存储空间</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>定时清理</b>：每天凌晨2点自动执行</li>
 *   <li><b>可配置保留期</b>：默认保留180天,可通过配置调整</li>
 *   <li><b>自动删除</b>：删除指定日期之前的所有审计日志</li>
 *   <li><b>执行日志</b>：记录删除的日志数量和执行时间</li>
 * </ul>
 *
 * <h3>配置参数</h3>
 * <ul>
 *   <li><b>audit.log.retention.days</b>：审计日志保留天数（默认180天）</li>
 * </ul>
 *
 * <h3>执行时间</h3>
 * <p>默认每天凌晨2点执行（Cron表达式: 0 0 2 * * ?）</p>
 * <p>此时间点通常系统负载较低,适合执行清理任务</p>
 *
 * <h3>使用示例</h3>
 * <p>在application.yml中配置：</p>
 * <pre>
 * audit:
 *   log:
 *     retention:
 *       days: 180  # 保留180天
 * </pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li><b>数据归档</b>：清理前建议先备份或归档重要数据</li>
 *   <li><b>性能影响</b>：删除大量数据可能影响性能,建议在低峰期执行</li>
 *   <li><b>监控日志</b>：关注任务执行日志,确保正常清理</li>
 *   <li><b>保留期限</b>：根据合规要求设置合理的保留期（如HIPAA要求6年）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.repository.AuditLogRepository#deleteByCreateTimeBefore(LocalDateTime)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogCleanupTask {

    private final AuditLogRepository auditLogRepository;

    /**
     * 审计日志保留天数（从配置文件读取）
     *
     * <p>默认值：180天（6个月）</p>
     * <p>可在application.yml中通过audit.log.retention.days配置</p>
     */
    @Value("${audit.log.retention.days:180}")
    private int retentionDays;

    /**
     * 每天凌晨2点执行清理
     *
     * <p>Cron表达式：0 0 2 * * ?</p>
     * <ul>
     *   <li>0 - 秒：第0秒</li>
     *   <li>0 - 分：第0分</li>
     *   <li>2 - 时：凌晨2点</li>
     *   <li>* - 日：每天</li>
     *   <li>* - 月：每月</li>
     *   <li>* - 星期：每周</li>
     *   <li>? - 不指定</li>
     * </ul>
     *
     * <p><b>执行逻辑：</b></p>
     * <ol>
     *   <li>计算截止日期（当前时间 - 保留天数）</li>
     *   <li>统计当前日志总数</li>
     *   <li>删除截止日期之前的所有日志</li>
     *   <li>计算删除数量并记录日志</li>
     * </ol>
     *
     * @since 1.0
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldAuditLogs() {
        log.info("=== 开始执行审计日志清理任务 ===");
        log.info("保留期限: {} 天", retentionDays);

        try {
            // 计算截止日期
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            log.info("截止日期: {} (将删除此日期之前的所有日志)", cutoffDate);

            // 统计删除前的记录数
            long countBefore = auditLogRepository.count();
            log.info("清理前总记录数: {}", countBefore);

            if (countBefore == 0) {
                log.info("没有需要清理的审计日志,任务结束");
                return;
            }

            // 执行删除
            auditLogRepository.deleteByCreateTimeBefore(cutoffDate);

            // 统计删除后的记录数
            long countAfter = auditLogRepository.count();
            long deleted = countBefore - countAfter;

            log.info("=== 审计日志清理完成 ===");
            log.info("删除记录数: {}", deleted);
            log.info("剩余记录数: {}", countAfter);
            log.info("保留期限: {} 天", retentionDays);
            log.info("====================");

            // 如果没有删除任何记录,给出警告
            if (deleted == 0) {
                log.warn("警告: 没有删除任何记录,可能所有日志都在保留期内");
            }

        } catch (Exception e) {
            log.error("审计日志清理失败", e);
            log.error("清理任务执行失败,请检查数据库连接和权限");
        }
    }

    /**
     * 手动触发清理（用于测试和管理）
     *
     * <p>可以通过管理接口或JMX手动调用此方法</p>
     * <p>用于紧急清理或测试清理逻辑</p>
     *
     * @since 1.0
     */
    public void cleanOldAuditLogsManually() {
        log.info("手动触发审计日志清理任务");
        cleanOldAuditLogs();
    }

    /**
     * 获取当前保留天数配置
     *
     * <p>用于监控和管理</p>
     *
     * @return 保留天数
     * @since 1.0
     */
    public int getRetentionDays() {
        return retentionDays;
    }

    /**
     * 获取清理统计信息
     *
     * <p>用于监控和展示</p>
     *
     * @return 清理统计信息
     * @since 1.0
     */
    public CleanupStats getCleanupStats() {
        long totalCount = auditLogRepository.count();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        // 估算将被删除的记录数（不准确,仅供参考）
        // 注意：这个操作比较耗时,实际应用中可以考虑缓存
        long estimatedToDelete = 0;
        try {
            // 简单估算：假设数据分布均匀
            // 实际应用中可以通过添加查询方法精确统计
            estimatedToDelete = totalCount / (retentionDays + 1); // 粗略估算
        } catch (Exception e) {
            log.warn("估算待删除记录数失败", e);
        }

        return new CleanupStats(
                totalCount,
                retentionDays,
                cutoffDate,
                estimatedToDelete
        );
    }

    /**
     * 清理统计信息
     */
    public record CleanupStats(
            long totalRecords,         // 当前总记录数
            int retentionDays,          // 保留天数
            LocalDateTime cutoffDate,   // 截止日期
            long estimatedToDelete      // 估算将被删除的记录数
    ) {}
}
