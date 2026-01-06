package com.his.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import com.his.entity.Prescription;
import com.his.service.PrescriptionService;
import com.his.test.base.BaseControllerTest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公共处方查询控制器测试类
 * <p>
 * 测试范围：
 * <ul>
 *   <li>查询处方详情（GET /api/common/prescriptions/{id}） - 所有认证用户</li>
 *   <li>按病历查询处方列表（GET /api/common/prescriptions/by-record/{recordId}） - 所有认证用户</li>
 *   <li>多角色权限测试（DOCTOR, PHARMACIST, ADMIN）</li>
 * </ul>
 *
 * <p>覆盖率目标: 80%+
 *
 * @author HIS开发团队
 * @version 1.0
 */
@DisplayName("公共处方查询控制器测试")
class CommonPrescriptionControllerTest extends BaseControllerTest {

    @MockBean
    private PrescriptionService prescriptionService;

    // ==================== GET /{id} - 查询处方详情测试 ====================

    @Test
    @DisplayName("查询处方详情 - DOCTOR角色成功")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_Doctor_Success() throws Exception {
        // Given: 准备处方数据
        Prescription prescription = createTestPrescription();
        when(prescriptionService.getById(1L)).thenReturn(prescription);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.prescriptionNo").value("RX202501010001"))
                .andExpect(jsonPath("$.data.totalAmount").value(156.80))
                .andExpect(jsonPath("$.data.itemCount").value(3));
    }

    @Test
    @DisplayName("查询处方详情 - PHARMACIST角色成功")
    @WithMockUser(roles = "PHARMACIST")
    void testGetById_Pharmacist_Success() throws Exception {
        // Given
        Prescription prescription = createTestPrescription();
        when(prescriptionService.getById(1L)).thenReturn(prescription);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(1));
    }

    @Test
    @DisplayName("查询处方详情 - ADMIN角色成功")
    @WithMockUser(roles = "ADMIN")
    void testGetById_Admin_Success() throws Exception {
        // Given
        Prescription prescription = createTestPrescription();
        when(prescriptionService.getById(1L)).thenReturn(prescription);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(1));
    }

    @Test
    @DisplayName("查询处方详情 - NURSE角色成功")
    @WithMockUser(roles = "NURSE")
    void testGetById_Nurse_Success() throws Exception {
        // Given
        Prescription prescription = createTestPrescription();
        when(prescriptionService.getById(1L)).thenReturn(prescription);

        // When & Then: 护士也可以访问common接口
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询处方详情 - 处方不存在")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_NotFound() throws Exception {
        // Given: 处方不存在
        when(prescriptionService.getById(999L))
                .thenThrow(new IllegalArgumentException("处方不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/prescriptions/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("处方不存在"));
    }

    @Test
    @DisplayName("查询处方详情 - 无效ID")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_InvalidId() throws Exception {
        // Given: 无效ID
        when(prescriptionService.getById(0L))
                .thenThrow(new IllegalArgumentException("无效的处方ID"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/prescriptions/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询处方详情 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_SystemException() throws Exception {
        // Given: 系统异常
        when(prescriptionService.getById(1L))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("查询处方详情 - 未认证用户")
    void testGetById_Unauthenticated() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 未认证用户会收到401或403
        mockMvc.perform(get("/api/common/prescriptions/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    // ==================== GET /by-record/{recordId} - 按病历查询处方列表测试 ====================

    @Test
    @DisplayName("按病历查询处方 - DOCTOR角色成功")
    @WithMockUser(roles = "DOCTOR")
    void testGetByRecordId_Doctor_Success() throws Exception {
        // Given: 准备处方列表
        Prescription prescription1 = new Prescription();
        prescription1.setMainId(1L);
        prescription1.setPrescriptionNo("RX202501010001");
        prescription1.setTotalAmount(new BigDecimal("156.80"));
        prescription1.setStatus((short) 2); // REVIEWED

        Prescription prescription2 = new Prescription();
        prescription2.setMainId(2L);
        prescription2.setPrescriptionNo("RX202501010002");
        prescription2.setTotalAmount(new BigDecimal("89.50"));
        prescription2.setStatus((short) 2);

        List<Prescription> prescriptions = Arrays.asList(prescription1, prescription2);
        when(prescriptionService.getByRecordId(1L)).thenReturn(prescriptions);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/by-record/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].mainId").value(1))
                .andExpect(jsonPath("$.data[0].prescriptionNo").value("RX202501010001"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(156.80))
                .andExpect(jsonPath("$.data[1].mainId").value(2))
                .andExpect(jsonPath("$.data[1].prescriptionNo").value("RX202501010002"))
                .andExpect(jsonPath("$.data[1].totalAmount").value(89.50));
    }

    @Test
    @DisplayName("按病历查询处方 - PHARMACIST角色成功")
    @WithMockUser(roles = "PHARMACIST")
    void testGetByRecordId_Pharmacist_Success() throws Exception {
        // Given
        Prescription prescription = createTestPrescription();
        List<Prescription> prescriptions = Arrays.asList(prescription);
        when(prescriptionService.getByRecordId(1L)).thenReturn(prescriptions);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/by-record/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("按病历查询处方 - ADMIN角色成功")
    @WithMockUser(roles = "ADMIN")
    void testGetByRecordId_Admin_Success() throws Exception {
        // Given
        Prescription prescription = createTestPrescription();
        List<Prescription> prescriptions = Arrays.asList(prescription);
        when(prescriptionService.getByRecordId(1L)).thenReturn(prescriptions);

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/by-record/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("按病历查询处方 - 空列表")
    @WithMockUser(roles = "DOCTOR")
    void testGetByRecordId_EmptyList() throws Exception {
        // Given: 返回空列表
        when(prescriptionService.getByRecordId(999L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/common/prescriptions/by-record/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("按病历查询处方 - 病历不存在")
    @WithMockUser(roles = "DOCTOR")
    void testGetByRecordId_RecordNotFound() throws Exception {
        // Given: 病历不存在
        when(prescriptionService.getByRecordId(999L))
                .thenThrow(new IllegalArgumentException("病历不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/prescriptions/by-record/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("病历不存在"));
    }

    @Test
    @DisplayName("按病历查询处方 - 无效ID")
    @WithMockUser(roles = "DOCTOR")
    void testGetByRecordId_InvalidId() throws Exception {
        // Given: 无效ID
        when(prescriptionService.getByRecordId(0L))
                .thenThrow(new IllegalArgumentException("无效的病历ID"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/prescriptions/by-record/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("按病历查询处方 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testGetByRecordId_SystemException() throws Exception {
        // Given: 系统异常
        when(prescriptionService.getByRecordId(1L))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/prescriptions/by-record/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("按病历查询处方 - 未认证用户")
    void testGetByRecordId_Unauthenticated() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 未认证用户会收到401或403
        mockMvc.perform(get("/api/common/prescriptions/by-record/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用处方实体
     */
    private Prescription createTestPrescription() {
        Prescription prescription = new Prescription();
        prescription.setMainId(1L);
        prescription.setPrescriptionNo("RX202501010001");
        prescription.setTotalAmount(new BigDecimal("156.80"));
        prescription.setItemCount(3);
        prescription.setStatus((short) 2); // REVIEWED
        prescription.setPrescriptionType((short) 1);
        prescription.setValidityDays(3);
        prescription.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 30, 0));
        prescription.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 11, 0, 0));

        return prescription;
    }
}
