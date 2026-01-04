package com.his.controller;

import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公共药品控制器测试类（重构后版本，支持JsonView）
 * <p>
 * 测试范围：
 * <ul>
 *   <li>药品查询（GET /api/common/medicines） - 分页、多条件筛选</li>
 *   <li>药品详情（GET /api/common/medicines/{id}） - JsonView字段可见性</li>
 *   <li>JsonView功能 - 不同角色看到不同字段</li>
 *   <li>简化搜索（GET /api/common/medicines/search） - @Deprecated</li>
 * </ul>
 *
 * <p>覆盖率目标: 80%+
 *
 * @author HIS开发团队
 * @version 2.0
 */
@DisplayName("公共药品控制器测试（JsonView版本）")
class CommonMedicineControllerTest extends BaseControllerTest {

    @MockBean
    private MedicineService medicineService;

    // ==================== GET / - 分页查询测试 ====================

    @Test
    @DisplayName("分页查询 - DOCTOR角色成功")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_Doctor_Success() throws Exception {
        // Given: 准备分页数据
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.content[0].retailPrice").exists())
                // Doctor视图应该包含这些字段
                .andExpect(jsonPath("$.data.content[0].specification").exists())
                .andExpect(jsonPath("$.data.content[0].unit").exists())
                .andExpect(jsonPath("$.data.content[0].stockStatus").exists())
                // 但不应该包含进货价
                .andExpect(jsonPath("$.data.content[0].purchasePrice").doesNotExist());
    }

    @Test
    @DisplayName("分页查询 - PHARMACIST角色能看到进货价")
    @WithMockUser(roles = "PHARMACIST")
    void testSearch_Pharmacist_SeesPurchasePrice() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then: Pharmacist视图应该包含进货价
        mockMvc.perform(get("/api/common/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].purchasePrice").value(18.50))
                .andExpect(jsonPath("$.data.content[0].profitMargin").exists())
                .andExpect(jsonPath("$.data.content[0].minStock").value(50))
                .andExpect(jsonPath("$.data.content[0].maxStock").value(500));
    }

    @Test
    @DisplayName("分页查询 - ADMIN角色等同于PHARMACIST")
    @WithMockUser(roles = "ADMIN")
    void testSearch_Admin_SeesPurchasePrice() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then: ADMIN应该和PHARMACIST一样看到进货价
        mockMvc.perform(get("/api/common/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].purchasePrice").value(18.50));
    }

    @Test
    @DisplayName("分页查询 - 无角色用户只看到Public字段")
    @WithMockUser
    void testSearch_Public_OnlyBasicFields() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then: Public视图不应该包含这些字段
        mockMvc.perform(get("/api/common/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").exists())
                .andExpect(jsonPath("$.data.content[0].retailPrice").exists())
                .andExpect(jsonPath("$.data.content[0].specification").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].unit").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].stockStatus").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].purchasePrice").doesNotExist());
    }

    @Test
    @DisplayName("关键字搜索 - DOCTOR角色")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_Keyword_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            eq("阿莫西林"), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("keyword", "阿莫西林"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林胶囊"));
    }

    @Test
    @DisplayName("按分类查询 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_Category_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), eq("抗生素"), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("category", "抗生素"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].category").value("抗生素"));
    }

    @Test
    @DisplayName("按是否处方药查询 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_IsPrescription_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), eq((short) 1), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("isPrescription", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].isPrescription").value(1)); // Changed from (short) 1 to 1
    }

    @Test
    @DisplayName("只显示有货药品 - 成功")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_InStock_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), eq(true), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(100));
    }

    @Test
    @DisplayName("药师查询价格区间 - 成功")
    @WithMockUser(roles = "PHARMACIST")
    void testSearch_PriceRange_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(),
            any(), eq(new BigDecimal("10")), eq(new BigDecimal("50")), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("minPrice", "10")
                .param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retailPrice").value(25.80));
    }

    @Test
    @DisplayName("药师按厂家查询 - 成功")
    @WithMockUser(roles = "PHARMACIST")
    void testSearch_Manufacturer_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), any(),
            eq("某某制药有限公司"), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("manufacturer", "某某制药有限公司"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].manufacturer").value("某某制药有限公司"));
    }

    @Test
    @DisplayName("药师查询低库存药品 - 成功")
    @WithMockUser(roles = "PHARMACIST")
    void testSearch_LowStock_Success() throws Exception {
        // Given: Create a medicine with low stock
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(30); // Lower than minStock (50), so stockStatus will be "LOW_STOCK"
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForPharmacist(
            any(), any(), any(), eq("LOW"),
            any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("stockStatus", "LOW"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].stockStatus").value("LOW_STOCK"));
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_EmptyResult() throws Exception {
        // Given: 返回空页
        Page<Medicine> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 20),
            0
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("keyword", "不存在的药"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("分页查询 - 支持排序")
    @WithMockUser(roles = "DOCTOR")
    void testSearch_WithSort_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        Page<Medicine> page = new PageImpl<>(
            Arrays.asList(medicine),
            PageRequest.of(0, 20),
            1
        );

        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/common/medicines")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ==================== GET /{id} - 详情查询测试 ====================

    @Test
    @DisplayName("查询详情 - DOCTOR角色成功")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_Doctor_Success() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        when(medicineService.getById(1L)).thenReturn(medicine);

        // When & Then
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.name").value("阿莫西林胶囊"))
                // Doctor视图应该包含这些字段
                .andExpect(jsonPath("$.data.specification").value("0.25g*24粒"))
                .andExpect(jsonPath("$.data.unit").value("盒"))
                .andExpect(jsonPath("$.data.dosageForm").value("胶囊"))
                .andExpect(jsonPath("$.data.manufacturer").value("某某制药有限公司"))
                .andExpect(jsonPath("$.data.stockStatus").exists())
                // 但不应该包含进货价
                .andExpect(jsonPath("$.data.purchasePrice").doesNotExist())
                .andExpect(jsonPath("$.data.profitMargin").doesNotExist());
    }

    @Test
    @DisplayName("查询详情 - PHARMACIST角色能看到进货价")
    @WithMockUser(roles = "PHARMACIST")
    void testGetById_Pharmacist_SeesSensitiveFields() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        when(medicineService.getById(1L)).thenReturn(medicine);

        // When & Then: Pharmacist视图应该包含所有敏感字段
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchasePrice").value(18.50))
                .andExpect(jsonPath("$.data.profitMargin").exists())
                .andExpect(jsonPath("$.data.minStock").value(50))
                .andExpect(jsonPath("$.data.maxStock").value(500))
                .andExpect(jsonPath("$.data.storageCondition").value("密闭，在阴凉干燥处保存"))
                .andExpect(jsonPath("$.data.approvalNo").value("国药准字H12345678"));
    }

    @Test
    @DisplayName("查询详情 - 无角色用户只看到Public字段")
    @WithMockUser
    void testGetById_Public_OnlyBasicFields() throws Exception {
        // Given
        Medicine medicine = createTestMedicine();
        when(medicineService.getById(1L)).thenReturn(medicine);

        // When & Then: Public视图不应该包含这些字段
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.retailPrice").exists())
                .andExpect(jsonPath("$.data.specification").doesNotExist())
                .andExpect(jsonPath("$.data.unit").doesNotExist())
                .andExpect(jsonPath("$.data.stockStatus").doesNotExist())
                .andExpect(jsonPath("$.data.purchasePrice").doesNotExist());
    }

    @Test
    @DisplayName("查询详情 - 药品不存在")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_NotFound() throws Exception {
        // Given
        when(medicineService.getById(999L))
                .thenThrow(new IllegalArgumentException("药品不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/medicines/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询详情 - 无效ID")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_InvalidId() throws Exception {
        // Given
        when(medicineService.getById(0L))
                .thenThrow(new IllegalArgumentException("无效的药品ID"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/medicines/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询详情 - 系统异常")
    @WithMockUser(roles = "DOCTOR")
    void testGetById_SystemException() throws Exception {
        // Given
        when(medicineService.getById(1L))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET /search - 简化搜索测试 ====================

    @Test
    @DisplayName("简化搜索 - 有关键词返回结果")
    @WithMockUser
    void testSearchSimple_WithKeyword_Success() throws Exception {
        // Given: 准备药品列表
        Medicine medicine1 = new Medicine();
        medicine1.setMainId(1L);
        medicine1.setName("阿莫西林");

        Medicine medicine2 = new Medicine();
        medicine2.setMainId(2L);
        medicine2.setName("阿司匹林");

        List<Medicine> medicines = Arrays.asList(medicine1, medicine2);
        when(medicineService.searchMedicines("阿")).thenReturn(medicines);

        // When & Then
        mockMvc.perform(get("/api/common/medicines/search")
                        .param("keyword", "阿"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("阿莫西林"))
                .andExpect(jsonPath("$.data[1].name").value("阿司匹林"));
    }

    @Test
    @DisplayName("简化搜索 - 无关键词返回全部")
    @WithMockUser
    void testSearchSimple_WithoutKeyword_Success() throws Exception {
        // Given: 准备所有药品
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setName("布洛芬");

        when(medicineService.searchMedicines(null))
                .thenReturn(Arrays.asList(medicine));

        // When & Then
        mockMvc.perform(get("/api/common/medicines/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("简化搜索 - 空结果")
    @WithMockUser
    void testSearchSimple_EmptyResult() throws Exception {
        // Given: 返回空列表
        when(medicineService.searchMedicines("不存在的药"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/common/medicines/search")
                        .param("keyword", "不存在的药"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("简化搜索 - 系统异常")
    @WithMockUser
    void testSearchSimple_SystemException() throws Exception {
        // Given: 系统异常
        when(medicineService.searchMedicines("阿"))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/medicines/search")
                        .param("keyword", "阿"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("未认证用户访问应该被拒绝")
    void testAccessDenied_Unauthenticated() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 未认证用户会收到401或403
        mockMvc.perform(get("/api/common/medicines"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
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
        medicine.setCategory("抗生素");
        medicine.setIsPrescription((short) 1);
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setDosageForm("胶囊");
        medicine.setManufacturer("某某制药有限公司");
        medicine.setStorageCondition("密闭，在阴凉干燥处保存");
        medicine.setApprovalNo("国药准字H12345678");
        medicine.setExpiryWarningDays(90);
        medicine.setStatus((short) 1);
        medicine.setCreatedAt(java.time.LocalDateTime.now());
        medicine.setUpdatedAt(java.time.LocalDateTime.now());

        return medicine;
    }
}
