package com.his.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志告警服务
 *
 * <p>监控系统错误日志并在达到阈值时触发告警</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>错误计数</b>：记录系统错误次数</li>
 *   <li><b>阈值告警</b>：错误达到阈值时自动触发告警</li>
 *   <li><b>告限流</b>：防止频繁告警（至少间隔30分钟）</li>
 *   <li><b>邮件通知</b>：支持邮件告警（可配置）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>系统监控</b>：实时监控系统健康状态</li>
 *   <li><b>问题预警</b>：在问题严重化前提前发现</li>
 *   <li><b>运维响应</b>：及时通知运维人员处理</li>
 * </ul>
 *
 * <h3>配置参数</h3>
 * <ul>
 *   <li><b>monitoring.alert.email.enabled</b>：是否启用邮件告警</li>
 *   <li><b>monitoring.alert.email.to</b>：告警接收邮箱</li>
 *   <li><b>monitoring.alert.error.threshold</b>：错误告警阈值</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class AlertService {

    @Value("${monitoring.alert.email.enabled:false}")
    private boolean emailAlertEnabled;

    @Value("${monitoring.alert.email.to:admin@example.com}")
    private String alertEmail;

    @Value("${monitoring.alert.error.threshold:100}")
    private int errorThreshold;

    /**
     * 错误计数器（线程安全）
     */
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 上次告警时间（用于限流）
     */
    private LocalDateTime lastAlertTime;

    /**
     * 告警间隔时间（分钟）
     */
    private static final int ALERT_INTERVAL_MINUTES = 30;

    /**
     * 记录错误并检查是否需要告警
     *
     * <p>每次调用时增加错误计数,当计数达到阈值时触发告警</p>
     *
     * <p><b>告警触发条件：</b></p>
     * <ul>
     *   <li>错误计数 >= 阈值</li>
     *   <li>距离上次告警时间 >= 30分钟</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 在GlobalExceptionHandler中使用
     * {@code @ExceptionHandler(Exception.class)}
     * public ResponseEntity<Result<Void>> handleException(Exception e) {
     *     alertService.recordError("系统异常", MDC.get("traceId"));
     *     // ...
     * }
     * </pre>
     *
     * @param message 错误消息
     * @param traceId 链路追踪ID（可为null）
     * @since 1.0
     */
    public void recordError(String message, String traceId) {
        long count = errorCount.incrementAndGet();

        log.warn("错误日志计数: {}, TraceId: {}", count, traceId);

        // 检查是否需要告警
        if (count % errorThreshold == 0) {
            sendAlert("错误日志数量异常",
                    String.format("最近错误日志数量达到 %d 次", count));
        }
    }

    /**
     * 重置错误计数器
     *
     * <p>用于在问题解决后重置计数器</p>
     * <p>通常由运维人员在控制台手动触发</p>
     *
     * @since 1.0
     */
    public void resetErrorCount() {
        long oldCount = errorCount.getAndSet(0);
        log.info("错误计数器已重置,之前计数: {}", oldCount);
    }

    /**
     * 获取当前错误计数
     *
     * <p>用于监控和展示</p>
     *
     * @return 当前错误计数
     * @since 1.0
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * 发送告警
     *
     * <p>包含告警限流逻辑,防止频繁告警</p>
     *
     * <p><b>告警限流策略：</b></p>
     * <ul>
     *   <li>距离上次告警时间 < 30分钟 → 不发送</li>
     *   <li>距离上次告警时间 >= 30分钟 → 发送</li>
     * </ul>
     *
     * @param subject 告警主题
     * @param message 告警消息
     * @since 1.0
     */
    private void sendAlert(String subject, String message) {
        // 防止频繁告警(至少间隔30分钟)
        if (lastAlertTime != null &&
                LocalDateTime.now().minusMinutes(ALERT_INTERVAL_MINUTES).isBefore(lastAlertTime)) {
            log.debug("告警限流: 距离上次告警不足30分钟,跳过本次告警");
            return;
        }

        log.warn("=== 触发告警 ===");
        log.warn("告警主题: {}", subject);
        log.warn("告警消息: {}", message);
        log.warn("告警时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.warn("==================");

        if (emailAlertEnabled) {
            sendEmailAlert(subject, message);
        }

        lastAlertTime = LocalDateTime.now();
    }

    /**
     * 发送邮件告警
     *
     * <p>如果邮件配置不存在或发送失败,只记录日志不抛出异常</p>
     *
     * <p><b>注意：</b>需要配置Spring Mail相关参数才能正常工作</p>
     * <ul>
     *   <li>spring.mail.host</li>
     *   <li>spring.mail.port</li>
     *   <li>spring.mail.username</li>
     *   <li>spring.mail.password</li>
     * </ul>
     *
     * @param subject 邮件主题
     * @param message 邮件内容
     * @since 1.0
     */
    private void sendEmailAlert(String subject, String message) {
        try {
            // TODO: 实现邮件发送
            // 注意：需要注入JavaMailSender
            // 示例代码：
            // SimpleMailMessage mailMessage = new SimpleMailMessage();
            // mailMessage.setTo(alertEmail);
            // mailMessage.setSubject("[HIS告警] " + subject);
            // mailMessage.setText(message);
            // mailSender.send(mailMessage);

            log.info("邮件告警功能待实现（需要配置Spring Mail）");
            log.info("告警邮件将发送到: {}", alertEmail);
            log.info("邮件主题: [HIS告警] {}", subject);
            log.info("邮件内容: {}", message);

        } catch (Exception e) {
            log.error("发送邮件告警失败", e);
        }
    }

    /**
     * 检查告警状态
     *
     * <p>用于监控展示</p>
     *
     * @return 告警状态信息
     * @since 1.0
     */
    public AlertStatus getAlertStatus() {
        return new AlertStatus(
                errorCount.get(),
                errorThreshold,
                lastAlertTime,
                emailAlertEnabled
        );
    }

    /**
     * 告警状态信息
     */
    public record AlertStatus(
            long currentErrorCount,    // 当前错误计数
            long errorThreshold,       // 告警阈值
            LocalDateTime lastAlertTime,  // 上次告警时间
            boolean emailAlertEnabled  // 邮件告警是否启用
    ) {}
}
