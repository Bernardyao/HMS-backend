package com.his.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 挂号记录实体类
 *
 * <p>映射数据库表 his_registration，存储门诊挂号业务的核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>挂号管理</b>：记录患者挂号信息、就诊日期、就诊类型（初诊/复诊/急诊）</li>
 *   <li><b>费用管理</b>：记录挂号费、优惠金额等费用信息</li>
 *   <li><b>排号管理</b>：分配排队号，管理预约时间</li>
 *   <li><b>状态跟踪</b>：跟踪挂号状态（待就诊/已就诊/已取消）</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>regNo</b>：挂号流水号全局唯一，由序列生成，患者挂号时自动分配</li>
 *   <li><b>状态流转</b>：待就诊(0) → 已就诊(1) 或 已取消(2)，状态不可逆</li>
 *   <li><b>就诊类型</b>：1=初诊, 2=复诊, 3=急诊，不同类型收费可能不同</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 *   <li><b>取消限制</b>：已就诊的挂号不能取消，需记录取消原因</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>患者</b>：多对一关系，关联患者信息</li>
 *   <li><b>医生</b>：多对一关系，关联接诊医生</li>
 *   <li><b>科室</b>：多对一关系，关联就诊科室</li>
 *   <li><b>病历记录</b>：一对一关系，一次挂号对应一份病历</li>
 *   <li><b>收费记录</b>：一对多关系，一次挂号可以有多笔收费（挂号费、检查费等）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>患者挂号</b>：选择科室、医生、就诊日期，系统分配挂号流水号和排队号</li>
 *   <li><b>医生接诊</b>：查看挂号列表，按排队号顺序接诊，更新状态为"已就诊"</li>
 *   <li><b>取消挂号</b>：患者取消挂号，记录取消原因，状态更新为"已取消"</li>
 *   <li><b>统计分析</b>：统计各科室、各医生的挂号量、就诊率等</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Patient
 * @see Doctor
 * @see Department
 * @see MedicalRecord
 * @see Charge
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
     * 状态
     *
     * <p><b>状态枚举：</b></p>
     * <ul>
     *   <li>0 - 待就诊 (WAITING)</li>
     *   <li>1 - 已就诊 (COMPLETED)</li>
     *   <li>2 - 已取消 (CANCELLED)</li>
     *   <li>3 - 已退费 (REFUNDED)</li>
     *   <li>4 - 已缴挂号费 (PAID_REGISTRATION)</li>
     *   <li>5 - 就诊中 (IN_CONSULTATION)</li>
     * </ul>
     *
     * @see com.his.enums.RegStatusEnum
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
