package com.his.controller;


import com.his.service.PatientService;
import com.his.vo.PatientSearchVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 护士工作站控制器 - 患者搜索接口集成测试
 *
 * @author HIS 开发团队
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("护士工作站 - 患者搜索接口测试")
class NurseWorkstationControllerPatientSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @Test
    @DisplayName("搜索患者 - NURSE角色有权限")
    @WithMockUser(roles = "NURSE")
    void searchPatients_NurseRole_Success() throws Exception {
        // Given
        List<PatientSearchVO> mockResults = Arrays.asList(
                createPatientSearchVO(1L, "张三", "320106199001011234", "13812345678"),
                createPatientSearchVO(2L, "张三丰", "320106199002022345", "13887654321")
        );

        when(patientService.searchPatients("张")).thenReturn(mockResults);

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "张")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("张三"))
                .andExpect(jsonPath("$.data[0].idCard").value("320106199001011234"))  // 不脱敏
                .andExpect(jsonPath("$.data[0].phone").value("13812345678"))  // 不脱敏
                .andExpect(jsonPath("$.data[1].name").value("张三丰"));

        verify(patientService).searchPatients("张");
    }

    @Test
    @DisplayName("搜索患者 - ADMIN角色有权限")
    @WithMockUser(roles = "ADMIN")
    void searchPatients_AdminRole_Success() throws Exception {
        // Given
        List<PatientSearchVO> mockResults = Collections.singletonList(
                createPatientSearchVO(1L, "李四", "320106199003033456", "13900139000")
        );

        when(patientService.searchPatients("李四")).thenReturn(mockResults);

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "李四")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("李四"));

        verify(patientService).searchPatients("李四");
    }

    @Test
    @DisplayName("搜索患者 - DOCTOR角色无权限")
    @WithMockUser(roles = "DOCTOR")
    void searchPatients_DoctorRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "张三")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(patientService, never()).searchPatients(anyString());
    }

    @Test
    @DisplayName("搜索患者 - 未认证用户无权限")
    void searchPatients_Unauthenticated_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "张三")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 项目中未认证通常返回 403

        verify(patientService, never()).searchPatients(anyString());
    }

    @Test
    @DisplayName("搜索患者 - 无结果")
    @WithMockUser(roles = "NURSE")
    void searchPatients_NoResults() throws Exception {
        // Given
        when(patientService.searchPatients("不存在的患者")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "不存在的患者")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(patientService).searchPatients("不存在的患者");
    }

    @Test
    @DisplayName("搜索患者 - 关键字为空")
    @WithMockUser(roles = "NURSE")
    void searchPatients_EmptyKeyword() throws Exception {
        // Given
        when(patientService.searchPatients("")).thenThrow(new IllegalArgumentException("搜索关键字不能为空"));

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Controller 捕获异常并返回 Result.badRequest()，HTTP 状态为 200
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("搜索关键字不能为空"));

        verify(patientService).searchPatients("");
    }

    @Test
    @DisplayName("搜索患者 - 关键字过短")
    @WithMockUser(roles = "NURSE")
    void searchPatients_KeywordTooShort() throws Exception {
        // Given
        when(patientService.searchPatients("张")).thenThrow(new IllegalArgumentException("搜索关键字至少需要 2 个字符"));

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "张")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Controller 捕获异常并返回 Result.badRequest()，HTTP 状态为 200
                .andExpect(jsonPath("$.code").value(400));

        verify(patientService).searchPatients("张");
    }

    @Test
    @DisplayName("搜索患者 - 按身份证号搜索")
    @WithMockUser(roles = "NURSE")
    void searchPatients_ByIdCard() throws Exception {
        // Given
        List<PatientSearchVO> mockResults = Collections.singletonList(
                createPatientSearchVO(1L, "张三", "320106199001011234", "13812345678")
        );

        when(patientService.searchPatients("320106")).thenReturn(mockResults);

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "320106")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].idCard").value("320106199001011234"));

        verify(patientService).searchPatients("320106");
    }

    @Test
    @DisplayName("搜索患者 - 按手机号搜索")
    @WithMockUser(roles = "NURSE")
    void searchPatients_ByPhone() throws Exception {
        // Given
        List<PatientSearchVO> mockResults = Collections.singletonList(
                createPatientSearchVO(1L, "张三", "320106199001011234", "13812345678")
        );

        when(patientService.searchPatients("138")).thenReturn(mockResults);

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "138")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].phone").value("13812345678"));

        verify(patientService).searchPatients("138");
    }

    @Test
    @DisplayName("搜索患者 - 系统异常")
    @WithMockUser(roles = "NURSE")
    void searchPatients_SystemException() throws Exception {
        // Given
        when(patientService.searchPatients("张三")).thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then
        mockMvc.perform(get("/api/nurse/patients/search")
                        .param("keyword", "张三")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Controller 捕获异常并返回 Result.error()，HTTP 状态为 200
                .andExpect(jsonPath("$.code").value(500));

        verify(patientService).searchPatients("张三");
    }

    // ==================== 辅助方法 ====================

    private PatientSearchVO createPatientSearchVO(Long id, String name, String idCard, String phone) {
        return PatientSearchVO.builder()
                .patientId(id)
                .patientNo("P2025010500" + id)
                .name(name)
                .idCard(idCard)
                .gender((short) 1)
                .genderDesc("男")
                .age((short) 34)
                .phone(phone)
                .build();
    }
}
