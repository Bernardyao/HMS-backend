package com.his.log.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器
 *
 * 功能：
 * 1. 为每个请求生成唯一的 TraceId
 * 2. 将 TraceId 放入 MDC（Mapped Diagnostic Context）
 * 3. 在日志中自动包含 TraceId，方便追踪分布式调用链
 * 4. 请求结束后清理 MDC，避免内存泄漏
 *
 * 日志格式示例：
 * 2025-12-31 10:30:45.123 [http-nio-8080-exec-1] [a1b2c3d4-e5f6-7890-abcd-ef1234567890] INFO  com.his.controller.AuthController - 接收登录请求
 *                                                                 ↑ 这是 TraceId
 *
 * 使用方式：
 * - 自动生效，无需手动调用
 * - 在 logback-spring.xml 中通过 %X{traceId} 引用
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 最高优先级，确保最早执行
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * TraceId 在 MDC 中的键名
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * HTTP 请求头中传递 TraceId 的键名
     * 用于微服务场景下的链路追踪
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取或生成 TraceId
        String traceId = generateOrGetTraceId(request);

        // 2. 将 TraceId 放入 MDC
        MDC.put(TRACE_ID_KEY, traceId);

        // 3. 将 TraceId 添加到响应头（方便前端追踪）
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            // 4. 继续执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 5. 请求结束后清理 MDC（非常重要！否则会导致内存泄漏）
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 生成或获取 TraceId
     *
     * 策略：
     * 1. 如果请求头中已有 TraceId（上游服务传递），则使用
     * 2. 否则生成新的 UUID
     *
     * @param request HTTP 请求
     * @return TraceId
     */
    private String generateOrGetTraceId(HttpServletRequest request) {
        // 尝试从请求头获取（微服务场景）
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId != null && !traceId.isEmpty()) {
            // 验证 TraceId 格式（防止注入攻击）
            if (isValidTraceId(traceId)) {
                log.debug("使用上游传递的 TraceId: {}", traceId);
                return traceId;
            } else {
                log.warn("上游 TraceId 格式无效，将重新生成: {}", traceId);
            }
        }

        // 生成新的 TraceId
        String newTraceId = UUID.randomUUID().toString();
        log.debug("生成新的 TraceId: {}", newTraceId);
        return newTraceId;
    }

    /**
     * 验证 TraceId 格式
     *
     * 防止恶意输入导致日志格式混乱
     *
     * @param traceId TraceId
     * @return 是否有效
     */
    private boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return false;
        }

        // 检查长度（UUID 格式：36 字符）
        if (traceId.length() > 100) {
            return false;
        }

        // 检查是否包含换行符（防止日志注入攻击）
        if (traceId.contains("\n") || traceId.contains("\r")) {
            return false;
        }

        return true;
    }

    /**
     * 获取当前线程的 TraceId
     *
     * 使用场景：
     * - 在业务代码中手动获取 TraceId
     * - 在异常处理中将 TraceId 返回给前端
     *
     * @return TraceId，如果不存在则返回 "UNKNOWN"
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId != null ? traceId : "UNKNOWN";
    }
}
