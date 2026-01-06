package com.his.monitoring;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

/**
 * 收费业务监控指标
 *
 * <p>记录收费单创建、支付处理的业务指标</p>
 *
 * <p>指标名称：</p>
 * <ul>
 *   <li>charge.creation.duration - 收费单创建响应时间（按类型和状态分类）</li>
 *   <li>charge.payment.duration - 支付处理响应时间（按支付方式和状态分类）</li>
 * </ul>
 */
@Slf4j
@Component
public class ChargeServiceMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 构造函数
     */
    public ChargeServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("收费业务监控指标已注册");
    }

    /**
     * 记录收费单创建
     *
     * @param chargeType 收费类型（registration, prescription, mixed）
     * @param durationMs 响应时间（毫秒）
     * @param success 是否成功
     */
    public void recordChargeCreation(String chargeType, long durationMs, boolean success) {
        Timer.builder("charge.creation.duration")
                .description("收费单创建响应时间")
                .tag("type", chargeType)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        log.debug("记录收费单创建: type={}, duration={}ms, success={}", chargeType, durationMs, success);
    }

    /**
     * 记录支付处理
     *
     * @param paymentMethod 支付方式（cash, card, wechat, alipay）
     * @param durationMs 响应时间（毫秒）
     * @param success 是否成功
     */
    public void recordPayment(String paymentMethod, long durationMs, boolean success) {
        Timer.builder("charge.payment.duration")
                .description("支付处理响应时间")
                .tag("method", paymentMethod)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        log.debug("记录支付处理: method={}, duration={}ms, success={}", paymentMethod, durationMs, success);
    }

    /**
     * 记录退费处理
     *
     * @param refundType 退费类型（charge, prescription）
     * @param durationMs 响应时间（毫秒）
     * @param success 是否成功
     */
    public void recordRefund(String refundType, long durationMs, boolean success) {
        Timer.builder("charge.refund.duration")
                .description("退费处理响应时间")
                .tag("type", refundType)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        log.debug("记录退费处理: type={}, duration={}ms, success={}", refundType, durationMs, success);
    }
}
