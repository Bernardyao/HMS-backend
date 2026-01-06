package com.his.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.entity.Patient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据脱敏上下文测试
 *
 * <p>验证 DataMaskingContext 的功能：
 * <ul>
 *   <li>默认情况下启用脱敏</li>
 *   <li>可以禁用脱敏显示明文</li>
 *   <li>使用 Scope 自动恢复脱敏状态</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("数据脱敏上下文测试")
class DataMaskingContextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("测试默认状态 - 脱敏启用")
    void testDefaultState_MaskingEnabled() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");
        patient.setIdCard("110101199001011234");

        // Act - 默认情况下脱敏应该启用
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).contains("\"idCard\":\"110101********1234\"");
        assertThat(DataMaskingContext.getStatus()).isEqualTo("ENABLED (敏感数据脱敏)");
    }

    @Test
    @DisplayName("测试禁用脱敏 - 显示明文")
    void testDisableMasking() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");
        patient.setIdCard("110101199001011234");

        // Act - 禁用脱敏
        DataMaskingContext.disableMasking();
        String json = objectMapper.writeValueAsString(patient);

        try {
            // Assert - 应该包含明文
            assertThat(json).contains("\"phone\":\"13800138000\"");
            assertThat(json).contains("\"idCard\":\"110101199001011234\"");
            assertThat(DataMaskingContext.getStatus()).isEqualTo("DISABLED (显示明文)");
        } finally {
            // Cleanup - 恢复脱敏状态
            DataMaskingContext.enableMasking();
        }

        // After cleanup - 验证脱敏已恢复
        String jsonAfter = objectMapper.writeValueAsString(patient);
        assertThat(jsonAfter).contains("\"phone\":\"138****8000\"");
        assertThat(DataMaskingContext.getStatus()).isEqualTo("ENABLED (敏感数据脱敏)");
    }

    @Test
    @DisplayName("测试 Scope 自动恢复 - try-with-resources")
    void testScopeAutoRestore() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");

        // Act & Assert - 在 Scope 内脱敏被禁用
        String jsonInside;
        try (DataMaskingContext.Scope scope = DataMaskingContext.disable()) {
            assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();
            jsonInside = objectMapper.writeValueAsString(patient);
            assertThat(jsonInside).contains("\"phone\":\"13800138000\"");
        }

        // Assert - Scope 关闭后脱敏自动恢复
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();
        String jsonOutside = objectMapper.writeValueAsString(patient);
        assertThat(jsonOutside).contains("\"phone\":\"138****8000\"");
    }

    @Test
    @DisplayName("测试嵌套 Scope")
    void testNestedScope() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");

        // Act - 嵌套使用 Scope
        try (DataMaskingContext.Scope outer = DataMaskingContext.disable()) {
            assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();

            String json1 = objectMapper.writeValueAsString(patient);
            assertThat(json1).contains("\"phone\":\"13800138000\"");

            // 内层 Scope
            try (DataMaskingContext.Scope inner = DataMaskingContext.disable()) {
                assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();
                String json2 = objectMapper.writeValueAsString(patient);
                assertThat(json2).contains("\"phone\":\"13800138000\"");
            }

            // 内层关闭后，外层仍然禁用
            assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();
            String json3 = objectMapper.writeValueAsString(patient);
            assertThat(json3).contains("\"phone\":\"13800138000\"");
        }

        // 外层关闭后恢复
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();
        String jsonFinal = objectMapper.writeValueAsString(patient);
        assertThat(jsonFinal).contains("\"phone\":\"138****8000\"");
    }

    @Test
    @DisplayName("测试多次调用 enableMasking - 幂等性")
    void testEnableMaskingIdempotent() {
        // Arrange
        DataMaskingContext.disableMasking();
        assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();

        // Act - 多次调用
        DataMaskingContext.enableMasking();
        DataMaskingContext.enableMasking();

        // Assert - 不应该抛出异常
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    @DisplayName("测试线程隔离 - 不同线程独立状态")
    void testThreadIsolation() throws InterruptedException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");

        // 主线程：默认脱敏
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();

        // 创建新线程并禁用脱敏
        Thread thread = new Thread(() -> {
            DataMaskingContext.disableMasking();
            assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();
            // 恢复脱敏
            DataMaskingContext.enableMasking();
        });

        thread.start();
        thread.join();

        // Assert - 主线程不受影响
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    @DisplayName("测试 Scope 在异常情况下也能恢复")
    void testScopeRestoreOnException() {
        // Arrange - 不预先禁用脱敏
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();

        // Act - 在 Scope 内抛出异常
        try (DataMaskingContext.Scope scope = DataMaskingContext.disable()) {
            assertThat(DataMaskingContext.isMaskingDisabled()).isTrue();
            throw new RuntimeException("测试异常");
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Assert - 即使异常，Scope 也会正确关闭
        assertThat(DataMaskingContext.isMaskingDisabled()).isFalse();
    }
}
