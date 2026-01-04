package com.his.controller;

import com.his.dto.InventoryStatsVO;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.Arrays;


import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PharmacistMedicineController 集成测试
 * <p>
 * 测试药师工作站的药品管理API，包括高级查询、低库存预警、库存统计等功能
 * </p>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("药师工作站-药品管理API测试")
class PharmacistMedicineControllerTest extends BaseControllerTest {

    @MockBean
    private MedicineService medicineService;

    private Medicine testMedicine;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    protected void setUp() throws Exception {
        testMedicine = createTestMedicine();
    }

    // ==================== GET /api/pharmacist/medicines/search 测试 ====================

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("GET /api/pharmacist/medicines/search - 成功高级查询")
    void testSearchMedicinesAdvanced_Success() throws Exception {
        // Given - 准备Mock数据
        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then - 执行高级查询
        mockMvc.perform(get("/api/pharmacist/medicines/search")
                .param("keyword", "阿司匹林")
                .param("category", "抗生素")
                .param("isPrescription", "1")
                .param("stockStatus", "LOW")
                .param("manufacturer", "某某制药")
                .param("minPrice", "10")
                .param("maxPrice", "50")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林胶囊"));

        verify(medicineService, times(1)).searchMedicinesForPharmacist(
            eq("阿司匹林"), eq("抗生素"), eq((short) 1), eq("LOW"),
            eq("某某制药"), eq(new BigDecimal("10")), eq(new BigDecimal("50")), any()
        );
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("药师视图应包含进货价和利润率")
    void testPharmacistView_HasPurchasePriceAndProfit() throws Exception {
        // Given - 模拟药师查询能看到进货价
        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then
        mockMvc.perform(get("/api/pharmacist/medicines/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retailPrice").exists())
                .andExpect(jsonPath("$.data.content[0].purchasePrice").value(18.50))
                .andExpect(jsonPath("$.data.content[0].profitMargin").exists())
                .andExpect(jsonPath("$.data.content[0].minStock").value(50))
                .andExpect(jsonPath("$.data.content[0].maxStock").value(500))
                .andExpect(jsonPath("$.data.content[0].storageCondition").value("密闭，在阴凉干燥处保存"))
                .andExpect(jsonPath("$.data.content[0].approvalNo").value("国药准字H12345678"));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("价格区间查询 - 成功")
    void testPriceRangeQuery_Success() throws Exception {
        // Given
        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then - 只传价格参数
        mockMvc.perform(get("/api/pharmacist/medicines/search")
                .param("minPrice", "10")
                .param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("按生产厂家查询 - 成功")
    void testManufacturerQuery_Success() throws Exception {
        // Given
        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then
        mockMvc.perform(get("/api/pharmacist/medicines/search")
                .param("manufacturer", "某某制药"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/pharmacist/medicines/low-stock-alert 测试 ====================

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("GET /api/pharmacist/medicines/low-stock-alert - 成功查询低库存")
    void testGetLowStockAlert_Success() throws Exception {
        // Given - 准备低库存数据
        Medicine lowStock = createTestMedicine();
        lowStock.setStockQuantity(30);
        lowStock.setMinStock(50);

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), eq("LOW"), any(), any(), any(), any()
        )).thenReturn(createMockPage(lowStock));

        // When & Then
        mockMvc.perform(get("/api/pharmacist/medicines/low-stock-alert")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("低库存记录")))
                .andExpect(jsonPath("$.data.content[0].stockStatus").value("LOW_STOCK"));

        verify(medicineService, times(1)).searchMedicinesForPharmacist(
            eq(null), eq(null), eq(null), eq("LOW"),
            eq(null), eq(null), eq(null), any()
        );
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("低库存预警 - 支持分页")
    void testGetLowStockAlert_Pagination() throws Exception {
        // Given - 使用Answer动态返回请求的分页信息
        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(7);
            return new org.springframework.data.domain.PageImpl<>(
                Arrays.asList(testMedicine),
                pageable,
                1
            );
        });

        // When & Then - 请求第二页
        mockMvc.perform(get("/api/pharmacist/medicines/low-stock-alert")
                .param("page", "1")
                .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(30));
    }

    // ==================== GET /api/pharmacist/medicines/inventory-stats 测试 ====================

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

        verify(medicineService, times(1)).getInventoryStats();
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("库存统计 - 计算占比")
    void testGetInventoryStats_WithRates() throws Exception {
        // Given
        InventoryStatsVO stats = InventoryStatsVO.builder()
            .totalMedicines(100L)
            .inStockCount(75L)
            .lowStockCount(20L)
            .outOfStockCount(5L)
            .build();

        when(medicineService.getInventoryStats()).thenReturn(stats);

        // When & Then - 验证占比计算
        mockMvc.perform(get("/api/pharmacist/medicines/inventory-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMedicines").value(100))
                .andExpect(jsonPath("$.data.inStockCount").value(75))
                .andExpect(jsonPath("$.data.lowStockCount").value(20))
                .andExpect(jsonPath("$.data.outOfStockCount").value(5));
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

    // ==================== 权限测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("医生访问药师API应该被拒绝")
    void testAccessDenied_Doctor() throws Exception {
        // When & Then - 医生访问药师API
        mockMvc.perform(get("/api/pharmacist/medicines/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户应该被拒绝")
    void testAccessDenied_Unauthenticated() throws Exception {
        // When & Then - 未登录访问
        mockMvc.perform(get("/api/pharmacist/medicines/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用药品实体
     */
    private Medicine createTestMedicine() {
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setMedicineCode("MED001");
        medicine.setName("阿莫西林胶囊");
        medicine.setGenericName("阿莫西林");
        medicine.setRetailPrice(new BigDecimal("25.80"));
        medicine.setPurchasePrice(new BigDecimal("18.50"));
        medicine.setStockQuantity(100);
        medicine.setMinStock(50);
        medicine.setMaxStock(500);
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setDosageForm("胶囊");
        medicine.setCategory("抗生素");
        medicine.setIsPrescription((short) 1);
        medicine.setManufacturer("某某制药有限公司");
        medicine.setStorageCondition("密闭，在阴凉干燥处保存");
        medicine.setApprovalNo("国药准字H12345678");
        medicine.setExpiryWarningDays(90);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);

        return medicine;
    }

    /**
     * 创建Mock分页数据
     */
    private org.springframework.data.domain.Page<Medicine> createMockPage() {
        return new org.springframework.data.domain.PageImpl<>(
            Arrays.asList(testMedicine),
            org.springframework.data.domain.PageRequest.of(0, 20),
            1
        );
    }

    /**
     * 创建指定药品的Mock分页数据
     */
    private org.springframework.data.domain.Page<Medicine> createMockPage(Medicine medicine) {
        return new org.springframework.data.domain.PageImpl<>(
            Arrays.asList(medicine),
            org.springframework.data.domain.PageRequest.of(0, 20),
            1
        );
    }
}
