package com.his.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 处方信息实体类
 *
 * <p>映射数据库表 his_prescription，存储门诊处方业务的核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>处方管理</b>：记录处方号、处方类型（西药/中药/中成药）、总金额等</li>
 *   <li><b>审核管理</b>：支持处方审核流程，记录审核医生和审核时间</li>
 *   <li><b>发药管理</b>：记录发药时间和发药人</li>
 *   <li><b>退药管理</b>：支持退药操作，记录退药时间和退药原因</li>
 *   <li><b>有效期管理</b>：设置处方有效期天数，超期不能取药</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>prescriptionNo</b>：处方号全局唯一，由序列生成，开处方时自动分配</li>
 *   <li><b>处方类型</b>：1=西药, 2=中药, 3=中成药</li>
 *   <li><b>状态流转</b>：草稿(0) → 已开方(1) → 已审核(2) → 已缴费(5) → 已发药(3) 或 已退费(4)</li>
 *   <li><b>审核要求</b>：处方必须经过审核才能发药</li>
 *   <li><b>缴费限制</b>：只有缴费后的处方才能发药</li>
 *   <li><b>有效期控制</b>：超过validityDays天的处方不能发药</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>病历</b>：多对一关系，一张处方归属于一份病历</li>
 *   <li><b>患者</b>：多对一关系，记录处方归属患者</li>
 *   <li><b>开方医生</b>：多对一关系，记录开具处方的医生</li>
 *   <li><b>审核医生</b>：多对一关系（可选），记录审核处方的医生</li>
 *   <li><b>处方明细</b>：一对多关系，一张处方包含多个处方明细项</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>开处方</b>：医生接诊后为患者开具处方</li>
 *   <li><b>审核处方</b>：资深医生审核处方，确保用药安全</li>
 *   <li><b>缴费</b>：患者缴纳处方费用，状态更新为"已缴费"</li>
 *   <li><b>发药</b>：药房根据处方发药，更新发药时间和发药人</li>
 *   <li><b>退药</b>：患者退药时，记录退药原因和退药时间</li>
 *   <li><b>处方查询</b>：患者和医生查询处方历史记录</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see MedicalRecord
 * @see Patient
 * @see Doctor
 * @see PrescriptionDetail
 */
@Data
@Entity
@Table(name = "his_prescription")
public class Prescription {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 病历
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MedicalRecord medicalRecord;

    /**
     * 患者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Patient patient;

    /**
     * 开方医生
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Doctor doctor;

    /**
     * 处方号
     */
    @Column(name = "prescription_no", nullable = false, length = 50)
    private String prescriptionNo;

    /**
     * 处方类型（1=西药, 2=中药, 3=中成药）
     */
    @Column(name = "prescription_type", nullable = false)
    private Short prescriptionType = 1;

    /**
     * 总金额
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 药品数量
     */
    @Column(name = "item_count", nullable = false)
    private Integer itemCount = 0;

    /**
     * 状态（0=草稿, 1=已开方, 2=已审核, 3=已发药, 4=已退费, 5=已缴费）
     */
    @Column(name = "status", nullable = false)
    private Short status = 0;

    /**
     * 软删除标记
     */
    @Column(name = "is_deleted", nullable = false)
    private Short isDeleted = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 有效天数
     */
    @Column(name = "validity_days")
    private Integer validityDays = 3;

    /**
     * 审核医生
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_doctor_main_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Doctor reviewDoctor;

    /**
     * 审核时间
     */
    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     */
    @Column(name = "review_remark", length = 500)
    private String reviewRemark;

    /**
     * 发药时间
     */
    @Column(name = "dispense_time")
    private LocalDateTime dispenseTime;

    /**
     * 发药人ID
     */
    @Column(name = "dispense_by")
    private Long dispenseBy;

    /**
     * 退药时间
     */
    @Column(name = "return_time")
    private LocalDateTime returnTime;

    /**
     * 退药原因
     */
    @Column(name = "return_reason", length = 500)
    private String returnReason;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 处方明细列表
     */
    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<PrescriptionDetail> details = new java.util.ArrayList<>();

    /**
     * JPA生命周期回调：实体持久化前自动调用
     *
     * <p>在首次保存（INSERT）到数据库之前，JPA自动调用此方法</p>
     *
     * <p><b>执行时机：</b></p>
     * <ul>
     *   <li>调用entityManager.persist()时</li>
     *   <li>调用repository.save()保存新实体时</li>
     *   <li>任何JPA事务提交前</li>
     * </ul>
     *
     * <p><b>自动操作：</b></p>
     * <ul>
     *   <li>设置createdAt为当前时间（记录创建时间）</li>
     *   <li>设置updatedAt为当前时间（初始更新时间）</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>此方法只在首次创建时调用，更新时不会调用</li>
     *   <li>不要在此方法中访问关联实体（可能触发懒加载异常）</li>
     *   <li>此方法不应抛出异常，否则会导致持久化失败</li>
     * </ul>
     *
     * @since 1.0
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA生命周期回调：实体更新前自动调用
     *
     * <p>在更新（UPDATE）数据库记录之前，JPA自动调用此方法</p>
     *
     * <p><b>执行时机：</b></p>
     * <ul>
     *   <li>调用entityManager.merge()时</li>
     *   <li>调用repository.save()更新已存在的实体时</li>
     *   <li>任何JPA事务提交前（检测到实体脏字段时）</li>
     * </ul>
     *
     * <p><b>自动操作：</b></p>
     * <ul>
     *   <li>更新updatedAt为当前时间（记录最后修改时间）</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>此方法只在更新时调用，创建时不会调用</li>
     *   <li>JPA脏检查机制：只有实体字段发生变化时才会触发</li>
     *   <li>不要在此方法中访问关联实体（可能触发N+1查询问题）</li>
     *   <li>此方法不应抛出异常，否则会导致更新失败</li>
     * </ul>
     *
     * @since 1.0
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
