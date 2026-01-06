package com.his.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.his.entity.AuditLogEntity;
import com.his.service.AuditLogQueryService;
import com.his.test.base.BaseControllerTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 审计日志控制器测试类
 *
 * <p>测试审计日志查询API的功能</p>
 *
 * <h3>测试策略</h3>
 * <ul>
 *   <li><b>MockMvc</b>：使用MockMvc测试HTTP请求和响应</li>
 *   <li><b>MockBean</b>：使用MockBean隔离Service层</li>
 *   <li><b>安全测试</b>：使用@WithMockUser模拟管理员登录</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@DisplayName("审计日志控制器测试")
class AuditLogControllerTest extends BaseControllerTest {

    @MockBean
    private AuditLogQueryService auditLogQueryService;

    // ==================== searchAuditLogs 测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够搜索审计日志（无过滤条件）")
    void testSearchAuditLogs_NoFilters() throws Exception {
        // Given - 准备测试数据
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setModule("认证管理");
        log1.setAction("用户登录");
        log1.setAuditType("SENSITIVE_OPERATION");
        log1.setStatus("SUCCESS");
        log1.setCreateTime(LocalDateTime.now());

        Page<AuditLogEntity> page = new PageImpl<>(Arrays.asList(log1), PageRequest.of(0, 20), 1);
        when(auditLogQueryService.searchAuditLogs(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then - 执行测试并验证结果
        mockMvc.perform(get("/api/audit-logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].module").value("认证管理"))
                .andExpect(jsonPath("$.data.content[0].action").value("用户登录"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够按模块搜索审计日志")
    void testSearchAuditLogs_ByModule() throws Exception {
        // Given - 准备测试数据
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setModule("挂号管理");

        Page<AuditLogEntity> page = new PageImpl<>(Arrays.asList(log1), PageRequest.of(0, 20), 1);
        when(auditLogQueryService.searchAuditLogs(
                eq("挂号管理"), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/search")
                        .param("module", "挂号管理")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].module").value("挂号管理"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够按时间范围搜索审计日志")
    void testSearchAuditLogs_ByTimeRange() throws Exception {
        // Given - 准备测试数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setModule("测试模块");

        Page<AuditLogEntity> page = new PageImpl<>(Arrays.asList(log1), PageRequest.of(0, 20), 1);
        when(auditLogQueryService.searchAuditLogs(
                any(), any(), any(), any(), eq(startTime), eq(endTime), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/search")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够按审计类型搜索")
    void testSearchAuditLogs_ByAuditType() throws Exception {
        // Given
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setAuditType("SENSITIVE_OPERATION");

        Page<AuditLogEntity> page = new PageImpl<>(Arrays.asList(log1), PageRequest.of(0, 20), 1);
        when(auditLogQueryService.searchAuditLogs(
                any(), any(), any(), eq("SENSITIVE_OPERATION"), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/search")
                        .param("auditType", "SENSITIVE_OPERATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].auditType").value("SENSITIVE_OPERATION"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该支持分页查询")
    void testSearchAuditLogs_WithPagination() throws Exception {
        // Given
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);

        Page<AuditLogEntity> page = new PageImpl<>(Arrays.asList(log1), PageRequest.of(0, 10), 1);
        when(auditLogQueryService.searchAuditLogs(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/search")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createTime,desc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0));
    }

    // ==================== getAuditLogsByTraceId 测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够根据TraceId查询审计日志")
    void testGetAuditLogsByTraceId() throws Exception {
        // Given
        String traceId = "test-trace-id-1234567890abcdef";

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setTraceId(traceId);
        log1.setModule("测试模块");

        List<AuditLogEntity> logs = Arrays.asList(log1);
        when(auditLogQueryService.getAuditLogsByTraceId(traceId)).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/trace/{traceId}", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].traceId").value(traceId))
                .andExpect(jsonPath("$.data[0].module").value("测试模块"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("TraceId查询结果为空时应该返回空数组")
    void testGetAuditLogsByTraceId_EmptyResult() throws Exception {
        // Given
        String traceId = "non-existent-trace-id";
        when(auditLogQueryService.getAuditLogsByTraceId(traceId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/audit-logs/trace/{traceId}", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== getAuditLogsByOperator 测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够查询操作人的审计日志")
    void testGetAuditLogsByOperator() throws Exception {
        // Given
        Long operatorId = 123L;

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setOperatorId(operatorId);
        log1.setOperatorUsername("admin");
        log1.setModule("认证管理");

        List<AuditLogEntity> logs = Arrays.asList(log1);
        when(auditLogQueryService.getAuditLogsByOperator(operatorId)).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/audit-logs/operator/{operatorId}", operatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].operatorId").value(operatorId))
                .andExpect(jsonPath("$.data[0].operatorUsername").value("admin"))
                .andExpect(jsonPath("$.data[0].module").value("认证管理"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("操作人查询结果为空时应该返回空数组")
    void testGetAuditLogsByOperator_EmptyResult() throws Exception {
        // Given
        Long operatorId = 999L;
        when(auditLogQueryService.getAuditLogsByOperator(operatorId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/audit-logs/operator/{operatorId}", operatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("非管理员用户不应该访问审计日志接口")
    void testAccessDenied_NonAdmin() throws Exception {
        // When & Then - 使用非管理员角色
        mockMvc.perform(get("/api/audit-logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
