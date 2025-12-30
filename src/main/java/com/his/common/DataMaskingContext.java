package com.his.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据脱敏上下文控制
 *
 * <p>通过 ThreadLocal 控制是否启用脱敏功能
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>默认情况下，所有敏感数据都会自动脱敏</li>
 *   <li>对于有特殊权限的用户（如管理员、审计人员），可以通过禁用脱敏来查看明文</li>
 *   <li>使用 try-with-resources 或 try-finally 确保上下文正确清理</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 示例1：在 Controller 中为管理员禁用脱敏
 * {@code @GetMapping("/api/admin/patients/{id}")}
 * {@code @PreAuthorize("hasRole('ADMIN')"}
 * public Result{@code <PatientVO>} getPatientDetail(@PathVariable Long id) {
 *     DataMaskingContext.disableMasking();
 *     try {
 *         return Result.success(patientService.getById(id));
 *     } finally {
 *         DataMaskingContext.enableMasking();
 *     }
 * }
 *
 * // 示例2：使用 try-with-resources（推荐）
 * {@code @GetMapping("/api/audit/patients/{id}")}
 * public Result{@code <PatientVO>} auditPatient(@PathVariable Long id) {
 *     try (DataMaskingContext.Scope scope = DataMaskingContext.disable()) {
 *         return Result.success(patientService.getById(id));
 *     }
 *     // 自动恢复脱敏状态
 * }
 * </pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>必须在 finally 块中恢复脱敏状态，否则会影响后续请求</li>
 *   <li>在异步任务中使用时需要注意线程切换问题</li>
 *   <li>仅用于有明确权限要求的场景，不应滥用</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @see SensitiveData
 * @see SensitiveDataSerializer
 */
@Slf4j
public class DataMaskingContext {

    /**
     * 私有构造函数，防止实例化
     */
    private DataMaskingContext() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * ThreadLocal 变量，用于存储当前线程的脱敏状态
     * <p>使用 Integer 作为引用计数，支持嵌套 Scope
     * <p>> 0 = 禁用脱敏（显示明文），0 或 null = 启用脱敏
     */
    private static final ThreadLocal<Integer> MASKING_DEPTH = new ThreadLocal<>();

    /**
     * 禁用脱敏（显示明文）
     *
     * <p>调用此方法后，当前线程的所有敏感数据序列化时将显示明文
     * <p>支持嵌套调用，使用引用计数管理
     *
     * @see #enableMasking()
     * @see #disable()
     */
    public static void disableMasking() {
        Integer currentDepth = MASKING_DEPTH.get();
        if (currentDepth == null) {
            MASKING_DEPTH.set(1);
        } else {
            MASKING_DEPTH.set(currentDepth + 1);
        }
        log.debug("数据脱敏已禁用 - 线程: {}, 深度: {}", Thread.currentThread().getName(), MASKING_DEPTH.get());
    }

    /**
     * 启用脱敏（默认状态）
     *
     * <p>恢复数据脱敏功能，敏感数据将被自动脱敏
     * <p>使用引用计数，只有当所有嵌套的 Scope 都关闭后才真正恢复脱敏
     */
    public static void enableMasking() {
        Integer currentDepth = MASKING_DEPTH.get();
        if (currentDepth == null || currentDepth <= 1) {
            MASKING_DEPTH.remove();
            log.debug("数据脱敏已启用 - 线程: {}", Thread.currentThread().getName());
        } else {
            MASKING_DEPTH.set(currentDepth - 1);
            log.debug("数据脱敏深度减少 - 线程: {}, 深度: {}", Thread.currentThread().getName(), currentDepth - 1);
        }
    }

    /**
     * 检查当前是否禁用了脱敏
     *
     * @return true = 禁用脱敏（显示明文），false = 启用脱敏
     */
    public static boolean isMaskingDisabled() {
        Integer depth = MASKING_DEPTH.get();
        return depth != null && depth > 0;
    }

    /**
     * 创建一个自动恢复的脱敏作用域
     *
     * <p>使用 try-with-resources 语法，自动管理脱敏状态的恢复
     *
     * <h3>使用示例</h3>
     * <pre>
     * // 禁用脱敏
     * try (DataMaskingContext.Scope scope = DataMaskingContext.disable()) {
     *     // 此处代码执行时，敏感数据显示明文
     *     Patient patient = patientService.getById(id);
     *     return Result.success(patient);
     * }
     * // 自动恢复脱敏状态
     * </pre>
     *
     * @return Scope 对象，用于自动恢复脱敏状态
     */
    public static Scope disable() {
        disableMasking();
        return new Scope();
    }

    /**
     * 脱敏作用域
     *
     * <p>实现了 AutoCloseable 接口，支持 try-with-resources 语法
     * <p>当作用域结束时（close 方法被调用），自动恢复脱敏状态
     */
    public static class Scope implements AutoCloseable {

        private final String threadName;
        private final long startTime;

        /**
         * 私有构造函数，只能通过 {@link DataMaskingContext#disable()} 创建
         */
        private Scope() {
            this.threadName = Thread.currentThread().getName();
            this.startTime = System.currentTimeMillis();
        }

        /**
         * 关闭作用域，恢复脱敏状态
         *
         * <p>此方法会在 try-with-resources 语法的代码块结束时自动调用
         */
        @Override
        public void close() {
            enableMasking();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("脱敏作用域已关闭 - 线程: {}, 持续时间: {}ms", threadName, duration);
        }
    }

    /**
     * 获取当前线程的脱敏状态（用于调试和监控）
     *
     * @return 当前脱敏状态描述
     */
    public static String getStatus() {
        if (isMaskingDisabled()) {
            return "DISABLED (显示明文)";
        } else {
            return "ENABLED (敏感数据脱敏)";
        }
    }
}
