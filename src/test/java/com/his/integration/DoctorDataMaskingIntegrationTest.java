package com.his.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.entity.Doctor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 医生信息脱敏集成测试
 *
 * <p>验证 Doctor 实体序列化为 JSON 时，敏感字段自动脱敏
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("医生信息脱敏集成测试")
class DoctorDataMaskingIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 手机号自动脱敏")
    void testDoctorJsonSerialization_PhoneMasked() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setPhone("13800138000");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).doesNotContain("13800138000");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 邮箱自动脱敏")
    void testDoctorJsonSerialization_EmailMasked() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setEmail("zhangsan@hospital.com");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"email\":\"zh******@hospital.com\"");
        assertThat(json).doesNotContain("zhangsan");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 长邮箱自动脱敏")
    void testDoctorJsonSerialization_LongEmailMasked() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setEmail("wangxiaoming@example.com");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"email\":\"wa**********@example.com\"");
        assertThat(json).doesNotContain("wangxiaoming");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 短邮箱自动脱敏")
    void testDoctorJsonSerialization_ShortEmailMasked() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setEmail("ab@example.com");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"email\":\"ab***@example.com\"");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 多字段同时脱敏")
    void testDoctorJsonSerialization_MultipleFieldsMasked() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setName("李医生");
        doctor.setPhone("15912345678");
        doctor.setEmail("lidoctor@hospital.com");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"phone\":\"159****5678\"");
        assertThat(json).contains("\"email\":\"li******@hospital.com\"");

        // 验证明文不存在
        assertThat(json).doesNotContain("15912345678");
        assertThat(json).doesNotContain("lidoctor");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - null值不脱敏")
    void testDoctorJsonSerialization_NullValues() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setPhone(null);
        doctor.setEmail(null);

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"phone\":null");
        assertThat(json).contains("\"email\":null");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 空字符串不脱敏")
    void testDoctorJsonSerialization_EmptyValues() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setPhone("");
        doctor.setEmail("");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        assertThat(json).contains("\"phone\":\"\"");
        assertThat(json).contains("\"email\":\"\"");
    }

    @Test
    @DisplayName("测试医生信息JSON序列化 - 无效邮箱格式不脱敏")
    void testDoctorJsonSerialization_InvalidEmail() throws JsonProcessingException {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setEmail("invalid-email");

        // Act
        String json = objectMapper.writeValueAsString(doctor);

        // Assert
        // 无效邮箱格式应该返回原值
        assertThat(json).contains("\"email\":\"invalid-email\"");
    }
}
