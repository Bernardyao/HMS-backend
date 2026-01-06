package com.his.service;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.his.entity.AuditLogEntity;
import com.his.repository.AuditLogRepository;
import com.his.service.impl.AuditLogServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审计日志服务测试类
 *
 * <p>使用Mockito测试AuditLogService的业务逻辑</p>
 *
 * <h3>测试策略</h3>
 * <ul>
 *   <li><b>单元测试</b>：使用Mock隔离外部依赖</li>
 *   <li><b>验证行为</b>：验证方法调用和参数传递</li>
 *   <li><b>异常处理</b>：验证异常情况的处理</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审计日志服务测试")
@ActiveProfiles("test")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    /**
     * 初始化测试环境
     *
     * <p>使用反射注入mock的repository</p>
     */
    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
    }

    // ==================== saveAuditLogAsync 测试 ====================

    @Test
    @DisplayName("应该异步保存审计日志")
    void testSaveAuditLogAsync_Success() {
        // Given - 准备测试数据
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(1L);
        entity.setModule("测试模块");
        entity.setAction("测试操作");
        entity.setAuditType("BUSINESS");
        entity.setStatus("SUCCESS");

        when(auditLogRepository.save(any())).thenReturn(entity);

        // When - 执行测试
        CompletableFuture<Void> future = auditLogService.saveAuditLogAsync(entity);

        // Then - 验证结果
        assertNotNull(future, "Future不应该为null");
        assertDoesNotThrow(() -> future.get(), "Future应该正常完成");
        verify(auditLogRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("保存失败时不应该抛出异常")
    void testSaveAuditLogAsync_Failure() {
        // Given - 模拟保存失败
        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule("测试模块");
        entity.setAction("测试操作");

        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("数据库连接失败"));

        // When - 执行测试
        CompletableFuture<Void> future = auditLogService.saveAuditLogAsync(entity);

        // Then - 验证结果（不应该抛出异常）
        assertDoesNotThrow(() -> future.get(), "即使保存失败也不应该抛出异常");
        verify(auditLogRepository, times(1)).save(entity);
    }

    // ==================== buildAuditLogEntity 测试 ====================

    @Test
    @DisplayName("应该构建完整的审计日志实体（成功情况）")
    void testBuildAuditLogEntity_Success() {
        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "SUCCESS", 100L, null
        );

        // Then - 验证结果
        assertNotNull(entity, "实体不应该为null");
        assertEquals("测试模块", entity.getModule());
        assertEquals("测试操作", entity.getAction());
        assertEquals("BUSINESS", entity.getAuditType());
        assertEquals("测试描述", entity.getDescription());
        assertEquals("SUCCESS", entity.getStatus());
        assertEquals(100L, entity.getExecutionTime());
        assertNull(entity.getExceptionType());
        assertNull(entity.getExceptionMessage());
        // Note: createTime is set by JPA @CreatedDate during persistence
        // Tested in integration tests, not unit tests
    }

    @Test
    @DisplayName("应该正确记录异常信息")
    void testBuildAuditLogEntity_WithException() {
        // Given - 准备异常
        RuntimeException exception = new RuntimeException("测试异常");

        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "FAILURE", 100L, exception
        );

        // Then - 验证异常信息
        assertEquals("FAILURE", entity.getStatus());
        assertEquals("RuntimeException", entity.getExceptionType());
        assertEquals("测试异常", entity.getExceptionMessage());
    }

    @Test
    @DisplayName("应该截断过长的异常消息")
    void testBuildAuditLogEntity_LongExceptionMessage() {
        // Given - 准备超长异常消息（>1000字符）
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            longMessage.append("a");
        }
        RuntimeException exception = new RuntimeException(longMessage.toString());

        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "FAILURE", 100L, exception
        );

        // Then - 验证异常消息被截断
        assertEquals(1003, entity.getExceptionMessage().length()); // 1000 + "..."
        assertTrue(entity.getExceptionMessage().endsWith("..."));
    }

    @Test
    @DisplayName("异常消息为null时不应该报错")
    void testBuildAuditLogEntity_NullExceptionMessage() {
        // Given - 准备没有消息的异常
        RuntimeException exception = new RuntimeException((String) null);

        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "FAILURE", 100L, exception
        );

        // Then - 验证结果
        assertEquals("FAILURE", entity.getStatus());
        assertEquals("RuntimeException", entity.getExceptionType());
        assertNull(entity.getExceptionMessage());
    }

    @Test
    @DisplayName("应该正确设置操作人信息")
    void testBuildAuditLogEntity_OperatorInfo() {
        // When - 构建审计日志实体
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "SUCCESS", 100L, null
        );

        // Then - 验证操作人信息（测试环境下可能是匿名用户）
        // 注意：实际值取决于测试环境的安全上下文
        assertNotNull(entity.getOperatorUsername(), "操作人用户名应该被设置");
    }

    @Test
    @DisplayName("应该正确设置TraceId")
    void testBuildAuditLogEntity_TraceId() {
        // Given - 设置MDC中的TraceId
        String testTraceId = "test-trace-id-1234567890abcdef";
        org.slf4j.MDC.put("traceId", testTraceId);

        try {
            // When - 构建审计日志实体
            AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                    "测试模块", "测试操作", "BUSINESS", "测试描述",
                    "SUCCESS", 100L, null
            );

            // Then - 验证TraceId
            assertEquals(testTraceId, entity.getTraceId());
        } finally {
            // 清理MDC
            org.slf4j.MDC.remove("traceId");
        }
    }

    @Test
    @DisplayName("执行时间应该正确设置")
    void testBuildAuditLogEntity_ExecutionTime() {
        // When - 构建审计日志实体
        long executionTime = 12345L;
        AuditLogEntity entity = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "BUSINESS", "测试描述",
                "SUCCESS", executionTime, null
        );

        // Then - 验证执行时间
        assertEquals(executionTime, entity.getExecutionTime());
    }

    @Test
    @DisplayName("审计类型应该正确设置")
    void testBuildAuditLogEntity_AuditType() {
        // When - 使用不同的审计类型构建实体
        AuditLogEntity entity1 = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "SENSITIVE_OPERATION", "测试描述",
                "SUCCESS", 100L, null
        );

        AuditLogEntity entity2 = auditLogService.buildAuditLogEntity(
                "测试模块", "测试操作", "DATA_ACCESS", "测试描述",
                "SUCCESS", 100L, null
        );

        // Then - 验证审计类型
        assertEquals("SENSITIVE_OPERATION", entity1.getAuditType());
        assertEquals("DATA_ACCESS", entity2.getAuditType());
    }
}
