package com.his.controller;

import com.his.dto.MedicalRecordDTO;
import com.his.entity.MedicalRecord;
import com.his.service.MedicalRecordService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 病历管理控制器测试类
 * <p>
 * 测试范围：
 * <ul>
 *   <li>创建/更新病历（POST /api/doctor/medical-records/save）</li>
 *   <li>查询病历详情（GET /api/doctor/medical-records/{id}）</li>
 *   <li>根据挂号ID查询病历（GET /api/doctor/medical-records/by-registration/{registrationId}）</li>
 *   <li>提交病历（POST /api/doctor/medical-records/{id}/submit）</li>
 * </ul>
 *
 * <p>覆盖率目标: 75%+
 *
 * @author HIS开发团队
 * @since 1.0.0
 */
@DisplayName("病历控制器测试")
class MedicalRecordControllerTest extends BaseControllerTest {

    @MockBean
    private MedicalRecordService medicalRecordService;

    // ==================== POST /save - 创建/更新病历测试 ====================

    @Test
    @DisplayName("创建病历 - 成功场景")
    @WithMockUser(roles = "DOCTOR")
    void testSaveMedicalRecord_Success() throws Exception {
        // Given: 准备病历DTO
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("头痛");
        dto.setPresentIllness("持续3天");
        dto.setPastHistory("无既往病史");
        dto.setDiagnosis("偏头痛");
        dto.setTreatmentPlan("止痛药物治疗");

        MedicalRecord savedRecord = new MedicalRecord();
        savedRecord.setMainId(1L);
        savedRecord.setChiefComplaint("头痛");
        savedRecord.setDiagnosis("偏头痛");

        when(medicalRecordService.saveOrUpdate(any())).thenReturn(savedRecord);

        // When & Then: 执行请求并验证
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("保存成功"))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛"))
                .andExpect(jsonPath("$.data.diagnosis").value("偏头痛"));
    }

    @Test
    @DisplayName("更新已有病历 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testUpdateMedicalRecord_Success() throws Exception {
        // Given: 准备已存在病历的更新数据
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("头痛加重");
        dto.setDiagnosis("紧张性头痛");

        MedicalRecord updatedRecord = new MedicalRecord();
        updatedRecord.setMainId(1L);
        updatedRecord.setChiefComplaint("头痛加重");
        updatedRecord.setDiagnosis("紧张性头痛");

        when(medicalRecordService.saveOrUpdate(any())).thenReturn(updatedRecord);

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛加重"))
                .andExpect(jsonPath("$.data.diagnosis").value("紧张性头痛"));
    }

    @Test
    @DisplayName("创建病历 - 挂号记录不存在")
    @WithMockUser(roles = "DOCTOR")
    void testSaveMedicalRecord_RegistrationNotFound() throws Exception {
        // Given: 无效的挂号ID
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(999L);
        dto.setChiefComplaint("头痛");

        when(medicalRecordService.saveOrUpdate(any()))
                .thenThrow(new IllegalArgumentException("挂号记录不存在"));

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("挂号记录不存在"));
    }

    @Test
    @DisplayName("创建病历 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testSaveMedicalRecord_SystemException() throws Exception {
        // Given: Service抛出通用异常
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("头痛");

        when(medicalRecordService.saveOrUpdate(any()))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("保存失败")));
    }

    // ==================== GET /{id} - 查询病历详情测试 ====================

    @Test
    @DisplayName("查询病历详情 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordById_Success() throws Exception {
        // Given: 准备病历数据
        MedicalRecord record = new MedicalRecord();
        record.setMainId(1L);
        record.setChiefComplaint("头痛");
        record.setDiagnosis("偏头痛");

        when(medicalRecordService.getById(1L)).thenReturn(record);

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛"))
                .andExpect(jsonPath("$.data.diagnosis").value("偏头痛"));
    }

    @Test
    @DisplayName("查询病历详情 - 记录不存在")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordById_NotFound() throws Exception {
        // Given: 病历不存在
        when(medicalRecordService.getById(999L))
                .thenThrow(new IllegalArgumentException("病历不存在"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("病历不存在"));
    }

    @Test
    @DisplayName("查询病历详情 - 无效ID格式（ID为0）")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordById_InvalidId() throws Exception {
        // Given: ID为0
        when(medicalRecordService.getById(0L))
                .thenThrow(new IllegalArgumentException("无效的病历ID"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询病历详情 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordById_SystemException() throws Exception {
        // Given: 系统异常
        when(medicalRecordService.getById(1L))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("查询失败")));
    }

    // ==================== GET /by-registration/{registrationId} - 根据挂号ID查询测试 ====================

    @Test
    @DisplayName("根据挂号ID查询病历 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordByRegistrationId_Success() throws Exception {
        // Given: 准备病历数据
        MedicalRecord record = new MedicalRecord();
        record.setMainId(1L);
        record.setChiefComplaint("发热");
        record.setDiagnosis("上呼吸道感染");

        when(medicalRecordService.getByRegistrationId(1L)).thenReturn(record);

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/by-registration/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.chiefComplaint").value("发热"));
    }

    @Test
    @DisplayName("根据挂号ID查询病历 - 病历不存在（返回null）")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordByRegistrationId_NoRecord() throws Exception {
        // Given: 挂号存在但病历不存在
        when(medicalRecordService.getByRegistrationId(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/by-registration/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("该挂号单尚未创建病历"));
                // Note: data字段为null时可能不会被序列化
    }

    @Test
    @DisplayName("根据挂号ID查询病历 - 挂号记录不存在")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordByRegistrationId_RegistrationNotFound() throws Exception {
        // Given: 挂号不存在
        when(medicalRecordService.getByRegistrationId(999L))
                .thenThrow(new IllegalArgumentException("挂号记录不存在"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/by-registration/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("挂号记录不存在"));
    }

    @Test
    @DisplayName("根据挂号ID查询病历 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordByRegistrationId_SystemException() throws Exception {
        // Given: 系统异常
        when(medicalRecordService.getByRegistrationId(1L))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medical-records/by-registration/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("查询失败")));
    }

    // ==================== POST /{id}/submit - 提交病历测试 ====================

    @Test
    @DisplayName("提交病历 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testSubmitMedicalRecord_Success() throws Exception {
        // Given: 准备病历ID (void method, no need to mock)
        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("提交成功"));
                // Note: data字段为null时可能不会被序列化
    }

    @Test
    @DisplayName("提交病历 - 病历不存在")
    @WithMockUser(roles = "DOCTOR")
    void testSubmitMedicalRecord_NotFound() throws Exception {
        // Given: 病历不存在
        doThrow(new IllegalArgumentException("病历不存在"))
                .when(medicalRecordService).submit(999L);

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/999/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("病历不存在"));
    }

    @Test
    @DisplayName("提交病历 - 已提交过")
    @WithMockUser(roles = "DOCTOR")
    void testSubmitMedicalRecord_AlreadySubmitted() throws Exception {
        // Given: 病历已提交
        doThrow(new IllegalStateException("病历已提交，无法重复提交"))
                .when(medicalRecordService).submit(1L);

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("病历已提交，无法重复提交"));
    }

    @Test
    @DisplayName("提交病历 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testSubmitMedicalRecord_SystemException() throws Exception {
        // Given: 系统异常
        doThrow(new RuntimeException("数据库更新失败"))
                .when(medicalRecordService).submit(1L);

        // When & Then
        mockMvc.perform(post("/api/doctor/medical-records/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("提交失败")));
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("DOCTOR角色 - 有权限访问病历接口")
    @WithMockUser(roles = "DOCTOR")
    void testDoctorRole_AccessAllowed() throws Exception {
        // Given: 准备数据
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("头痛");

        when(medicalRecordService.saveOrUpdate(any())).thenReturn(new MedicalRecord());

        // When & Then: DOCTOR角色应能访问
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN角色 - 有权限访问病历接口")
    @WithMockUser(roles = "ADMIN")
    void testAdminRole_AccessAllowed() throws Exception {
        // Given: 准备数据
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("头痛");

        when(medicalRecordService.saveOrUpdate(any())).thenReturn(new MedicalRecord());

        // When & Then: ADMIN角色应能访问
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("NURSE角色 - 无权限访问病历接口")
    @WithMockUser(roles = "NURSE")
    void testNurseRole_AccessDenied() throws Exception {
        // Given: 准备数据
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);

        // When & Then: NURSE角色应返回403
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户 - 返回401或403")
    void testUnauthenticated_AccessDenied() throws Exception {
        // Given: 准备数据（不使用@WithMockUser）
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);

        // When & Then: 未认证用户会收到401或403（取决于Security配置）
        mockMvc.perform(post("/api/doctor/medical-records/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }
}
