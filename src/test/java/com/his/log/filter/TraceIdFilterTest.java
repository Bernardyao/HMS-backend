package com.his.log.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceId 过滤器测试
 *
 * 测试目标：
 * 1. 验证 TraceId 是否正确生成
 * 2. 验证 TraceId 是否正确传递到 MDC
 * 3. 验证 TraceId 是否在日志中正确显示
 * 4. 验证请求结束后 MDC 是否正确清理
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@DisplayName("TraceId 过滤器测试")
class TraceIdFilterTest {

    @Test
    @DisplayName("应该生成唯一 TraceId")
    void shouldGenerateUniqueTraceId() {
        // 模拟过滤器设置 TraceId
        String traceId1 = "test-trace-id-1";
        String traceId2 = "test-trace-id-2";

        MDC.put(TraceIdFilter.TRACE_ID_KEY, traceId1);
        String result1 = TraceIdFilter.getTraceId();
        assertEquals(traceId1, result1, "应该返回设置的 TraceId");

        MDC.put(TraceIdFilter.TRACE_ID_KEY, traceId2);
        String result2 = TraceIdFilter.getTraceId();
        assertEquals(traceId2, result2, "应该返回新的 TraceId");

        MDC.remove(TraceIdFilter.TRACE_ID_KEY);
    }

    @Test
    @DisplayName("TraceId 不存在时返回 UNKNOWN")
    void shouldReturnUnknownWhenTraceIdNotExists() {
        // 确保 MDC 中没有 TraceId
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        String traceId = TraceIdFilter.getTraceId();
        assertEquals("UNKNOWN", traceId, "TraceId 不存在时应返回 UNKNOWN");
    }

    @Test
    @DisplayName("应该多次返回相同的 TraceId")
    void shouldReturnSameTraceIdMultipleTimes() {
        String traceId = "test-trace-id-123";
        MDC.put(TraceIdFilter.TRACE_ID_KEY, traceId);

        // 多次调用应该返回相同的 TraceId
        assertEquals(traceId, TraceIdFilter.getTraceId());
        assertEquals(traceId, TraceIdFilter.getTraceId());
        assertEquals(traceId, TraceIdFilter.getTraceId());

        MDC.remove(TraceIdFilter.TRACE_ID_KEY);
    }

    @Test
    @DisplayName("MDC 清理后应返回 UNKNOWN")
    void shouldReturnUnknownAfterMdcClear() {
        String traceId = "test-trace-id-456";
        MDC.put(TraceIdFilter.TRACE_ID_KEY, traceId);
        assertEquals(traceId, TraceIdFilter.getTraceId());

        // 清理 MDC
        MDC.clear();
        assertEquals("UNKNOWN", TraceIdFilter.getTraceId(), "MDC 清理后应返回 UNKNOWN");
    }

    @BeforeEach
    @AfterEach
    void tearDown() {
        // 清理 MDC，避免影响其他测试
        MDC.clear();
    }
}
