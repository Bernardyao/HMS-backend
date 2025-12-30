package com.his.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 患者信息脱敏集成测试
 *
 * <p>验证 Patient 实体序列化为 JSON 时，敏感字段自动脱敏
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("患者信息脱敏集成测试")
class PatientDataMaskingIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 手机号自动脱敏")
    void testPatientJsonSerialization_PhoneMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("13800138000");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).doesNotContain("13800138000");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 身份证自动脱敏")
    void testPatientJsonSerialization_IdCardMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setIdCard("110101199001011234");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"idCard\":\"110101********1234\"");
        assertThat(json).doesNotContain("110101199001011234");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 医保卡号自动脱敏")
    void testPatientJsonSerialization_MedicalCardNoMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setMedicalCardNo("1234567890123456");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"medicalCardNo\":\"1234********3456\"");
        assertThat(json).doesNotContain("1234567890123456");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 地址自动脱敏")
    void testPatientJsonSerialization_AddressMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setAddress("北京市朝阳区建国路88号");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"address\":\"北京市朝阳区***\"");
        assertThat(json).doesNotContain("建国路88号");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 紧急联系人自动脱敏")
    void testPatientJsonSerialization_EmergencyContactMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setEmergencyContact("张三");
        patient.setEmergencyPhone("15912345678");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"emergencyContact\":\"张*\"");
        assertThat(json).contains("\"emergencyPhone\":\"159****5678\"");
        assertThat(json).doesNotContain("张三");
        assertThat(json).doesNotContain("15912345678");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 多字段同时脱敏")
    void testPatientJsonSerialization_MultipleFieldsMasked() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setName("李明");
        patient.setPhone("13800138000");
        patient.setIdCard("110101199001011234");
        patient.setAddress("北京市海淀区中关村大街1号");
        patient.setMedicalCardNo("6222021234567890");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).contains("\"idCard\":\"110101********1234\"");
        assertThat(json).contains("\"address\":\"北京市海淀区***\"");
        assertThat(json).contains("\"medicalCardNo\":\"6222********7890\"");

        // 验证明文不存在
        assertThat(json).doesNotContain("13800138000");
        assertThat(json).doesNotContain("110101199001011234");
        assertThat(json).doesNotContain("中关村大街");
        assertThat(json).doesNotContain("6222021234567890");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - null值不脱敏")
    void testPatientJsonSerialization_NullValues() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone(null);
        patient.setIdCard(null);

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"phone\":null");
        assertThat(json).contains("\"idCard\":null");
    }

    @Test
    @DisplayName("测试患者信息JSON序列化 - 空字符串不脱敏")
    void testPatientJsonSerialization_EmptyValues() throws JsonProcessingException {
        // Arrange
        Patient patient = new Patient();
        patient.setPhone("");
        patient.setAddress("");

        // Act
        String json = objectMapper.writeValueAsString(patient);

        // Assert
        assertThat(json).contains("\"phone\":\"\"");
        assertThat(json).contains("\"address\":\"\"");
    }
}
