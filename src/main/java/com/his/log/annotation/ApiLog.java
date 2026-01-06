package com.his.log.annotation;

import java.lang.annotation.*;

/**
 * API 请求日志注解
 *
 * 使用场景：
 * 1. 标记需要记录详细日志的 Controller 方法
 * 2. 自定义日志描述，方便追踪业务操作
 * 3. 控制是否记录请求参数和响应结果
 *
 * 示例：
 * <pre>{@code
 * @PostMapping("/patients")
 * @ApiLog(value = "创建患者", recordRequest = true, recordResponse = true)
 * public Result<Patient> createPatient(@RequestBody Patient patient) {
 *     // ...
 * }
 * }</pre>
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    /**
     * 操作描述
     * 例如：创建患者、更新医生信息、删除处方等
     */
    String value() default "";

    /**
     * 操作类型
     * 用于分类统计
     */
    OperationType operationType() default OperationType.OTHER;

    /**
     * 是否记录请求参数
     * true: 记录请求参数到日志
     * false: 不记录（适用于敏感操作）
     */
    boolean recordRequest() default true;

    /**
     * 是否记录响应结果
     * true: 记录响应结果到日志
     * false: 不记录（适用于数据量大或敏感数据）
     */
    boolean recordResponse() default false;

    /**
     * 慢请求阈值（毫秒）
     * 超过此阈值会记录 WARN 级别日志
     * 默认 3000ms
     */
    long slowThreshold() default 3000;

    /**
     * 是否记录参数详细值
     * true: 记录参数的完整 JSON
     * false: 只记录参数类型和大小
     */
    boolean detailedParams() default false;
}
