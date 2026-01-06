package com.his.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 性能统计工具类
 *
 * <p>计算性能指标的统计信息：</p>
 * <ul>
 *   <li>平均值、P50、P95、P99、最大值、最小值</li>
 *   <li>吞吐量（ops/s）</li>
 * </ul>
 */
public class PerformanceStats {

    private final List<Long> nanos;

    private PerformanceStats(List<Long> nanos) {
        this.nanos = new ArrayList<>(nanos);
        Collections.sort(this.nanos);
    }

    /**
     * 从纳秒列表计算性能统计
     *
     * @param durations 持续时间列表（纳秒）
     * @return 性能统计对象
     */
    public static PerformanceStats calculate(List<Long> durations) {
        return new PerformanceStats(durations);
    }

    /**
     * 获取平均响应时间（微秒）
     */
    public double getAverageMicros() {
        if (nanos.isEmpty()) {
            return 0;
        }
        long total = nanos.stream().mapToLong(Long::longValue).sum();
        return (total / nanos.size()) / 1000.0;
    }

    /**
     * 获取平均响应时间（毫秒）
     */
    public double getAverageMillis() {
        return getAverageMicros() / 1000.0;
    }

    /**
     * 获取P50响应时间（微秒）
     */
    public double getP50Micros() {
        return getPercentile(50) / 1000.0;
    }

    /**
     * 获取P50响应时间（毫秒）
     */
    public double getP50Millis() {
        return getP50Micros() / 1000.0;
    }

    /**
     * 获取P95响应时间（微秒）
     */
    public double getP95Micros() {
        return getPercentile(95) / 1000.0;
    }

    /**
     * 获取P95响应时间（毫秒）
     */
    public double getP95Millis() {
        return getP95Micros() / 1000.0;
    }

    /**
     * 获取P99响应时间（微秒）
     */
    public double getP99Micros() {
        return getPercentile(99) / 1000.0;
    }

    /**
     * 获取P99响应时间（毫秒）
     */
    public double getP99Millis() {
        return getP99Micros() / 1000.0;
    }

    /**
     * 获取最小响应时间（微秒）
     */
    public double getMinMicros() {
        if (nanos.isEmpty()) {
            return 0;
        }
        return nanos.get(0) / 1000.0;
    }

    /**
     * 获取最小响应时间（毫秒）
     */
    public double getMinMillis() {
        return getMinMicros() / 1000.0;
    }

    /**
     * 获取最大响应时间（微秒）
     */
    public double getMaxMicros() {
        if (nanos.isEmpty()) {
            return 0;
        }
        return nanos.get(nanos.size() - 1) / 1000.0;
    }

    /**
     * 获取最大响应时间（毫秒）
     */
    public double getMaxMillis() {
        return getMaxMicros() / 1000.0;
    }

    /**
     * 获取吞吐量（每秒操作数）
     */
    public double getThroughputPerSecond() {
        if (nanos.isEmpty()) {
            return 0;
        }
        double totalSeconds = nanos.stream().mapToLong(Long::longValue).sum() / 1_000_000_000.0;
        return nanos.size() / totalSeconds;
    }

    /**
     * 获取指定百分位的响应时间（纳秒）
     */
    private double getPercentile(int percentile) {
        if (nanos.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(nanos.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, nanos.size() - 1));

        return nanos.get(index);
    }

    /**
     * 获取样本数量
     */
    public int getCount() {
        return nanos.size();
    }
}
