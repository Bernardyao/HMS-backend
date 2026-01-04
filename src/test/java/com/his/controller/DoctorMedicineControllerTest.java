package com.his.controller;

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
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医生工作站药品查询控制器测试类
 *
 * <p>全面测试医生工作站的药品查询API，包括综合查询、详情查询、分类查询等</p>
 *
 * <h3>测试策略</h3>
 * <ul>
 *   <li><b>MockMvc</b>：使用MockMvc模拟HTTP请求和响应，验证API契约</li>
 *   <li><b>MockBean</b>：使用@MockBean隔离Service层，专注于Controller层测试</li>
 *   <li><b>安全测试</b>：使用@WithMockUser模拟医生登录，验证权限控制</li>
 *   <li><b>数据验证</b>：验证医生视图不包含敏感商业信息（进货价、利润率）</li>
 * </ul>
 *
 * <h3>测试覆盖范围</h3>
 * <ul>
 *   <li><b>综合查询</b>：无过滤条件、关键字搜索、分类筛选、处方药筛选、库存筛选</li>
 *   <li><b>分页功能</b>：验证分页参数正确传递到Service层</li>
 *   <li><b>排序功能</b>：验证排序参数正确传递</li>
 *   <li><b>详情查询</b>：正常查询、药品不存在</li>
 *   <li><b>分类查询</b>：按分类筛选、支持分页</li>
 *   <li><b>权限控制</b>：药师和未认证用户访问被拒绝</li>
 *   <li><b>数据安全</b>：医生视图不包含进货价等敏感信息</li>
 *   <li><b>库存状态</b>：验证库存状态正确显示（IN_STOCK/LOW_STOCK/OUT_OF_STOCK）</li>
 * </ul>
 *
 * <h3>测试数据</h3>
 * <ul>
 *   <li>使用Mock数据避免依赖数据库</li>
 *   <li>测试数据包含边界值（空结果、分页边界）</li>
 *   <li>验证医生视图只包含零售价，不包含进货价和利润率</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.controller.DoctorMedicineController
 */
@DisplayName("医生工作站-药品查询API测试")
class DoctorMedicineControllerTest extends BaseControllerTest {

    @MockBean
    private MedicineService medicineService;

    private Medicine testMedicine;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    protected void setUp() {
        testMedicine = createTestMedicine();
    }

    // ==================== GET /api/doctor/medicines 测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines - 成功查询药品列表")
    void testSearchMedicines_Success() throws Exception {
        // Given - 准备Mock数据
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then - 执行请求并验证结果
        mockMvc.perform(get("/api/doctor/medicines")
                .param("keyword", "阿莫西林")
                .param("category", "抗生素")
                .param("isPrescription", "1")
                .param("inStock", "true")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "name,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].mainId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.content[0].stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.data.content[0].retailPrice").exists());

        verify(medicineService, times(1)).searchMedicinesForDoctor(
            eq("阿莫西林"), eq("抗生素"), eq((short) 1), eq(true), any()
        );
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines - 空结果")
    void testSearchMedicines_EmptyResult() throws Exception {
        // Given - 准备Mock空数据
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createEmptyMockPage());

        // When & Then - 执行请求
        mockMvc.perform(get("/api/doctor/medicines")
                .param("keyword", "不存在的药品"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines - 所有参数可选")
    void testSearchMedicines_AllParamsOptional() throws Exception {
        // Given
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then - 不传任何参数
        mockMvc.perform(get("/api/doctor/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines - 分页功能")
    void testSearchMedicines_Pagination() throws Exception {
        // Given - 使用Answer动态返回请求的分页信息
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(4);
            return new org.springframework.data.domain.PageImpl<>(
                Arrays.asList(testMedicine),
                pageable,
                1
            );
        });

        // When & Then - 请求第二页
        mockMvc.perform(get("/api/doctor/medicines")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10));
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines - 排序功能")
    void testSearchMedicines_Sorting() throws Exception {
        // Given
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then - 按库存降序排序
        mockMvc.perform(get("/api/doctor/medicines")
                .param("sort", "stockQuantity,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/doctor/medicines/{id} 测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines/{id} - 成功查询药品详情")
    void testGetMedicineDetail_Success() throws Exception {
        // Given
        when(medicineService.getById(1L)).thenReturn(testMedicine);

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.name").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.data.retailPrice").exists())
                .andExpect(jsonPath("$.data.purchasePrice").doesNotExist()); // 医生看不到进货价

        verify(medicineService, times(1)).getById(1L);
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines/{id} - 药品不存在")
    void testGetMedicineDetail_NotFound() throws Exception {
        // Given
        when(medicineService.getById(999L))
            .thenThrow(new IllegalArgumentException("药品不存在，ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("药品不存在")));
    }

    // ==================== GET /api/doctor/medicines/by-category/{category} 测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines/by-category/{category} - 成功按分类查询")
    void testGetByCategory_Success() throws Exception {
        // Given
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/by-category/抗生素"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询成功")));

        verify(medicineService, times(1)).searchMedicinesForDoctor(
            eq(null), eq("抗生素"), eq(null), eq(null), any()
        );
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("GET /api/doctor/medicines/by-category/{category} - 带分页")
    void testGetByCategory_WithPagination() throws Exception {
        // Given
        when(medicineService.searchMedicinesForDoctor(
            any(), any(), any(), any(), any()
        )).thenReturn(createMockPage());

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/by-category/解热镇痛药")
                .param("page", "0")
                .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 权限测试 ====================

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("药师访问医生API应该被拒绝")
    void testAccessDenied_Pharmacist() throws Exception {
        // When & Then - 药师访问医生API
        mockMvc.perform(get("/api/doctor/medicines")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户应该被拒绝")
    void testAccessDenied_Unauthenticated() throws Exception {
        // When & Then - 未登录访问
        mockMvc.perform(get("/api/doctor/medicines")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==================== 数据验证测试 ====================

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("医生视图不应包含进货价")
    void testDoctorView_NoPurchasePrice() throws Exception {
        // Given
        when(medicineService.getById(1L)).thenReturn(testMedicine);

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retailPrice").exists())
                .andExpect(jsonPath("$.data.purchasePrice").doesNotExist())
                .andExpect(jsonPath("$.data.profitMargin").doesNotExist());
    }

    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    @DisplayName("医生视图应包含库存状态")
    void testDoctorView_HasStockStatus() throws Exception {
        // Given
        Medicine lowStockMedicine = createTestMedicine();
        lowStockMedicine.setStockQuantity(30);
        lowStockMedicine.setMinStock(50);
        when(medicineService.getById(1L)).thenReturn(lowStockMedicine);

        // When & Then
        mockMvc.perform(get("/api/doctor/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockStatus").value("LOW_STOCK"));
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
     * 创建空的Mock分页数据
     */
    private org.springframework.data.domain.Page<Medicine> createEmptyMockPage() {
        return new org.springframework.data.domain.PageImpl<>(
            Collections.emptyList(),
            org.springframework.data.domain.PageRequest.of(0, 20),
            0
        );
    }
}
