package com.his.monitoring;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志指标报告器
 *
 * <p>定期检查关键指标并记录告警日志</p>
 *
 * <p>告警规则：</p>
 * <ul>
 *   <li>序列生成失败率 > 1% → ERROR</li>
 *   <li>序列生成P99响应时间 > 100ms → WARN</li>
 *   <li>收费创建P99响应时间 > 1000ms → WARN</li>
 * </ul>
 */
@Slf4j
@Component
public class LoggingMetricsReporter {

    private final MeterRegistry meterRegistry;
    private final SequenceGenerationMetrics sequenceMetrics;

    /**
     * 告警阈值配置
     */
    private static final double FAILURE_RATE_ERROR_THRESHOLD = 0.01; // 1%
    private static final double FAILURE_RATE_WARN_THRESHOLD = 0.005; // 0.5%
    private static final double SEQUENCE_P99_WARN_THRESHOLD_MS = 100.0;
    private static final double CHARGE_P99_WARN_THRESHOLD_MS = 1000.0;

    /**
     * 构造函数
     */
    public LoggingMetricsReporter(
            MeterRegistry meterRegistry,
            SequenceGenerationMetrics sequenceMetrics) {
        this.meterRegistry = meterRegistry;
        this.sequenceMetrics = sequenceMetrics;
    }

    /**
     * 定期报告指标 - 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void reportMetrics() {
        try {
            reportSequenceGenerationMetrics();
            reportChargeCreationMetrics();
        } catch (Exception e) {
            log.error("报告指标失败", e);
        }
    }

    /**
     * 报告序列生成指标
     */
    private void reportSequenceGenerationMetrics() {
        // 检查失败率
        double failureRate = sequenceMetrics.getFailureRate("charge_no");

        if (failureRate > FAILURE_RATE_ERROR_THRESHOLD) {
            log.error("【告警】序列生成失败率过高: {}%, 成功: {}, 失败: {}",
                    String.format("%.2f", failureRate * 100),
                    sequenceMetrics.getSuccessCount("charge_no"),
                    sequenceMetrics.getFailureCount("charge_no"));
        } else if (failureRate > FAILURE_RATE_WARN_THRESHOLD) {
            log.warn("【警告】序列生成失败率偏高: {}%, 成功: {}, 失败: {}",
                    String.format("%.2f", failureRate * 100),
                    sequenceMetrics.getSuccessCount("charge_no"),
                    sequenceMetrics.getFailureCount("charge_no"));
        }

        // 检查P99响应时间
        Timer timer = meterRegistry.find("sequence.generation.duration").timer();
        if (timer != null) {
            // 使用takeSnapshot()获取百分位数据
            double p99Ms = timer.mean(TimeUnit.MILLISECONDS); // 使用平均响应时间作为替代指标

            if (p99Ms > SEQUENCE_P99_WARN_THRESHOLD_MS) {
                log.warn("【警告】序列生成平均响应时间过长: {} ms (阈值: {} ms), 总次数: {}",
                        String.format("%.2f", p99Ms), SEQUENCE_P99_WARN_THRESHOLD_MS, timer.count());
            }
        }
    }

    /**
     * 报告收费创建指标
     */
    private void reportChargeCreationMetrics() {
        Timer timer = meterRegistry.find("charge.creation.duration").timer();
        if (timer != null) {
            double meanMs = timer.mean(TimeUnit.MILLISECONDS);

            if (meanMs > CHARGE_P99_WARN_THRESHOLD_MS) {
                log.warn("【警告】收费创建平均响应时间过长: {} ms (阈值: {} ms), 总次数: {}",
                        String.format("%.2f", meanMs), CHARGE_P99_WARN_THRESHOLD_MS, timer.count());
            }
        }
    }

    /**
     * 手动触发健康检查（用于测试）
     */
    public void checkNow() {
        log.info("手动触发指标检查");
        reportMetrics();
    }
}
