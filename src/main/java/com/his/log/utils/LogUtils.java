package com.his.log.utils;

import org.slf4j.MDC;

import com.his.common.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志工具类
 *
 * <p>提供统一的日志记录方法，简化业务代码中的日志记录，确保日志格式的一致性</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>统一日志格式</b>：所有日志使用相同的格式，便于解析和分析</li>
 *   <li><b>自动上下文信息</b>：自动包含TraceId、用户信息等上下文数据</li>
 *   <li><b>敏感信息脱敏</b>：支持敏感数据自动脱敏，防止泄露</li>
 *   <li><b>分类日志方法</b>：提供业务操作、数据访问、敏感操作等专用的日志方法</li>
 *   <li><b>性能监控</b>：支持记录操作耗时，识别慢查询</li>
 * </ul>
 *
 * <h3>日志分类</h3>
 * <table border="1">
 *   <tr><th>日志类型</th><th>方法</th><th>日志级别</th><th>使用场景</th></tr>
 *   <tr><td>业务操作日志</td><td>logBusinessOperation</td><td>INFO</td><td>记录关键业务操作（挂号、收费、开处方）</td></tr>
 *   <tr><td>数据访问日志</td><td>logDataAccess</td><td>INFO</td><td>记录数据查询和导出操作</td></tr>
 *   <tr><td>敏感操作日志</td><td>logSensitiveOperation</td><td>WARN</td><td>记录查看敏感信息、导出等操作</td></tr>
 *   <tr><td>系统错误日志</td><td>logSystemError</td><td>ERROR</td><td>记录系统异常和错误</td></tr>
 *   <tr><td>性能日志</td><td>logPerformance</td><td>DEBUG/WARN</td><td>记录操作耗时，慢操作记录为WARN</td></tr>
 *   <tr><td>参数校验日志</td><td>logValidationError</td><td>WARN</td><td>记录参数校验失败</td></tr>
 *   <tr><td>外部调用日志</td><td>logExternalServiceCall</td><td>INFO</td><td>记录外部服务调用</td></tr>
 * </table>
 *
 * <h3>使用示例</h3>
 * <h4>1. 记录业务操作</h4>
 * <pre>
 * {@code @PostMapping("/registrations")}
 * public Result createRegistration({@code @RequestBody} RegistrationDTO dto) {
 *     Registration registration = registrationService.register(dto);
 *     LogUtils.logBusinessOperation("患者管理", "创建挂号", registration.toString());
 *     return Result.success(registration);
 * }
 * </pre>
 *
 * <h4>2. 记录敏感操作</h4>
 * <pre>
 * {@code @GetMapping("/patients/{id}/idcard")}
 * public Result getIdCard({@code @PathVariable} Long id) {
 *     Patient patient = patientService.getById(id);
 *     LogUtils.logSensitiveOperation("查看身份证", "患者", id);
 *     return Result.success(patient.getIdCard());
 * }
 * </pre>
 *
 * <h4>3. 记录性能</h4>
 * <pre>
 * long startTime = System.currentTimeMillis();
 * List{@code <Patient>} patients = patientService.findByKeyword(keyword);
 * long executionTime = System.currentTimeMillis() - startTime;
 * LogUtils.logPerformance("患者搜索", executionTime, 1000);  // 超过1秒会记录WARN
 * </pre>
 *
 * <h3>日志格式示例</h3>
 * <pre>
 * [业务操作] 模块: 患者管理 | 操作: 创建挂号 | 操作人: 用户(1, admin) | 详情: {...}
 * [敏感操作] ⚠️ 操作: 查看身份证 | 目标: 患者(100) | 操作人: 用户(1, admin) | TraceId: a1b2c3d4...
 * [性能警告] 操作: 患者搜索 | 执行时间: 1500ms | 阈值: 1000ms | ⚠️ 慢操作
 * </pre>
 *
 * <h3>与MDC集成</h3>
 * <p>本工具类自动从MDC获取TraceId（由{@link com.his.config.LogTraceFilter}设置），
 * 并从{@link com.his.common.SecurityUtils}获取当前用户信息</p>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>关键业务操作必须记录日志（如：挂号、收费、处方、退费）</li>
 *   <li>敏感数据查看必须记录日志（如：身份证、手机号、病历详情）</li>
 *   <li>性能敏感操作必须记录耗时（如：复杂查询、批量操作）</li>
 *   <li>日志消息应简洁明确，便于检索和审计</li>
 *   <li>避免在循环中频繁记录日志，减少I/O开销</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see org.slf4j.MDC
 * @see com.his.common.SecurityUtils
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
