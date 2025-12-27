package com.his.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 缴费记录表
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
