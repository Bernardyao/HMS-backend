package com.his.enums;

/**
 * 病历状态枚举
 *
 * <p>用于标识病历的当前状态，控制病历的编辑权限和工作流。</p>
 *
 * <h3>状态流转说明</h3>
 * <pre>
 * DRAFT(草稿) → SUBMITTED(已提交) → AUDITED(已审核)
 *     ↓              ↓                  ↓
 * 可编辑       不可编辑           不可编辑
 * </pre>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li><b>DRAFT（草稿）</b>：医生正在编写病历，可自由编辑</li>
 *   <li><b>SUBMITTED（已提交）</b>：医生已完成病历编写并提交，不可编辑</li>
 *   <li><b>AUDITED（已审核）</b>：病历已通过审核，锁定不可编辑</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>新创建的病历默认为草稿状态</li>
 *   <li>草稿状态的病历可以编辑和删除</li>
 *   <li>已提交的病历不可编辑，确保医疗记录的严肃性</li>
 *   <li>已审核的病历具有法律效力，不可修改</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 查询草稿状态的病历
 * List&lt;MedicalRecord&gt; drafts = medicalRecordRepository
 *     .findByStatus(MedicalRecordStatusEnum.DRAFT.getCode());
 *
 * // 检查病历是否可以编辑
 * public boolean canEdit(MedicalRecord record) {
 *     return record.getStatus().equals(MedicalRecordStatusEnum.DRAFT.getCode());
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.enums.RegStatusEnum
 */
public enum MedicalRecordStatusEnum {

    /**
     * 草稿
     * <p>医生正在编写或修改病历，病历内容不完整或需要进一步确认</p>
     * <p><b>特征：</b></p>
     * <ul>
     *   <li>可以自由编辑病历内容</li>
     *   <li>可以删除病历</li>
     *   <li>不会出现在正式报表中</li>
     * </ul>
     *
     * @see #SUBMITTED
     * @see #AUDITED
     */
    DRAFT((short) 0, "草稿"),

    /**
     * 已提交
     * <p>医生已完成病历编写并提交，等待审核</p>
     * <p><b>特征：</b></p>
     * <ul>
     *   <li>病历已锁定，不可编辑</li>
     *   <li>已提交给审核人员</li>
     *   <li>具有医疗记录的法律效力</li>
     * </ul>
     *
     * @see #DRAFT
     * @see #AUDITED
     */
    SUBMITTED((short) 1, "已提交"),

    /**
     * 已审核
     * <p>病历已通过审核，确认为正式医疗记录</p>
     * <p><b>特征：</b></p>
     * <ul>
     *   <li>病历完全锁定，不可编辑</li>
     *   <li>具有完整的法律效力</li>
     *   <li>可作为医疗纠纷的证据</li>
     * </ul>
     *
     * @see #DRAFT
     * @see #SUBMITTED
     */
    AUDITED((short) 2, "已审核");

    private final Short code;
    private final String description;

    /**
     * 构造枚举常量
     *
     * @param code        病历状态代码（与数据库存储一致）
     * @param description 病历状态描述（用于显示）
     */
    MedicalRecordStatusEnum(Short code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取病历状态代码
     *
     * @return 病历状态代码
     */
    public Short getCode() {
        return code;
    }

    /**
     * 获取病历状态描述
     *
     * @return 病历状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举常量
     *
     * <p>提供类型安全的枚举查找方法，如果代码无效则抛出异常。</p>
     *
     * @param code 病历状态代码
     * @return 对应的枚举常量
     * @throws IllegalArgumentException 如果代码无效
     */
    public static MedicalRecordStatusEnum fromCode(Short code) {
        for (MedicalRecordStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的病历状态代码: " + code);
    }
}
