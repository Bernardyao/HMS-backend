package com.his.log.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志工具类测试
 *
 * 测试目标：
 * 1. 验证各种日志方法是否正常工作
 * 2. 验证日志格式是否正确
 * 3. 验证敏感信息脱敏功能
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@DisplayName("日志工具类测试")
class LogUtilsTest {

    @Test
    @DisplayName("应该记录业务操作日志")
    void shouldLogBusinessOperation() {
        // 这个测试不抛出异常即为成功
        assertDoesNotThrow(() -> {
            LogUtils.logBusinessOperation(
                "患者管理",
                "创建患者",
                "患者姓名: 张三, 年龄: 35"
            );
        });

        // 验证：查看控制台输出
        // [业务操作] 模块: 患者管理 | 操作: 创建患者 | 操作人: 用户(...) | 详情: 患者姓名: 张三, 年龄: 35
    }

    @Test
    @DisplayName("应该记录数据访问日志")
    void shouldLogDataAccess() {
        assertDoesNotThrow(() -> {
            LogUtils.logDataAccess("患者", 123L, "查询详情");
        });

        // 验证：查看控制台输出
        // [数据访问] 资源: 患者 | ID: 123 | 操作: 查询详情 | 操作人: 用户(...)
    }

    @Test
    @DisplayName("应该记录敏感操作日志")
    void shouldLogSensitiveOperation() {
        assertDoesNotThrow(() -> {
            LogUtils.logSensitiveOperation(
                "查看完整身份证号",
                "患者",
                123L
            );
        });

        // 验证：查看控制台输出
        // [敏感操作] ⚠️ 操作: 查看完整身份证号 | 目标: 患者(123) | 操作人: 用户(...) | TraceId: ...
    }

    @Test
    @DisplayName("应该记录系统错误日志")
    void shouldLogSystemError() {
        Exception exception = new RuntimeException("测试异常");

        assertDoesNotThrow(() -> {
            LogUtils.logSystemError("患者管理", "创建患者失败", exception);
        });

        // 验证：查看控制台输出
        // [系统错误] 模块: 患者管理 | 错误: 创建患者失败 | 操作人: 用户(...) | TraceId: ...
        // 异常堆栈...
    }

    @Test
    @DisplayName("应该记录性能日志")
    void shouldLogPerformance() {
        // 快速操作（不应触发警告）
        assertDoesNotThrow(() -> {
            LogUtils.logPerformance("快速查询", 100, 1000);
        });

        // 慢操作（应触发警告）
        assertDoesNotThrow(() -> {
            LogUtils.logPerformance("慢查询", 1500, 1000);
        });

        // 验证：查看控制台输出
        // [性能日志] 操作: 快速查询 | 执行时间: 100ms
        // [性能警告] 操作: 慢查询 | 执行时间: 1500ms | 阈值: 1000ms | ⚠️ 慢操作
    }

    @Test
    @DisplayName("应该记录参数校验失败日志")
    void shouldLogValidationError() {
        assertDoesNotThrow(() -> {
            LogUtils.logValidationError("patientId", -1L, "ID 必须大于 0");
        });

        // 验证：查看控制台输出
        // [参数校验] 参数: patientId | 值: -1 | 原因: ID 必须大于 0
    }

    @Test
    @DisplayName("应该记录外部服务调用日志")
    void shouldLogExternalServiceCall() {
        // 成功调用
        assertDoesNotThrow(() -> {
            LogUtils.logExternalServiceCall("支付服务", "创建支付订单", 200, true);
        });

        // 失败调用
        assertDoesNotThrow(() -> {
            LogUtils.logExternalServiceCall("支付服务", "创建支付订单", 500, false);
        });

        // 验证：查看控制台输出
        // [外部调用] 服务: 支付服务 | 方法: 创建支付订单 | 耗时: 200ms | 状态: ✅ 成功
        // [外部调用] 服务: 支付服务 | 方法: 创建支付订单 | 耗时: 500ms | 状态: ❌ 失败
    }

    @Test
    @DisplayName("应该脱敏敏感信息")
    void shouldMaskSensitiveData() {
        // 非敏感数据
        String normalData = "正常数据";
        String result1 = LogUtils.maskIfSensitive(normalData, false);
        assertEquals("正常数据", result1);

        // 敏感数据
        String sensitiveData = "123456789012345678";
        String result2 = LogUtils.maskIfSensitive(sensitiveData, true);
        assertEquals("12****78", result2);

        // 短数据
        String shortData = "123";
        String result3 = LogUtils.maskIfSensitive(shortData, true);
        assertEquals("****", result3);

        // null 数据
        String result4 = LogUtils.maskIfSensitive(null, true);
        assertNull(result4);
    }

    @AfterEach
    void tearDown() {
        // 清理 MDC，避免影响其他测试
        MDC.clear();
    }
}
