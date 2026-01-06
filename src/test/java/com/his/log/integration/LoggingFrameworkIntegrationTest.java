package com.his.log.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.his.common.Result;
import com.his.controller.AuthController;
import com.his.dto.LoginRequest;
import com.his.service.AuthService;
import com.his.test.base.BaseIntegrationTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 日志框架集成测试
 *
 * 测试目标：
 * 1. 验证 TraceId 是否在整个请求链中传递
 * 2. 验证日志框架与 Spring Context 集成正常
 * 3. 验证 MDC 功能正常
 *
 * 这是一个真正的集成测试，启动完整的 Spring Context
 *
 * <p>继承自BaseIntegrationTest，自动获得事务管理和数据清理</p>
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@DisplayName("日志框架集成测试")
class LoggingFrameworkIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private AuthController authController;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("应该成功启动 Spring Context")
    void shouldLoadSpringContext() {
        // 验证 Spring Context 能够成功加载
        assertNotNull(authController, "AuthController 应该被注入");
    }

    @Test
    @DisplayName("TraceId 应该在整个请求链中传递")
    void shouldPropagateTraceId() {
        // 1. 设置 TraceId（模拟 TraceIdFilter 的行为）
        String testTraceId = "test-trace-id-12345";
        MDC.put("traceId", testTraceId);

        try {
            // 2. 验证 TraceId 可以获取
            String actualTraceId = MDC.get("traceId");
            assertEquals(testTraceId, actualTraceId, "TraceId 应该正确设置和获取");

            // 3. 模拟登录请求
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("test");
            loginRequest.setPassword("test");

            // 4. Mock authService 返回值（模拟登录失败）
            when(authService.login(any())).thenThrow(new RuntimeException("用户名或密码错误"));

            // 5. 调用 Controller（Controller 会捕获异常并返回 Result）
            Result<?> result = authController.login(loginRequest);

            // 6. 验证 Controller 能够正常处理请求
            assertNotNull(result, "Result 不应该为 null");
            assertEquals(401, result.getCode(), "应该返回 401 状态码");

            // 7. 验证 authService 被调用
            verify(authService, times(1)).login(any());

        } finally {
            // 8. 清理 MDC
            MDC.clear();
        }
    }

    @Test
    @DisplayName("MDC 清理后 TraceId 应该为 null")
    void shouldReturnNullAfterMdcClear() {
        // 1. 设置 TraceId
        MDC.put("traceId", "test-id");
        assertNotNull(MDC.get("traceId"));

        // 2. 清理 MDC
        MDC.clear();

        // 3. 验证 TraceId 已清理
        assertNull(MDC.get("traceId"), "MDC 清理后 TraceId 应该为 null");
    }

    @Test
    @DisplayName("应该能够多次设置和获取 TraceId")
    void shouldSetAndGetTraceIdMultipleTimes() {
        try {
            // 第一次设置
            MDC.put("traceId", "trace-1");
            assertEquals("trace-1", MDC.get("traceId"));

            // 第二次设置（覆盖）
            MDC.put("traceId", "trace-2");
            assertEquals("trace-2", MDC.get("traceId"));

            // 第三次设置
            MDC.put("traceId", "trace-3");
            assertEquals("trace-3", MDC.get("traceId"));

        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("TraceId 应该在不同线程中隔离")
    void shouldIsolateTraceIdBetweenThreads() throws InterruptedException {
        String mainThreadTraceId = "main-thread-trace";
        MDC.put("traceId", mainThreadTraceId);

        // 用于存储子线程的 TraceId
        final String[] childThreadTraceId = new String[1];

        Thread childThread = new Thread(() -> {
            // 子线程应该看不到主线程的 TraceId
            childThreadTraceId[0] = MDC.get("traceId");

            // 子线程设置自己的 TraceId
            MDC.put("traceId", "child-thread-trace");
        });

        childThread.start();
        childThread.join();

        // 验证子线程看不到主线程的 TraceId
        assertNull(childThreadTraceId[0], "子线程不应该看到主线程的 TraceId");

        // 验证主线程的 TraceId 未受影响
        assertEquals(mainThreadTraceId, MDC.get("traceId"), "主线程的 TraceId 应该保持不变");

        // 清理
        MDC.clear();
    }

    @Test
    @DisplayName("Controller 应该正常工作并记录日志")
    void shouldControllerWorkWithLogging() {
        // 1. 设置 TraceId
        MDC.put("traceId", "test-trace-controller");

        try {
            // 2. 模拟登录请求
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("doctor001");
            loginRequest.setPassword("wrong-password");

            // 3. Mock authService 抛出异常（模拟密码错误）
            when(authService.login(any())).thenThrow(new RuntimeException("用户名或密码错误"));

            // 4. 调用 Controller
            Result<?> result = authController.login(loginRequest);

            // 5. 验证结果
            assertNotNull(result);
            assertEquals(401, result.getCode());
            assertTrue(result.getMessage().contains("用户名或密码错误"));

            // 6. 验证 TraceId 仍然存在
            assertEquals("test-trace-controller", MDC.get("traceId"));

        } finally {
            MDC.clear();
        }
    }
}
