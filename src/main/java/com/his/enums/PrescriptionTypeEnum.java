package com.his.enums;

/**
 * 处方类型枚举
 *
 * <p>用于区分处方的类型，西药和中药在系统中有不同的处理逻辑。</p>
 *
 * <h3>处方类型说明</h3>
 * <ul>
 *   <li><b>WESTERN_MEDICINE（西药处方）</b>：包含西药的处方，使用西药处方管理系统</li>
 *   <li><b>CHINESE_MEDICINE（中药处方）</b>：包含中药的处方，使用中药处方管理系统</li>
 * </ul>
 *
 * <h3>业务差异</h3>
 * <table border="1" style="border-collapse: collapse;">
 *   <tr>
 *     <th>维度</th>
 *     <th>西药处方</th>
 *     <th>中药处方</th>
 *   </tr>
 *   <tr>
 *     <td>药品管理系统</td>
 *     <td>西药库</td>
 *     <td>中药库</td>
 *   </tr>
 *   <tr>
 *     <td>发药流程</td>
 *     <td>批量发药</td>
 *     <td>药师调剂</td>
 *   </tr>
 *   <tr>
 *     <td>审核要求</td>
 *     <td>需要药师审核</td>
 *     <td>需要中药师审核</td>
 *   </tr>
 *   <tr>
 *     <td>库存管理</td>
 *     <td>整盒/整瓶管理</td>
 *     <td>重量/克数管理</td>
 *   </tr>
 * </table>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 查询西药处方
 * List&lt;Prescription&gt; westernPrescriptions = prescriptionRepository
 *     .findByPrescriptionType(PrescriptionTypeEnum.WESTERN_MEDICINE.getCode());
 *
 * // 检查处方类型
 * if (prescription.getPrescriptionType().equals(PrescriptionTypeEnum.CHINESE_MEDICINE.getCode())) {
 *     // 处理中药处方的特殊逻辑
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
public enum PrescriptionTypeEnum {

    /**
     * 西药处方
     * <p>包含西药的处方，使用西药管理系统进行管理</p>
     * <p><b>特征：</b></p>
     * <ul>
     *   <li>药品来自西药库</li>
     *   <li>使用说明书和用法用量</li>
 *   <li>按盒/瓶/片等单位管理</li>
     *   <li>需要西药师审核和发药</li>
     * </ul>
     *
     * @see #CHINESE_MEDICINE
     */
    WESTERN_MEDICINE((short) 1, "西药处方"),

    /**
     * 中药处方
     * <p>包含中药的处方，使用中药管理系统进行管理</p>
     * <p><b>特征：</b></p>
     * <ul>
     *   <li>药品来自中药库</li>
     *   <li>包含君臣佐使等配伍关系</li>
     *   <li>按克、两、钱等单位管理</li>
     *   <li>需要中药师调剂和煎煮指导</li>
     * </ul>
     *
     * @see #WESTERN_MEDICINE
     */
    CHINESE_MEDICINE((short) 2, "中药处方");

    private final Short code;
    private final String description;

    /**
     * 构造枚举常量
     *
     * @param code        处方类型代码（与数据库存储一致）
     * @param description 处方类型描述（用于显示）
     */
    PrescriptionTypeEnum(Short code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取处方类型代码
     *
     * @return 处方类型代码
     */
    public Short getCode() {
        return code;
    }

    /**
     * 获取处方类型描述
     *
     * @return 处方类型描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举常量
     *
     * <p>提供类型安全的枚举查找方法，如果代码无效则抛出异常。</p>
     *
     * @param code 处方类型代码
     * @return 对应的枚举常量
     * @throws IllegalArgumentException 如果代码无效
     */
    public static PrescriptionTypeEnum fromCode(Short code) {
        for (PrescriptionTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的处方类型代码: " + code);
    }
}
