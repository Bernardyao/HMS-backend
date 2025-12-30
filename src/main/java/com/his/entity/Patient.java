package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.his.common.SensitiveData;
import com.his.common.SensitiveType;
import com.his.config.SensitiveDataSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者信息表
 */
@Data
@Entity
@Table(name = "his_patient")
public class Patient {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 病历号
     */
    @Column(name = "patient_no", nullable = false, length = 50)
    private String patientNo;

    /**
     * 患者姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 性别（0=女, 1=男, 2=未知）
     */
    @Column(name = "gender", nullable = false)
    private Short gender;

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
     * 出生日期
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * 年龄
     */
    @Column(name = "age")
    private Short age;

    /**
     * 联系电话
     */
    @SensitiveData(type = SensitiveType.PHONE)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 身份证号
     */
    @SensitiveData(type = SensitiveType.ID_CARD)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "id_card", length = 18)
    private String idCard;

    /**
     * 医保卡号
     */
    @SensitiveData(type = SensitiveType.BANK_CARD)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "medical_card_no", length = 50)
    private String medicalCardNo;

    /**
     * 联系地址
     */
    @SensitiveData(type = SensitiveType.ADDRESS)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "address", length = 500)
    private String address;

    /**
     * 紧急联系人
     */
    @SensitiveData(type = SensitiveType.NAME)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "emergency_contact", length = 50)
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    @SensitiveData(type = SensitiveType.PHONE)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    /**
     * 血型
     */
    @Column(name = "blood_type", length = 10)
    private String bloodType;

    /**
     * 过敏史
     */
    @Column(name = "allergy_history", columnDefinition = "TEXT")
    private String allergyHistory;

    /**
     * 既往病史
     */
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

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
     * 患者的挂号记录列表
     */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Registration> registrations;

    /**
     * 患者的病历记录列表
     */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<MedicalRecord> medicalRecords;

    /**
     * 患者的处方列表
     */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Prescription> prescriptions;

    /**
     * 患者的缴费记录列表
     */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
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
