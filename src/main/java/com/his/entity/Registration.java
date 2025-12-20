package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 挂号记录表
 */
@Data
@Entity
@Table(name = "his_registration")
public class Registration {

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
     * 医生
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Doctor doctor;

    /**
     * 科室
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    /**
     * 挂号流水号
     */
    @Column(name = "reg_no", nullable = false, length = 50)
    private String regNo;

    /**
     * 就诊日期
     */
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /**
     * 就诊类型（1=初诊, 2=复诊, 3=急诊）
     */
    @Column(name = "visit_type", nullable = false)
    private Short visitType = 1;

    /**
     * 挂号费
     */
    @Column(name = "registration_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal registrationFee = BigDecimal.ZERO;

    /**
     * 状态（0=待就诊, 1=已就诊, 2=已取消）
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
     * 预约时间
     */
    @Column(name = "appointment_time")
    private LocalDateTime appointmentTime;

    /**
     * 排队号
     */
    @Column(name = "queue_no", length = 20)
    private String queueNo;

    /**
     * 取消原因
     */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

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
     * 对应的病历记录（一对一）
     */
    @OneToOne(mappedBy = "registration", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private MedicalRecord medicalRecord;

    /**
     * 缴费记录列表
     */
    @OneToMany(mappedBy = "registration", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Charge> charges;

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
