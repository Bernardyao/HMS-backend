package com.his.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费记录实体类
 *
 * <p>映射数据库表 his_charge，存储门诊收费业务的核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>收费管理</b>：记录收费单号、收费类型、应收金额、实收金额等</li>
 *   <li><b>支付管理</b>：支持多种支付方式（现金、银行卡、微信、支付宝、医保）</li>
 *   <li><b>退费管理</b>：记录退费金额、退费时间、退费原因</li>
 *   <li><b>优惠管理</b>：记录优惠金额、医保金额等</li>
 *   <li><b>发票管理</b>：生成和管理发票号</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>chargeNo</b>：收费单号全局唯一，由序列生成，收费时自动分配</li>
 *   <li><b>收费类型</b>：1=挂号费, 2=药费, 3=检查费, 4=治疗费</li>
 *   <li><b>状态流转</b>：未缴费(0) → 已缴费(1) → 已退费(2)</li>
 *   <li><b>金额计算</b>：actualAmount = totalAmount - discountAmount - insuranceAmount</li>
 *   <li><b>退费限制</b>：已退费的记录不能再次退费</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>患者</b>：多对一关系，记录缴费归属患者</li>
 *   <li><b>挂号记录</b>：多对一关系（可选），关联对应的挂号</li>
 *   <li><b>收费明细</b>：一对多关系，一笔收费可以包含多个收费明细项</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>挂号缴费</b>：患者挂号后缴纳挂号费</li>
 *   <li><b>处方缴费</b>：医生开处方后，患者缴纳药费</li>
 *   <li><b>退费处理</b>：患者退药或取消检查时，进行退费操作</li>
 *   <li><b>收费统计</b>：按日期、科室、医生等维度统计收费情况</li>
 *   <li><b>财务对账</b>：根据收费记录和支付方式进行财务对账</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Patient
 * @see Registration
 * @see ChargeDetail
 */
@Data
@Entity
@Table(name = "his_charge")
public class Charge {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 患者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Patient patient;

    /**
     * 收费单号
     */
    @Column(name = "charge_no", nullable = false, length = 50)
    private String chargeNo;

    /**
     * 收费类型（1=挂号费, 2=药费, 3=检查费, 4=治疗费）
     */
    @Column(name = "charge_type", nullable = false)
    private Short chargeType = 1;

    /**
     * 应收金额
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 实收金额
     */
    @Column(name = "actual_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualAmount;

    /**
     * 状态（0=未缴费, 1=已缴费, 2=已退费）
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
     * 挂号记录
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_main_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Registration registration;

    /**
     * 优惠金额
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 医保金额
     */
    @Column(name = "insurance_amount", precision = 10, scale = 2)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /**
     * 支付方式（1=现金, 2=银行卡, 3=微信, 4=支付宝, 5=医保）
     */
    @Column(name = "payment_method")
    private Short paymentMethod;

    /**
     * 交易流水号
     */
    @Column(name = "transaction_no", length = 100)
    private String transactionNo;

    /**
     * 收费时间
     */
    @Column(name = "charge_time")
    private LocalDateTime chargeTime;

    /**
     * 收费员ID
     */
    @Column(name = "cashier_main_id")
    private Long cashierMainId;

    /**
     * 退费金额
     */
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * 退费时间
     */
    @Column(name = "refund_time")
    private LocalDateTime refundTime;

    /**
     * 退费原因
     */
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    /**
     * 发票号
     */
    @Column(name = "invoice_no", length = 50)
    private String invoiceNo;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

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
     * 收费明细列表
     */
    @OneToMany(mappedBy = "charge", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ChargeDetail> details;

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
