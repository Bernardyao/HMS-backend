package com.his.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 编号生成监控指标
 *
 * <p>记录编号生成的成功次数、失败次数和响应时间</p>
 * <p>使用Micrometer与Prometheus集成</p>
 *
 * <p>指标名称：</p>
 * <ul>
 *   <li>sequence.generation.success - 编号生成成功次数</li>
 *   <li>sequence.generation.failure - 编号生成失败次数（按错误类型分类）</li>
 *   <li>sequence.generation.duration - 编号生成响应时间（P50/P95/P99）</li>
 * </ul>
 */
@Slf4j
@Component
public class SequenceGenerationMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer responseTimer;

    /**
     * 构造函数 - 注册所有监控指标
     */
    public SequenceGenerationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 编号生成响应时间计时器
        this.responseTimer = Timer.builder("sequence.generation.duration")
                .description("编号生成响应时间")
                .tag("type", "all")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        log.info("编号生成监控指标已注册");
    }

    /**
     * 记录编号生成成功
     */
    public void recordSuccess(String sequenceType) {
        Counter.builder("sequence.generation.success")
                .description("编号生成成功次数")
                .tag("type", sequenceType)
                .register(meterRegistry)
                .increment();

        log.debug("记录编号生成成功: type={}", sequenceType);
    }

    /**
     * 记录编号生成失败
     *
     * @param sequenceType 编号类型（charge_no, prescription_no, reg_no, patient_no）
     * @param errorType 错误类型（DataAccessException, IllegalStateException等）
     */
    public void recordFailure(String sequenceType, String errorType) {
        Counter.builder("sequence.generation.failure")
                .description("编号生成失败次数")
                .tag("type", sequenceType)
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();

        log.debug("记录编号生成失败: type={}, error={}", sequenceType, errorType);
    }

    /**
     * 开始计时 - 返回一个计时器样本
     *
     * <p>用法：</p>
     * <pre>
     * Timer.Sample sample = sequenceMetrics.startTimer();
     * try {
     *     // 执行编号生成
     *     return sequenceNo;
     * } finally {
     *     sequenceMetrics.stopTimer(sample);
     * }
     * </pre>
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止计时并记录响应时间
     *
     * @param sample 计时器样本
     */
    public void stopTimer(Timer.Sample sample) {
        sample.stop(responseTimer);
    }

    /**
     * 获取成功次数
     */
    public long getSuccessCount(String sequenceType) {
        Counter counter = meterRegistry.find("sequence.generation.success")
                .tag("type", sequenceType)
                .counter();
        return counter != null ? (long) counter.count() : 0;
    }

    /**
     * 获取失败次数
     */
    public long getFailureCount(String sequenceType) {
        Counter counter = meterRegistry.find("sequence.generation.failure")
                .tag("type", sequenceType)
                .counter();
        return counter != null ? (long) counter.count() : 0;
    }

    /**
     * 计算失败率
     *
     * @return 失败率（0.0-1.0），如果没有记录则返回0
     */
    public double getFailureRate(String sequenceType) {
        long success = getSuccessCount(sequenceType);
        long failure = getFailureCount(sequenceType);
        long total = success + failure;

        return total > 0 ? (double) failure / total : 0.0;
    }
}
