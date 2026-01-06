package com.his.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.his.common.SensitiveData;
import com.his.common.SensitiveType;
import com.his.config.SensitiveDataSerializer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 医生信息实体类
 *
 * <p>映射数据库表 his_doctor，存储医生的基本信息、资质、联系方式等核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>医生基本信息</b>：工号、姓名、性别、职称、专长等</li>
 *   <li><b>资质管理</b>：医师执业证号等资质信息</li>
 *   <li><b>联系方式</b>：电话、邮箱等（带敏感数据脱敏）</li>
 *   <li><b>状态管理</b>：医生启用/停用状态管理</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>数据脱敏</h3>
 * <p>以下字段使用 {@link com.his.common.SensitiveData} 注解进行自动脱敏：</p>
 * <ul>
 *   <li><b>phone</b>：手机号脱敏（保留前3后4位）</li>
 *   <li><b>email</b>：邮箱脱敏（保留前2后3位）</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>doctorNo</b>：医生工号全局唯一，医生建档时分配</li>
 *   <li><b>科室归属</b>：每个医生必须归属一个科室，不可为空</li>
 *   <li><b>状态管理</b>：0=停用, 1=启用，停用医生不能接诊和开处方</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 *   <li><b>资质要求</b>：licenseNo为执业证号，应该是唯一的</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>科室</b>：多对一关系，医生归属于某个科室</li>
 *   <li><b>挂号记录</b>：一对多关系，一个医生可以接诊多次挂号</li>
 *   <li><b>病历记录</b>：一对多关系，一个医生可以编写多份病历</li>
 *   <li><b>处方记录</b>：一对多关系，一个医生可以开具多张处方</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>挂号时</b>：患者选择医生进行挂号</li>
 *   <li><b>接诊时</b>：医生查看自己的挂号列表，接诊患者</li>
 *   <li><b>开处方时</b>：医生为患者开具处方，系统记录开方医生</li>
 *   <li><b>排班管理</b>：根据医生状态和科室进行排班</li>
 *   <li><b>绩效考核</b>：统计医生的接诊量、处方量等</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Department
 * @see Registration
 * @see MedicalRecord
 * @see Prescription
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
