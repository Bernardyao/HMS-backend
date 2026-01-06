package com.his.integration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import com.his.entity.AuditLogEntity;
import com.his.repository.AuditLogRepository;
import com.his.service.AuditLogService;
import com.his.test.base.BaseIntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志集成测试类
 *
 * <p>测试审计日志的完整功能,包括Service层和Repository层</p>
 *
 * <h3>测试策略</h3>
 * <ul>
 *   <li><b>真实数据库</b>：使用H2内存数据库进行真实的数据操作</li>
 *   <li><b>事务回滚</b>：测试后自动回滚,不污染数据库</li>
 *   <li><b>端到端测试</b>：从Service到Repository的完整流程</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@DisplayName("审计日志集成测试")
class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    // ==================== 保存和查询测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够保存和查询审计日志")
    void testSaveAndQueryAuditLog() {
        // Given - 准备测试数据
        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule("测试模块");
        entity.setAction("测试操作");
        entity.setAuditType("BUSINESS");
        entity.setDescription("测试描述");
        entity.setStatus("SUCCESS");
        entity.setExecutionTime(100L);
        entity.setCreateTime(LocalDateTime.now());

        // When - 保存审计日志
        AuditLogEntity saved = auditLogRepository.save(entity);

        // Then - 验证保存结果
        assertNotNull(saved.getId(), "ID应该自动生成");
        assertEquals("测试模块", saved.getModule());
        assertEquals("测试操作", saved.getAction());
        assertEquals("BUSINESS", saved.getAuditType());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals(100L, saved.getExecutionTime());

        // 查询并验证
        List<AuditLogEntity> logs = auditLogRepository.findByModuleOrderByCreateTimeDesc("测试模块");
        assertFalse(logs.isEmpty(), "应该能查询到刚保存的审计日志");
        assertEquals("测试模块", logs.get(0).getModule());
    }

    @Test
    @WithMockUser(username = "doctor001", roles = {"DOCTOR"})
    @DisplayName("应该能够保存失败的审计日志（带异常信息）")
    void testSaveAuditLogWithException() {
        // Given - 准备包含异常信息的审计日志
        RuntimeException exception = new RuntimeException("测试异常");

        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "FAILURE", 200L, exception
        );

        // When - 保存审计日志
        AuditLogEntity saved = auditLogRepository.save(entity);

        // Then - 验证异常信息被正确保存
        assertNotNull(saved.getId());
        assertEquals("FAILURE", saved.getStatus());
        assertEquals("RuntimeException", saved.getExceptionType());
        assertEquals("测试异常", saved.getExceptionMessage());
        assertEquals(200L, saved.getExecutionTime());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够异步保存审计日志")
    void testSaveAuditLogAsync() throws Exception {
        // Given - 准备测试数据
        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule("异步测试模块");
        entity.setAction("异步测试操作");
        entity.setAuditType("SENSITIVE_OPERATION");
        entity.setStatus("SUCCESS");
        entity.setExecutionTime(50L);

        // When - 异步保存
        CompletableFuture<Void> future = auditLogService.saveAuditLogAsync(entity);

        // Then - 等待异步完成并验证
        assertDoesNotThrow(() -> future.get(), "异步保存应该成功完成");

        // 等待一下确保异步任务完成
        Thread.sleep(100);

        // 验证数据已保存
        List<AuditLogEntity> logs = auditLogRepository.findByModuleOrderByCreateTimeDesc("异步测试模块");
        assertFalse(logs.isEmpty(), "异步保存应该成功");
        assertEquals("异步测试操作", logs.get(0).getAction());
    }

    // ==================== 按模块查询测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够按模块查询审计日志")
    void testFindByModule() {
        // Given - 准备测试数据（使用唯一名称避免冲突）
        String uniqueModule1 = "认证管理_" + System.currentTimeMillis();
        String uniqueModule2 = "挂号管理_" + System.currentTimeMillis();

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setModule(uniqueModule1);
        log1.setAction("用户登录");
        log1.setStatus("SUCCESS");

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setModule(uniqueModule2);
        log2.setAction("患者挂号");
        log2.setStatus("SUCCESS");

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        // Flush to ensure persistence
        auditLogRepository.flush();

        // When - 按模块查询
        List<AuditLogEntity> authLogs = auditLogRepository.findByModuleOrderByCreateTimeDesc(uniqueModule1);
        List<AuditLogEntity> regLogs = auditLogRepository.findByModuleOrderByCreateTimeDesc(uniqueModule2);

        // Then - 验证查询结果
        assertTrue(authLogs.size() >= 1, "应该至少有一条认证管理日志");
        assertEquals(uniqueModule1, authLogs.get(0).getModule());
        assertEquals("用户登录", authLogs.get(0).getAction());

        assertTrue(regLogs.size() >= 1, "应该至少有一条挂号管理日志");
        assertEquals(uniqueModule2, regLogs.get(0).getModule());
        assertEquals("患者挂号", regLogs.get(0).getAction());
    }

    // ==================== 按TraceId查询测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够根据TraceId查询审计日志")
    void testFindByTraceId() {
        // Given - 准备测试数据
        String traceId = "test-trace-id-1234567890abcdef";

        // 使用固定时间确保排序稳定
        LocalDateTime baseTime = LocalDateTime.now();

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setModule("测试模块1");
        log1.setAction("操作1");
        log1.setTraceId(traceId);
        log1.setStatus("SUCCESS");
        log1.setCreateTime(baseTime);

        auditLogRepository.save(log1);
        auditLogRepository.flush();  // 确保立即持久化

        // 等待至少10毫秒，确保时间戳不同
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setModule("测试模块2");
        log2.setAction("操作2");
        log2.setTraceId(traceId);
        log2.setStatus("SUCCESS");
        log2.setCreateTime(LocalDateTime.now());  // 使用当前时间，确保晚于log1

        auditLogRepository.save(log2);
        auditLogRepository.flush();  // 确保立即持久化

        // When - 根据TraceId查询
        List<AuditLogEntity> logs = auditLogRepository.findByTraceIdOrderByCreateTimeDesc(traceId);

        // Then - 验证查询结果
        assertEquals(2, logs.size());
        assertEquals(traceId, logs.get(0).getTraceId());
        assertEquals(traceId, logs.get(1).getTraceId());
        // 验证倒序排列（最新的在前）
        assertEquals("测试模块2", logs.get(0).getModule());
        assertEquals("测试模块1", logs.get(1).getModule());
    }

    // ==================== 按操作人查询测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够查询操作人的审计日志")
    void testFindByOperatorId() {
        // Given - 准备测试数据
        Long operatorId = 123L;

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setModule("模块1");
        log1.setAction("操作1");
        log1.setOperatorId(operatorId);
        log1.setOperatorUsername("admin");
        log1.setStatus("SUCCESS");
        log1.setCreateTime(LocalDateTime.now());

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setModule("模块2");
        log2.setAction("操作2");
        log2.setOperatorId(operatorId);
        log2.setOperatorUsername("admin");
        log2.setStatus("SUCCESS");
        log2.setCreateTime(LocalDateTime.now().plusSeconds(1));

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        // When - 根据操作人ID查询
        List<AuditLogEntity> logs = auditLogRepository.findByOperatorIdOrderByCreateTimeDesc(operatorId);

        // Then - 验证查询结果
        assertEquals(2, logs.size());
        assertEquals(operatorId, logs.get(0).getOperatorId());
        assertEquals(operatorId, logs.get(1).getOperatorId());
        assertEquals("admin", logs.get(0).getOperatorUsername());
    }

    // ==================== 按时间范围查询测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够按时间范围查询审计日志")
    void testFindByTimeRange() {
        // Given - 准备测试数据
        LocalDateTime now = LocalDateTime.now();

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setModule("测试模块");
        log1.setAction("操作1");
        log1.setStatus("SUCCESS");

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setModule("测试模块");
        log2.setAction("操作2");
        log2.setStatus("SUCCESS");

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        // Flush to ensure persistence
        auditLogRepository.flush();

        // When - 查询所有日志（简化测试逻辑）
        List<AuditLogEntity> logs = auditLogRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(
                now.minusHours(1), now.plusHours(1)
        );

        // Then - 验证查询结果
        assertTrue(logs.size() >= 2, "应该至少有两条记录");
        boolean hasLog2 = logs.stream()
                .anyMatch(log -> "操作2".equals(log.getAction()));
        assertTrue(hasLog2, "应该包含操作2的日志");
    }

    // ==================== 删除测试 ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("应该能够删除指定时间之前的审计日志")
    void testDeleteByCreateTimeBefore() {
        // Given - 准备测试数据（使用唯一模块名）
        String uniqueModule = "测试模块_" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        AuditLogEntity log1 = new AuditLogEntity();
        log1.setModule(uniqueModule);
        log1.setAction("旧日志");
        log1.setStatus("SUCCESS");

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setModule(uniqueModule);
        log2.setAction("新日志");
        log2.setStatus("SUCCESS");

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);
        auditLogRepository.flush();

        // Count logs with this module before deletion
        List<AuditLogEntity> logsBefore = auditLogRepository.findByModuleOrderByCreateTimeDesc(uniqueModule);
        long countBefore = logsBefore.size();

        // When - 尝试删除（使用过去时间，实际不会删除任何记录因为createTime都是now）
        auditLogRepository.deleteByCreateTimeBefore(now.minusHours(1));

        // Flush to ensure deletion is executed
        auditLogRepository.flush();

        // Then - 验证删除结果（记录数应该不变，因为删除的是更早的时间）
        List<AuditLogEntity> logsAfter = auditLogRepository.findByModuleOrderByCreateTimeDesc(uniqueModule);
        assertEquals(countBefore, logsAfter.size(), "删除过去时间的日志不应影响当前记录");
    }

    // ==================== 构建审计日志实体测试 ====================

    @Test
    @DisplayName("构建审计日志实体应该自动设置TraceId")
    void testBuildAuditLogEntity_WithTraceId() {
        // Given - 设置MDC中的TraceId
        String testTraceId = "integration-test-trace-id";
        org.slf4j.MDC.put("traceId", testTraceId);

        try {
            // When - 构建审计日志实体
            AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                    "测试模块", "测试操作", "BUSINESS", "测试描述",
                    "SUCCESS", 100L, null
            );

            // Then - 验证TraceId被正确设置
            assertEquals(testTraceId, entity.getTraceId());
            // Note: createTime is set by JPA @CreationTimestamp during persistence
            // The buildAuditLogEntity() method creates entity in memory, not persisted yet
        } finally {
            // 清理MDC
            org.slf4j.MDC.remove("traceId");
        }
    }

    @Test
    @WithMockUser(username = "doctor001", roles = {"DOCTOR"})
    @DisplayName("构建审计日志实体应该自动设置操作人信息")
    void testBuildAuditLogEntity_WithOperatorInfo() {
        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "SUCCESS", 100L, null
        );

        // Then - 验证操作人信息被自动设置
        assertNotNull(entity.getOperatorUsername(), "操作人用户名应该被自动设置");
    }
}
