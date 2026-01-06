package com.his.log.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 *
 * 用于标记需要记录审计日志的业务方法
 * 审计日志用于满足合规要求（如 HIPAA、等保三级等）
 *
 * 与 @ApiLog 的区别：
 * - @ApiLog: 技术视角，记录 API 调用、性能指标
 * - @AuditLog: 业务视角，记录谁、在什么时间、做了什么、结果如何
 *
 * 示例：
 * <pre>{@code
 * @PostMapping("/prescriptions")
 * @AuditLog(
 *     module = "处方管理",
 *     action = "开具处方",
 *     description = "医生为患者开具处方"
 * )
 * public Result<Prescription> createPrescription(...) {
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
public @interface AuditLog {

    /**
     * 业务模块
     * 例如：患者管理、医生工作站、药房管理
     */
    String module();

    /**
     * 操作动作
     * 例如：创建患者、开具处方、收费
     */
    String action();

    /**
     * 操作描述
     * 详细描述操作内容
     */
    String description() default "";

    /**
     * 业务类型
     * 用于审计日志分类
     */
    AuditType auditType() default AuditType.BUSINESS;

    /**
     * 是否记录成功结果
     */
    boolean recordSuccess() default true;

    /**
     * 是否记录失败结果
     */
    boolean recordFailure() default true;
}
