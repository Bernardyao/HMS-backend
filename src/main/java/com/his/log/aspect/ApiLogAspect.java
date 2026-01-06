package com.his.log.aspect;

import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.log.annotation.ApiLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API 请求日志切面
 *
 * 功能：
 * 1. 拦截所有标记了 @ApiLog 的方法
 * 2. 记录请求信息：URL、方法、参数、执行时间
 * 3. 记录响应信息：状态、结果（可选）
 * 4. 检测慢请求，性能监控
 * 5. 记录异常信息
 *
 * 日志示例：
 * <pre>
 * [API] GET /api/patients/123 | 参数: {} | 执行时间: 45ms | 状态: SUCCESS
 * [API] POST /api/prescriptions | 参数: {...(200 bytes)} | 执行时间: 3250ms | 状态: SUCCESS | ⚠️ 慢请求
 * [API] DELETE /api/doctors/456 | 参数: {} | 执行时间: 120ms | 状态: FAILED | 异常: IllegalArgumentException
 * </pre>
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 拦截所有标记了 @ApiLog 的方法
     */
    @Pointcut("@annotation(com.his.log.annotation.ApiLog)")
    public void apiLogPointcut() {
        // Pointcut 定义
    }

    /**
     * 环绕通知：记录 API 调用日志
     */
    @Around("apiLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ApiLog apiLog = signature.getMethod().getAnnotation(ApiLog.class);

        // 2. 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 3. 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 4. 获取请求信息
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";

        // 5. 记录请求参数
        String params = formatParams(joinPoint.getArgs(), apiLog.detailedParams());

        // 6. 记录请求开始日志
        log.info("[API-START] {} {} | 操作: {} | 参数: {}",
                method, uri, apiLog.value(), params);

        Object result = null;
        Throwable exception = null;

        try {
            // 7. 执行目标方法
            result = joinPoint.proceed();

            // 8. 记录响应结果（如果需要）
            if (apiLog.recordResponse()) {
                String response = formatResult(result);
                log.info("[API-RESPONSE] 响应: {}", response);
            }

            return result;

        } catch (Throwable e) {
            // 9. 捕获异常
            exception = e;
            throw e;

        } finally {
            // 10. 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;

            // 11. 记录请求结束日志
            logApiEnd(apiLog, method, uri, params, executionTime, exception);

            // 12. 慢请求警告
            if (executionTime > apiLog.slowThreshold()) {
                log.warn("[API-SLOW] {} {} | 操作: {} | 执行时间: {}ms | 阈值: {}ms | ⚠️ 慢请求",
                        method, uri, apiLog.value(), executionTime, apiLog.slowThreshold());
            }
        }
    }

    /**
     * 记录 API 请求结束日志
     */
    private void logApiEnd(ApiLog apiLog, String method, String uri,
                          String params, long executionTime, Throwable exception) {
        if (exception == null) {
            log.info("[API-END] {} {} | 操作: {} | 执行时间: {}ms | 状态: ✅ SUCCESS",
                    method, uri, apiLog.value(), executionTime);
        } else {
            log.error("[API-END] {} {} | 操作: {} | 执行时间: {}ms | 状态: ❌ FAILED | 异常: {}",
                    method, uri, apiLog.value(), executionTime, exception.getClass().getSimpleName());
        }
    }

    /**
     * 格式化请求参数
     *
     * @param args 参数数组
     * @param detailed 是否记录详细参数
     * @return 格式化后的参数字符串
     */
    private String formatParams(Object[] args, boolean detailed) {
        if (args == null || args.length == 0) {
            return "{}";
        }

        if (!detailed) {
            // 只记录参数类型和数量
            return String.format("[参数数量: %d | 类型: %s]",
                    args.length,
                    Stream.of(args)
                            .map(arg -> arg != null ? arg.getClass().getSimpleName() : "null")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(""));
        }

        // 记录详细参数（注意：不要记录敏感信息）
        try {
            String json = objectMapper.writeValueAsString(args);
            // 限制长度，避免日志过大
            if (json.length() > 500) {
                return json.substring(0, 500) + "...(总长度: " + json.length() + " 字符)";
            }
            return json;
        } catch (Exception e) {
            log.warn("参数序列化失败: {}", e.getMessage());
            return "[参数序列化失败]";
        }
    }

    /**
     * 格式化响应结果
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            String json = objectMapper.writeValueAsString(result);
            // 限制长度
            if (json.length() > 500) {
                return json.substring(0, 500) + "...(总长度: " + json.length() + " 字符)";
            }
            return json;
        } catch (Exception e) {
            log.warn("结果序列化失败: {}", e.getMessage());
            return "[结果序列化失败]";
        }
    }
}
