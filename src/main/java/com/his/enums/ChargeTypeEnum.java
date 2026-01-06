package com.his.enums;

/**
 * 收费类型枚举
 *
 * <p>用于区分收费单的类型，支持灵活的收费模式。</p>
 *
 * <h3>收费模式说明</h3>
 * <ul>
 *   <li><b>REGISTRATION_ONLY</b>：仅挂号费收费单，适用于单独的挂号收费</li>
 *   <li><b>PRESCRIPTION_ONLY</b>：仅处方费收费单，适用于单独的处方收费</li>
 *   <li><b>MIXED</b>：混合收费（挂号费+处方费），适用于一次性收取挂号费和处方费的场景</li>
 * </ul>
 *
 * <h3>业务场景</h3>
 * <ul>
 *   <li><b>护士工作站</b>：通常只收取挂号费（REGISTRATION_ONLY）</li>
 *   <li><b>收费窗口</b>：可以收取挂号费（REGISTRATION_ONLY）或处方费（PRESCRIPTION_ONLY），也可混合收取（MIXED）</li>
 *   <li><b>便民服务</b>：支持挂号费和处方费一次性收取（MIXED），减少患者跑腿</li>
 * </ul>
 *
 * <p><b>向后兼容性：</b>枚举值与原有的数字代码完全兼容（1/2/3），
 * 但使用枚举可以提供更好的类型安全性和IDE智能提示。</p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 查询混合收费单
 * List&lt;Charge&gt; mixedCharges = chargeRepository.findByChargeType(ChargeTypeEnum.MIXED.getCode());
 *
 * // 根据代码获取枚举
 * ChargeTypeEnum type = ChargeTypeEnum.fromCode((short) 2);
 * if (type == ChargeTypeEnum.PRESCRIPTION_ONLY) {
 *     // 处理处方收费逻辑
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.enums.ChargeStatusEnum
 */
public enum ChargeTypeEnum {

    /**
     * 仅挂号费
     * <p>收费单仅包含挂号费，不包含处方费</p>
     * <p><b>典型场景：</b>护士收取挂号费、单独挂号收费窗口</p>
     *
     * @see #PRESCRIPTION_ONLY
     * @see #MIXED
     */
    REGISTRATION_ONLY((short) 1, "仅挂号费"),

    /**
     * 仅处方费
     * <p>收费单仅包含处方费，不包含挂号费</p>
     * <p><b>典型场景：</b>患者凭处方到收费窗口缴费</p>
     *
     * @see #REGISTRATION_ONLY
     * @see #MIXED
     */
    PRESCRIPTION_ONLY((short) 2, "仅处方费"),

    /**
     * 混合收费（挂号费+处方费）
     * <p>收费单同时包含挂号费和处方费，一次性收取</p>
     * <p><b>典型场景：</b>便民服务、减少患者排队次数</p>
     *
     * @see #REGISTRATION_ONLY
     * @see #PRESCRIPTION_ONLY
     */
    MIXED((short) 3, "混合收费");

    private final Short code;
    private final String description;

    /**
     * 构造枚举常量
     *
     * @param code        收费类型代码（与数据库存储一致）
     * @param description 收费类型描述（用于显示）
     */
    ChargeTypeEnum(Short code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取收费类型代码
     *
     * @return 收费类型代码
     */
    public Short getCode() {
        return code;
    }

    /**
     * 获取收费类型描述
     *
     * @return 收费类型描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举常量
     *
     * <p>提供类型安全的枚举查找方法，如果代码无效则抛出异常。</p>
     *
     * @param code 收费类型代码
     * @return 对应的枚举常量
     * @throws IllegalArgumentException 如果代码无效
     */
    public static ChargeTypeEnum fromCode(Short code) {
        for (ChargeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的收费类型代码: " + code);
    }
}
