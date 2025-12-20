package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子病历表
 */
@Data
@Entity
@Table(name = "his_medical_record")
public class MedicalRecord {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 挂号记录（一对一）
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Registration registration;

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
     * 病历编号
     */
    @Column(name = "record_no", nullable = false, length = 50)
    private String recordNo;

    /**
     * 状态（0=草稿, 1=已提交, 2=已审核）
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
     * 就诊时间
     */
    @Column(name = "visit_time")
    private LocalDateTime visitTime;

    /**
     * 主诉
     */
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    /**
     * 现病史
     */
    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;

    /**
     * 既往史
     */
    @Column(name = "past_history", columnDefinition = "TEXT")
    private String pastHistory;

    /**
     * 个人史
     */
    @Column(name = "personal_history", columnDefinition = "TEXT")
    private String personalHistory;

    /**
     * 家族史
     */
    @Column(name = "family_history", columnDefinition = "TEXT")
    private String familyHistory;

    /**
     * 体格检查
     */
    @Column(name = "physical_exam", columnDefinition = "TEXT")
    private String physicalExam;

    /**
     * 辅助检查
     */
    @Column(name = "auxiliary_exam", columnDefinition = "TEXT")
    private String auxiliaryExam;

    /**
     * 诊断
     */
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    /**
     * 诊断编码
     */
    @Column(name = "diagnosis_code", length = 50)
    private String diagnosisCode;

    /**
     * 治疗方案
     */
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    /**
     * 医嘱
     */
    @Column(name = "doctor_advice", columnDefinition = "TEXT")
    private String doctorAdvice;

    /**
     * 版本号
     */
    @Column(name = "version")
    @Version
    private Integer version = 1;

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
     * 处方列表
     */
    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Prescription> prescriptions;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        visitTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
