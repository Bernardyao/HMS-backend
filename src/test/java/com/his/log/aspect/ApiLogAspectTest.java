package com.his.log.aspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * API 日志切面测试
 *
 * 测试目标：
 * 1. 验证 @ApiLog 注解是否正确拦截方法
 * 2. 验证日志是否正确记录（通过控制台输出验证）
 * 3. 验证慢请求检测是否生效
 *
 * 注意：这是一个手动测试类，需要查看控制台输出验证
 * 建议配合集成测试使用
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@SpringJUnitConfig
@DisplayName("API 日志切面测试")
class ApiLogAspectTest {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspectTest.class);

    /**
     * 测试 @ApiLog 注解的基本功能
     *
     * 预期：
     * 1. 控制台输出 "[API-START]" 日志
     * 2. 控制台输出 "[API-END]" 日志
     * 3. 日志包含 TraceId
     *
     * 注意：这是一个手动测试，运行后查看控制台输出
     */
    @Test
    @DisplayName("应该记录 API 请求日志（手动验证）")
    void shouldLogApiRequest() {
        log.info("=== 测试说明 ===");
        log.info("请在实际 Controller 方法上使用 @ApiLog 注解");
        log.info("然后调用该 API，观察控制台输出");
        log.info("预期输出：");
        log.info("  [API-START] GET /api/xxx | 操作: xxx | 参数: {}");
        log.info("  [API-END] GET /api/xxx | 操作: xxx | 执行时间: XXms | 状态: ✅ SUCCESS");
    }

    /**
     * 测试慢请求检测
     *
     * 预期：
     * 1. 如果执行时间超过 slowThreshold，应输出 WARN 日志
     * 2. 日志包含 "⚠️ 慢请求" 标记
     *
     * 注意：这是一个手动测试
     */
    @Test
    @DisplayName("应该检测慢请求（手动验证）")
    void shouldDetectSlowRequest() {
        log.info("=== 慢请求测试说明 ===");
        log.info("请使用 @ApiLog(slowThreshold = 100) 标记慢方法");
        log.info("预期输出：");
        log.info("  [API-SLOW] GET /api/slow-endpoint | 操作: 慢操作 | 执行时间: 500ms | 阈值: 100ms | ⚠️ 慢请求");
    }

    /**
     * 测试异常情况下的日志记录
     *
     * 预期：
     * 1. 即使方法抛出异常，也应记录 [API-END] 日志
     * 2. 状态应显示 "❌ FAILED"
     *
     * 注意：这是一个手动测试
     */
    @Test
    @DisplayName("应该记录异常情况（手动验证）")
    void shouldLogException() {
        log.info("=== 异常日志测试说明 ===");
        log.info("当标记了 @ApiLog 的方法抛出异常时");
        log.info("预期输出：");
        log.info("  [API-END] GET /api/xxx | 操作: xxx | 执行时间: XXms | 状态: ❌ FAILED | 异常: XxxException");
    }
}
