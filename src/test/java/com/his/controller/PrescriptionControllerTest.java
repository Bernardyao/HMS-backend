package com.his.controller;

import com.his.dto.PrescriptionDTO;
import com.his.entity.Prescription;
import com.his.service.PrescriptionService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 处方管理控制器测试类（重构后版本）
 * <p>
 * 测试范围：
 * <ul>
 *   <li>创建处方（POST /api/doctor/prescriptions/create）</li>
 *   <li>审核处方（POST /api/doctor/prescriptions/{id}/review）</li>
 * </ul>
 *
 * <p>注意：查询功能已移至 {@link com.his.controller.CommonPrescriptionController}</p>
 * <p>覆盖率目标: 75%+
 *
 * @author HIS开发团队
 * @version 2.0
 */
@DisplayName("处方控制器测试（操作类）")
class PrescriptionControllerTest extends BaseControllerTest {

    @MockBean
    private PrescriptionService prescriptionService;

    // ==================== POST /create - 创建处方测试 ====================

    @Test
    @DisplayName("创建处方 - 成功场景")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_Success() throws Exception {
        // Given: 准备处方DTO
        PrescriptionDTO dto = createTestPrescriptionDTO();

        Prescription savedPrescription = new Prescription();
        savedPrescription.setMainId(1L);
        savedPrescription.setPrescriptionNo("RX20260103001");
        savedPrescription.setPrescriptionType((short) 1);
        savedPrescription.setTotalAmount(new BigDecimal("150.00"));
        savedPrescription.setItemCount(2);
        savedPrescription.setStatus((short) 0);
        savedPrescription.setValidityDays(3);
        savedPrescription.setCreatedAt(LocalDateTime.now());
        savedPrescription.setUpdatedAt(LocalDateTime.now());

        when(prescriptionService.createPrescription(any())).thenReturn(savedPrescription);

        // When & Then
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String content = result.getResponse().getContentAsString();
                    // Log for debugging
                    System.out.println("Status: " + status);
                    System.out.println("Response: " + content);
                    // Accept either 200 success or 500 with error code in body
                    if (status != 200) {
                        throw new AssertionError("Expected status 200 but got " + status + ". Response: " + content);
                    }
                })
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("处方创建成功"));
    }

    @Test
    @DisplayName("创建处方 - 病历记录不存在")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_RecordNotFound() throws Exception {
        // Given: 无效的挂号ID
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(999L);
        dto.setItems(Collections.emptyList());

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new IllegalArgumentException("挂号记录不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("挂号记录不存在"));
    }

    @Test
    @DisplayName("创建处方 - 药品明细为空")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_EmptyItems() throws Exception {
        // Given: 药品列表为空
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setItems(Collections.emptyList());

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new IllegalArgumentException("药品明细不能为空"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("药品明细不能为空"));
    }

    @Test
    @DisplayName("创建处方 - 药品库存不足")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_InsufficientStock() throws Exception {
        // Given: 药品库存不足
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);

        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(1L);
        item.setQuantity(1000);
        dto.setItems(Collections.singletonList(item));

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new IllegalArgumentException("药品库存不足"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("药品库存不足"));
    }

    @Test
    @DisplayName("创建处方 - 药品不存在")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_MedicineNotFound() throws Exception {
        // Given: 药品不存在
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);

        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(999L);
        item.setQuantity(10);
        dto.setItems(Collections.singletonList(item));

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new IllegalArgumentException("药品不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("药品不存在"));
    }

    @Test
    @DisplayName("创建处方 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_SystemException() throws Exception {
        // Given: 系统异常
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setItems(Collections.emptyList());

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== POST /{id}/review - 审核处方测试 ====================

    @Test
    @DisplayName("审核处方 - PHARMACIST角色成功")
    @WithMockUser(roles = "PHARMACIST")
    void testReviewPrescription_Pharmacist_Success() throws Exception {
        // Given: void method, no need to mock for success case
        // When & Then
        mockMvc.perform(post("/api/doctor/prescriptions/1/review")
                        .param("reviewDoctorId", "10")
                        .param("remark", "处方合理，准予发药"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("审核成功"));
    }

    @Test
    @DisplayName("审核处方 - ADMIN角色成功")
    @WithMockUser(roles = "ADMIN")
    void testReviewPrescription_Admin_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/doctor/prescriptions/1/review")
                        .param("reviewDoctorId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("审核处方 - 已审核过")
    @WithMockUser(roles = "PHARMACIST")
    void testReviewPrescription_AlreadyReviewed() throws Exception {
        // Given: 处方已审核
        doThrow(new IllegalStateException("处方已审核，无法重复审核"))
                .when(prescriptionService).review(eq(1L), eq(10L), any());

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/1/review")
                        .param("reviewDoctorId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("处方已审核，无法重复审核"));
    }

    @Test
    @DisplayName("审核处方 - 处方不存在")
    @WithMockUser(roles = "PHARMACIST")
    void testReviewPrescription_NotFound() throws Exception {
        // Given: 处方不存在
        doThrow(new IllegalArgumentException("处方不存在"))
                .when(prescriptionService).review(eq(999L), eq(10L), any());

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/999/review")
                        .param("reviewDoctorId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("处方不存在"));
    }

    @Test
    @DisplayName("审核处方 - 无效ID")
    @WithMockUser(roles = "PHARMACIST")
    void testReviewPrescription_InvalidId() throws Exception {
        // Given: 无效ID
        doThrow(new IllegalArgumentException("无效的处方ID"))
                .when(prescriptionService).review(eq(0L), eq(10L), any());

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/0/review")
                        .param("reviewDoctorId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("审核处方 - DOCTOR角色无权限")
    @WithMockUser(roles = "DOCTOR")
    void testReviewPrescription_AccessDenied() throws Exception {
        // Given: DOCTOR角色尝试审核
        // When & Then: 应返回403
        mockMvc.perform(post("/api/doctor/prescriptions/1/review")
                        .param("reviewDoctorId", "10"))
                .andExpect(status().isForbidden());
    }

    // ==================== 权限和异常测试 ====================

    @Test
    @DisplayName("创建处方 - 未认证用户")
    void testCreatePrescription_Unauthenticated() throws Exception {
        // Given: 准备数据（不使用@WithMockUser）
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setItems(Collections.emptyList());

        // When & Then: 未认证用户会收到401或403
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    @DisplayName("Service层IllegalArgumentException")
    @WithMockUser(roles = "DOCTOR")
    void testServiceException_IllegalArgument() throws Exception {
        // Given: IllegalArgumentException
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setItems(Collections.emptyList());

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new IllegalArgumentException("非法参数"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("Service层通用Exception")
    @WithMockUser(roles = "DOCTOR")
    void testServiceException_Generic() throws Exception {
        // Given: 通用异常
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setItems(Collections.emptyList());

        when(prescriptionService.createPrescription(any()))
                .thenThrow(new RuntimeException("系统错误"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(post("/api/doctor/prescriptions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用处方DTO
     */
    private PrescriptionDTO createTestPrescriptionDTO() {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setPrescriptionType((short) 1);
        dto.setValidityDays(3);

        PrescriptionDTO.PrescriptionItemDTO item1 = new PrescriptionDTO.PrescriptionItemDTO();
        item1.setMedicineId(1L);
        item1.setQuantity(10);
        item1.setFrequency("一日三次");
        item1.setDosage("每次2片");
        item1.setRoute("口服");

        PrescriptionDTO.PrescriptionItemDTO item2 = new PrescriptionDTO.PrescriptionItemDTO();
        item2.setMedicineId(2L);
        item2.setQuantity(5);
        item2.setFrequency("一日两次");
        item2.setDosage("每次1片");

        dto.setItems(Arrays.asList(item1, item2));

        return dto;
    }
}
