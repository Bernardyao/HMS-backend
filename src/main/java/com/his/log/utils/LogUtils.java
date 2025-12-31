package com.his.log.utils;

import com.his.common.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 日志工具类
 *
 * 提供统一的日志记录方法，简化业务代码中的日志记录
 *
 * 功能：
 * 1. 统一的日志格式
 * 2. 自动包含 TraceId、用户信息
 * 3. 敏感信息自动脱敏
 * 4. 简化常用日志记录
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@Slf4j
public class LogUtils {

    private LogUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 记录业务操作日志
     *
     * @param module 模块名称（如：患者管理、医生工作站）
     * @param operation 操作描述（如：创建患者、更新处方）
     * @param details 详细信息
     */
    public static void logBusinessOperation(String module, String operation, String details) {
        String userInfo = getCurrentUserInfo();
        log.info("[业务操作] 模块: {} | 操作: {} | 操作人: {} | 详情: {}",
                module, operation, userInfo, details);
    }

    /**
     * 记录数据访问日志
     *
     * @param resource 资源类型（如：患者、医生、处方）
     * @param resourceId 资源ID
     * @param action 操作类型（如：查询、导出）
     */
    public static void logDataAccess(String resource, Long resourceId, String action) {
        String userInfo = getCurrentUserInfo();
        log.info("[数据访问] 资源: {} | ID: {} | 操作: {} | 操作人: {}",
                resource, resourceId, action, userInfo);
    }

    /**
     * 记录敏感操作日志
     *
     * 用于记录查看敏感数据、导出敏感信息等操作
     *
     * @param operation 操作描述
     * @param targetType 目标类型（如：患者、医生）
     * @param targetId 目标ID
     */
    public static void logSensitiveOperation(String operation, String targetType, Long targetId) {
        String userInfo = getCurrentUserInfo();
        String traceId = getTraceId();
        log.warn("[敏感操作] ⚠️ 操作: {} | 目标: {}({}) | 操作人: {} | TraceId: {}",
                operation, targetType, targetId, userInfo, traceId);
    }

    /**
     * 记录系统错误
     *
     * @param module 模块名称
     * @param error 错误信息
     * @param e 异常对象
     */
    public static void logSystemError(String module, String error, Throwable e) {
        String userInfo = getCurrentUserInfo();
        String traceId = getTraceId();
        log.error("[系统错误] 模块: {} | 错误: {} | 操作人: {} | TraceId: {}",
                module, error, userInfo, traceId, e);
    }

    /**
     * 记录性能日志
     *
     * @param operation 操作描述
     * @param executionTime 执行时间（毫秒）
     * @param threshold 慢操作阈值（毫秒）
     */
    public static void logPerformance(String operation, long executionTime, long threshold) {
        if (executionTime > threshold) {
            log.warn("[性能警告] 操作: {} | 执行时间: {}ms | 阈值: {}ms | ⚠️ 慢操作",
                    operation, executionTime, threshold);
        } else {
            log.debug("[性能日志] 操作: {} | 执行时间: {}ms", operation, executionTime);
        }
    }

    /**
     * 记录参数校验失败
     *
     * @param paramName 参数名
     * @param paramValue 参数值
     * @param reason 失败原因
     */
    public static void logValidationError(String paramName, Object paramValue, String reason) {
        log.warn("[参数校验] 参数: {} | 值: {} | 原因: {}",
                paramName, paramValue, reason);
    }

    /**
     * 记录外部服务调用
     *
     * @param serviceName 服务名称
     * @param method 调用方法
     * @param executionTime 执行时间
     * @param success 是否成功
     */
    public static void logExternalServiceCall(String serviceName, String method,
                                              long executionTime, boolean success) {
        String status = success ? "✅ 成功" : "❌ 失败";
        log.info("[外部调用] 服务: {} | 方法: {} | 耗时: {}ms | 状态: {}",
                serviceName, method, executionTime, status);
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息字符串
     */
    private static String getCurrentUserInfo() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String username = SecurityUtils.getCurrentUsername();
            return String.format("用户(%d, %s)", userId, username);
        } catch (Exception e) {
            return "匿名用户";
        }
    }

    /**
     * 获取当前 TraceId
     *
     * @return TraceId
     */
    private static String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "UNKNOWN";
    }

    /**
     * 脱敏工具
     *
     * 注意：这里只是简单示例，实际应该使用已有的 DataMaskingUtils
     *
     * @param data 原始数据
     * @param isSensitive 是否敏感
     * @return 脱敏后的数据
     */
    public static String maskIfSensitive(String data, boolean isSensitive) {
        if (!isSensitive || data == null) {
            return data;
        }

        // 简单脱敏：只显示前2位和后2位
        if (data.length() <= 4) {
            return "****";
        }

        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}
