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
import java.time.LocalDateTime;
import java.util.List;

/**
 * 医生信息表
 */
@Data
@Entity
@Table(name = "his_doctor")
public class Doctor {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 所属科室
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    /**
     * 医生工号
     */
    @Column(name = "doctor_no", nullable = false, length = 50)
    private String doctorNo;

    /**
     * 医生姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 性别（0=女, 1=男, 2=未知）
     */
    @Column(name = "gender", nullable = false)
    private Short gender;

    /**
     * 状态（0=停用, 1=启用）
     */
    @Column(name = "status", nullable = false)
    private Short status = 1;

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
     * 职称
     */
    @Column(name = "title", length = 50)
    private String title;

    /**
     * 专长
     */
    @Column(name = "specialty", length = 200)
    private String specialty;

    /**
     * 联系电话
     */
    @SensitiveData(type = SensitiveType.PHONE)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 电子邮箱
     */
    @SensitiveData(type = SensitiveType.EMAIL)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 医师执业证号
     */
    @Column(name = "license_no", length = 50)
    private String licenseNo;

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
     * 医生的挂号记录列表
     */
    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Registration> registrations;

    /**
     * 医生的病历记录列表
     */
    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<MedicalRecord> medicalRecords;

    /**
     * 医生开具的处方列表
     */
    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Prescription> prescriptions;

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
