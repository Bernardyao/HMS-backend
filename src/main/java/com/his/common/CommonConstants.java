package com.his.common;

/**
 * 通用常量类
 *
 * <p>集中管理系统中使用的魔法数字（Magic Numbers），提供类型安全的常量定义。
 * 通过使用常量类替代硬编码的数字，可以：</p>
 * <ul>
 *   <li>提高代码可读性和可维护性</li>
 *   <li>避免拼写错误和无效值</li>
 *   <li>便于后续修改和重构</li>
 *   <li>提供IDE智能提示和重构支持</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>
 * // ❌ 错误：使用魔法数字
 * if (registration.getIsDeleted() == 1) { ... }
 *
 * // ✅ 正确：使用常量
 * if (CommonConstants.DELETED.equals(registration.getIsDeleted())) { ... }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
public class CommonConstants {

    /**
     * 软删除状态：正常（未删除）
     * <p>用于标识业务数据处于正常可用状态，所有查询默认过滤已删除的数据</p>
     *
     * @see #DELETED
     */
    public static final Short NORMAL = 0;

    /**
     * 软删除状态：已删除
     * <p>用于标识业务数据已被标记为删除，采用软删除策略而非物理删除</p>
     * <p><b>注意：</b>软删除的数据在业务查询中会被自动过滤，不影响正常业务</p>
     *
     * @see #NORMAL
     */
    public static final Short DELETED = 1;

    /**
     * 就诊类型：初诊
     * <p>患者首次到某科室就诊，挂号费按初诊标准收取</p>
     *
     * @see #VISIT_TYPE_RETURN
     * @see #VISIT_TYPE_EMERGENCY
     */
    public static final Short VISIT_TYPE_FIRST = 1;

    /**
     * 就诊类型：复诊
     * <p>患者曾在同一科室就诊过，再次就诊时按复诊标准收费</p>
     *
     * @see #VISIT_TYPE_FIRST
     * @see #VISIT_TYPE_EMERGENCY
     */
    public static final Short VISIT_TYPE_RETURN = 2;

    /**
     * 就诊类型：急诊
     * <p>患者因紧急情况就诊，挂号费按急诊标准收取，优先级高于普通门诊</p>
     *
     * @see #VISIT_TYPE_FIRST
     * @see #VISIT_TYPE_RETURN
     */
    public static final Short VISIT_TYPE_EMERGENCY = 3;

    /**
     * 科室/医生状态：启用
     * <p>标识科室或医生当前处于可用状态，可以正常接收患者</p>
     *
     * @see #STATUS_DISABLED
     */
    public static final Short STATUS_ENABLED = 1;

    /**
     * 科室/医生状态：停用
     * <p>标识科室或医生当前不可用，不允许接收新患者</p>
     * <p><b>注意：</b>停用后已完成挂号的患者不受影响</p>
     *
     * @see #STATUS_ENABLED
     */
    public static final Short STATUS_DISABLED = 0;
}
