package com.his.controller;

import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公共药品控制器测试类
 * <p>
 * 测试范围：
 * <ul>
 *   <li>搜索药品（GET /api/common/medicines/search）</li>
 *   <li>查询药品详情（GET /api/common/medicines/{id}）</li>
 * </ul>
 *
 * <p>覆盖率目标: 75%+
 *
 * @author HIS开发团队
 * @since 1.0.0
 */
@DisplayName("公共药品控制器测试")
class CommonMedicineControllerTest extends BaseControllerTest {

    @MockBean
    private MedicineService medicineService;

    // ==================== GET /search - 搜索药品测试 ====================

    @Test
    @DisplayName("搜索药品 - 有关键词返回结果")
    @WithMockUser
    void testSearchMedicines_WithKeyword_Success() throws Exception {
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
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("阿莫西林"))
                .andExpect(jsonPath("$.data[1].name").value("阿司匹林"));
    }

    @Test
    @DisplayName("搜索药品 - 无关键词返回全部")
    @WithMockUser
    void testSearchMedicines_WithoutKeyword_Success() throws Exception {
        // Given: 准备所有药品
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setName("布洛芬");

        when(medicineService.searchMedicines(null))
                .thenReturn(Arrays.asList(medicine));

        // When & Then
        mockMvc.perform(get("/api/common/medicines/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("搜索药品 - 空结果")
    @WithMockUser
    void testSearchMedicines_EmptyResult() throws Exception {
        // Given: 返回空列表
        when(medicineService.searchMedicines("不存在的药"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/common/medicines/search")
                        .param("keyword", "不存在的药"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("搜索药品 - 系统异常")
    @WithMockUser
    void testSearchMedicines_SystemException() throws Exception {
        // Given: 系统异常
        when(medicineService.searchMedicines("阿"))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/medicines/search")
                        .param("keyword", "阿"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("搜索药品 - 未认证用户")
    void testSearchMedicines_Unauthenticated() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 未认证用户会收到401或403（取决于Security配置）
        mockMvc.perform(get("/api/common/medicines/search"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    // ==================== GET /{id} - 查询药品详情测试 ====================

    @Test
    @DisplayName("查询药品详情 - 成功")
    @WithMockUser
    void testGetMedicineById_Success() throws Exception {
        // Given: 准备药品数据
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setName("阿莫西林");
        medicine.setRetailPrice(new java.math.BigDecimal("15.50"));

        when(medicineService.getById(1L)).thenReturn(medicine);

        // When & Then
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.mainId").value(1))
                .andExpect(jsonPath("$.data.name").value("阿莫西林"))
                .andExpect(jsonPath("$.data.retailPrice").value(15.50));
    }

    @Test
    @DisplayName("查询药品详情 - 药品不存在")
    @WithMockUser
    void testGetMedicineById_NotFound() throws Exception {
        // Given: 药品不存在
        when(medicineService.getById(999L))
                .thenThrow(new IllegalArgumentException("药品不存在"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/medicines/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询药品详情 - 无效ID")
    @WithMockUser
    void testGetMedicineById_InvalidId() throws Exception {
        // Given: 无效ID
        when(medicineService.getById(0L))
                .thenThrow(new IllegalArgumentException("无效的药品ID"));

        // When & Then: GlobalExceptionHandler返回HTTP 400
        mockMvc.perform(get("/api/common/medicines/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询药品详情 - 系统异常")
    @WithMockUser
    void testGetMedicineById_SystemException() throws Exception {
        // Given: 系统异常
        when(medicineService.getById(1L))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then: GlobalExceptionHandler返回HTTP 500
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("查询药品详情 - 未认证用户")
    void testGetMedicineById_Unauthenticated() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 未认证用户会收到401或403（取决于Security配置）
        mockMvc.perform(get("/api/common/medicines/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }
}
