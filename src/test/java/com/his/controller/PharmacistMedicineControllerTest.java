package com.his.controller;

import com.his.dto.InventoryStatsVO;
import com.his.service.MedicineService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PharmacistMedicineController 集成测试（重构后版本）
 * <p>
 * 测试药师工作站的药品操作API，不再包含查询功能（查询已移至/api/common）
 * </p>
 *
 * <p>测试范围：</p>
 * <ul>
 *   <li>更新药品库存（PUT /api/pharmacist/medicines/{id}/stock）</li>
 *   <li>库存统计（GET /api/pharmacist/medicines/inventory-stats）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 2.0
 */
@DisplayName("药师工作站-药品操作API测试")
class PharmacistMedicineControllerTest extends BaseControllerTest {

    @MockBean
    private MedicineService medicineService;

    // ==================== PUT /{id}/stock - 更新库存测试 ====================

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("PUT /api/pharmacist/medicines/{id}/stock - 成功入库")
    void testUpdateStock_Inbound_Success() throws Exception {
        // Given - 模拟入库操作
        // void方法，不需要mock返回值

        // When & Then - 执行入库（quantity为正数）
        mockMvc.perform(put("/api/pharmacist/medicines/1/stock")
                .param("quantity", "100")
                .param("reason", "采购入库"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("库存更新成功"));

        verify(medicineService).updateStock(1L, 100, "采购入库");
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("PUT /api/pharmacist/medicines/{id}/stock - 成功出库")
    void testUpdateStock_Outbound_Success() throws Exception {
        // Given - 模拟出库操作
        // void方法，不需要mock返回值

        // When & Then - 执行出库（quantity为负数）
        mockMvc.perform(put("/api/pharmacist/medicines/1/stock")
                .param("quantity", "-5")
                .param("reason", "发药消耗"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(medicineService).updateStock(1L, -5, "发药消耗");
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("更新库存 - 缺少reason参数")
    void testUpdateStock_MissingReason_Fail() throws Exception {
        // When & Then - 缺少必填参数reason
        mockMvc.perform(put("/api/pharmacist/medicines/1/stock")
                .param("quantity", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("更新库存 - 缺少quantity参数")
    void testUpdateStock_MissingQuantity_Fail() throws Exception {
        // When & Then - 缺少必填参数quantity
        mockMvc.perform(put("/api/pharmacist/medicines/1/stock")
                .param("reason", "采购入库"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("更新库存 - 库存不足")
    void testUpdateStock_InsufficientStock() throws Exception {
        // Given - 库存不足异常
        org.mockito.Mockito.doThrow(new IllegalArgumentException("库存不足"))
                .when(medicineService).updateStock(1L, -1000, "发药消耗");

        // When & Then - GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(put("/api/pharmacist/medicines/1/stock")
                .param("quantity", "-1000")
                .param("reason", "发药消耗"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("更新库存 - 药品不存在")
    void testUpdateStock_MedicineNotFound() throws Exception {
        // Given - 药品不存在异常
        org.mockito.Mockito.doThrow(new IllegalArgumentException("药品不存在"))
                .when(medicineService).updateStock(999L, 100, "采购入库");

        // When & Then - GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(put("/api/pharmacist/medicines/999/stock")
                .param("quantity", "100")
                .param("reason", "采购入库"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("药品不存在"));
    }

    // ==================== GET /inventory-stats - 库存统计测试 ====================

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("GET /api/pharmacist/medicines/inventory-stats - 成功获取统计")
    void testGetInventoryStats_Success() throws Exception {
        // Given - 准备统计数据
        InventoryStatsVO stats = InventoryStatsVO.builder()
            .totalMedicines(100L)
            .inStockCount(75L)
            .lowStockCount(20L)
            .outOfStockCount(5L)
            .build();

        when(medicineService.getInventoryStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.totalMedicines").value(100))
                .andExpect(jsonPath("$.data.inStockCount").value(75))
                .andExpect(jsonPath("$.data.lowStockCount").value(20))
                .andExpect(jsonPath("$.data.outOfStockCount").value(5));

        verify(medicineService, org.mockito.Mockito.times(1)).getInventoryStats();
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("库存统计 - 空数据")
    void testGetInventoryStats_EmptyData() throws Exception {
        // Given
        InventoryStatsVO emptyStats = InventoryStatsVO.builder()
            .totalMedicines(0L)
            .inStockCount(0L)
            .lowStockCount(0L)
            .outOfStockCount(0L)
            .build();

        when(medicineService.getInventoryStats()).thenReturn(emptyStats);

        // When & Then
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMedicines").value(0))
                .andExpect(jsonPath("$.data.inStockCount").value(0))
                .andExpect(jsonPath("$.data.lowStockCount").value(0))
                .andExpect(jsonPath("$.data.outOfStockCount").value(0));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("库存统计 - 系统异常")
    void testGetInventoryStats_SystemException() throws Exception {
        // Given - 系统异常
        when(medicineService.getInventoryStats())
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then - GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== 权限测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("医生访问药师API应该被拒绝")
    void testAccessDenied_Doctor() throws Exception {
        // When & Then - 医生访问药师API
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户应该被拒绝")
    void testAccessDenied_Unauthenticated() throws Exception {
        // When & Then - 未登录访问
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
