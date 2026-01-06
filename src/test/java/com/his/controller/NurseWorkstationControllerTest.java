package com.his.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.his.dto.NurseWorkstationDTO;
import com.his.service.NurseWorkstationService;
import com.his.test.base.BaseControllerTest;
import com.his.vo.NurseRegistrationVO;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 护士工作站控制器测试类
 * <p>
 * 测试范围：
 * <ul>
 *   <li>查询今日挂号列表（POST /api/nurse/registrations/today）</li>
 * </ul>
 *
 * <p>覆盖率目标: 75%+
 *
 * @author HIS开发团队
 * @since 1.0.0
 */
@DisplayName("护士工作站控制器测试")
class NurseWorkstationControllerTest extends BaseControllerTest {

    @MockBean
    private NurseWorkstationService nurseWorkstationService;

    // ==================== POST /today - 查询今日挂号列表测试 ====================

    @Test
    @DisplayName("查询今日挂号 - 无过滤条件成功")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_NoFilters_Success() throws Exception {
        // Given: 准备今日挂号列表
        NurseRegistrationVO vo1 = new NurseRegistrationVO();
        vo1.setId(1L);
        vo1.setPatientName("张三");
        vo1.setDeptName("内科");

        NurseRegistrationVO vo2 = new NurseRegistrationVO();
        vo2.setId(2L);
        vo2.setPatientName("李四");
        vo2.setDeptName("外科");

        List<NurseRegistrationVO> registrations = Arrays.asList(vo1, vo2);
        when(nurseWorkstationService.getTodayRegistrations(any())).thenReturn(registrations);

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("2")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].patientName").value("张三"))
                .andExpect(jsonPath("$.data[1].patientName").value("李四"));
    }

    @Test
    @DisplayName("查询今日挂号 - 按科室过滤")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_WithDepartmentFilter_Success() throws Exception {
        // Given: 设置departmentId过滤条件
        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setDepartmentId(1L);

        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);
        vo.setPatientName("张三");
        vo.setDeptName("内科");

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("查询今日挂号 - 按状态过滤")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_WithStatusFilter_Success() throws Exception {
        // Given: 设置status过滤条件
        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setStatus((short) 1);

        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);
        vo.setStatus((short) 1);

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询今日挂号 - 按就诊类型过滤")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_WithVisitTypeFilter_Success() throws Exception {
        // Given: 设置visitType过滤条件
        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setVisitType((short) 1);

        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);
        vo.setVisitType((short) 1);

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询今日挂号 - 按关键词过滤")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_WithKeywordFilter_Success() throws Exception {
        // Given: 设置keyword过滤条件
        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setKeyword("张三");

        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);
        vo.setPatientName("张三");

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询今日挂号 - 多条件组合过滤")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_CombinedFilters_Success() throws Exception {
        // Given: 组合多个过滤条件
        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setDepartmentId(1L);
        dto.setStatus((short) 1);
        dto.setVisitType((short) 1);

        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);
        vo.setPatientName("张三");

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询今日挂号 - 空结果")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_EmptyResult() throws Exception {
        // Given: 返回空列表
        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("0")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("查询今日挂号 - 系统异常")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_SystemException() throws Exception {
        // Given: 系统异常
        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("查询失败")));
    }

    @Test
    @DisplayName("查询今日挂号 - IllegalArgumentException")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_IllegalArgumentException() throws Exception {
        // Given: 参数非法
        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenThrow(new IllegalArgumentException("无效的科室ID"));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的科室ID"));
    }

    @Test
    @DisplayName("查询今日挂号 - 无请求体")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_NoBody_Success() throws Exception {
        // Given: NurseWorkstationDTO的所有字段都是可选的
        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("NURSE角色 - 有权限访问")
    @WithMockUser(roles = "NURSE")
    void testNurseRole_AccessAllowed() throws Exception {
        // Given: 准备数据
        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then: NURSE角色应能访问
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN角色 - 有权限访问")
    @WithMockUser(roles = "ADMIN")
    void testAdminRole_AccessAllowed() throws Exception {
        // Given: 准备数据
        NurseRegistrationVO vo = new NurseRegistrationVO();
        vo.setId(1L);

        when(nurseWorkstationService.getTodayRegistrations(any()))
                .thenReturn(Collections.singletonList(vo));

        // When & Then: ADMIN角色应能访问
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DOCTOR角色 - 无权限访问")
    @WithMockUser(roles = "DOCTOR")
    void testDoctorRole_AccessDenied() throws Exception {
        // Given: DOCTOR角色尝试访问
        // When & Then: 应返回403
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new NurseWorkstationDTO())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户 - 返回401或403")
    void testUnauthenticated_AccessDenied() throws Exception {
        // Given: 不使用@WithMockUser
        // When & Then: 应返回401或403（取决于Security配置）
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    // ==================== 数据验证测试 ====================

    @Test
    @DisplayName("查询今日挂号 - JSON解析错误")
    @WithMockUser(roles = "NURSE")
    void testGetTodayRegistrations_InvalidJson() throws Exception {
        // Given: 无效的JSON
        // When & Then: GlobalExceptionHandler将JSON解析错误返回为500
        mockMvc.perform(post("/api/nurse/registrations/today")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isInternalServerError());
    }
}
