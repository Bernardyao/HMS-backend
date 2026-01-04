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
 * 患者信息实体类
 *
 * <p>映射数据库表 his_patient，存储患者的基本信息、联系方式、病史等核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>患者基本信息</b>：病历号、姓名、性别、年龄、出生日期等</li>
 *   <li><b>联系方式</b>：电话、身份证、医保卡号、地址、紧急联系人等（带敏感数据脱敏）</li>
 *   <li><b>医疗信息</b>：血型、过敏史、既往病史等</li>
 *   <li><b>关联关系</b>：与挂号、病历、处方、收费记录一对一或多对关系</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>数据脱敏</h3>
 * <p>以下字段使用 {@link com.his.common.SensitiveData} 注解进行自动脱敏：</p>
 * <ul>
 *   <li><b>phone</b>：手机号脱敏（保留前3后4位）</li>
 *   <li><b>idCard</b>：身份证号脱敏（保留前6后4位）</li>
 *   <li><b>medicalCardNo</b>：医保卡号脱敏</li>
 *   <li><b>address</b>：地址脱敏（保留省市区）</li>
 *   <li><b>emergencyContact</b>：紧急联系人姓名脱敏</li>
 *   <li><b>emergencyPhone</b>：紧急联系电话脱敏</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>patientNo</b>：病历号全局唯一，由序列生成，患者建档时自动分配</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 *   <li><b>生命周期</b>：@PrePersist和@PreUpdate自动管理createdAt和updatedAt</li>
 *   <li><b>懒加载</b>：关联的挂号、病历、处方、收费记录使用LAZY加载，避免N+1查询</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>挂号记录</b>：一对多关系，一个患者可以有多次挂号</li>
 *   <li><b>病历记录</b>：一对多关系，一个患者可以有多份病历</li>
 *   <li><b>处方记录</b>：一对多关系，一个患者可以有多张处方</li>
 *   <li><b>收费记录</b>：一对多关系，一个患者可以有多次收费</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>挂号时</b>：老患者根据姓名/身份证号查找，新患者建档创建记录</li>
 *   <li><b>就诊时</b>：医生查看患者基本信息、过敏史、既往病史</li>
 *   <li><b>开处方时</b>：关联患者ID，记录处方归属</li>
 *   <li><b>收费时</b>：关联患者ID，记录缴费归属</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Registration
 * @see MedicalRecord
 * @see Prescription
 * @see Charge
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
