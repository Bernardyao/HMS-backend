package com.his.monitoring;

import com.his.repository.ChargeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 序列生成器健康检查
 *
 * <p>自定义健康检查，验证序列生成功能是否正常</p>
 *
 * <p>健康检查内容：</p>
 * <ul>
 *   <li>测试序列生成函数是否可用</li>
 *   <li>检查响应时间是否在可接受范围内</li>
 *   <li>验证生成的编号格式是否正确</li>
 * </ul>
 */
@Slf4j
@Component
public class SequenceGeneratorHealthIndicator implements HealthIndicator {

    private final ChargeRepository chargeRepository;
    private final SequenceGenerationMetrics metrics;

    /**
     * 构造函数
     */
    public SequenceGeneratorHealthIndicator(
            ChargeRepository chargeRepository,
            SequenceGenerationMetrics metrics) {
        this.chargeRepository = chargeRepository;
        this.metrics = metrics;
    }

    /**
     * 健康检查
     *
     * @return 健康状态（UP, DOWN, DEGRADED）
     */
    @Override
    public Health health() {
        try {
            // 测试序列生成
            long startTime = System.nanoTime();
            String chargeNo = chargeRepository.generateChargeNo();
            long durationNs = System.nanoTime() - startTime;
            double durationMs = durationNs / 1_000_000.0;

            // 检查格式
            if (!chargeNo.matches("^CHG\\d{14}$")) {
                log.error("健康检查失败：编号格式不正确 - {}", chargeNo);
                return Health.down()
                        .withDetail("error", "Invalid charge number format")
                        .withDetail("chargeNo", chargeNo)
                        .withDetail("expectedFormat", "CHG + 14 digits")
                        .build();
            }

            // 检查响应时间
            String status;
            if (durationMs < 100) {
                status = "UP";
            } else if (durationMs < 500) {
                status = "DEGRADED";
            } else {
                status = "DOWN";
            }

            // 计算失败率
            double failureRate = metrics.getFailureRate("charge_no");

            log.info("序列生成健康检查: status={}, duration={}ms, chargeNo={}, failureRate={}",
                    status, String.format("%.2f", durationMs), chargeNo, String.format("%.2f%%", failureRate * 100));

            return Health.status(status)
                    .withDetail("responseTime", String.format("%.2f ms", durationMs))
                    .withDetail("sampleChargeNo", chargeNo)
                    .withDetail("failureRate", String.format("%.2f%%", failureRate * 100))
                    .withDetail("successCount", metrics.getSuccessCount("charge_no"))
                    .withDetail("failureCount", metrics.getFailureCount("charge_no"))
                    .build();

        } catch (Exception e) {
            log.error("序列生成健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
