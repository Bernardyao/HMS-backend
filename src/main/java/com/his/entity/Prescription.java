package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方主表
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
     * 状态（0=草稿, 1=已开方, 2=已审核, 3=已发药）
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
    private List<PrescriptionDetail> details;

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
