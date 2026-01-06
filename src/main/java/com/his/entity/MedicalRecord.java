package com.his.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 电子病历实体类
 *
 * <p>映射数据库表 his_medical_record，存储门诊就诊病历的核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>病历基本信息</b>：病历编号、就诊时间、状态等</li>
 *   <li><b>病情记录</b>：主诉、现病史、既往史、个人史、家族史</li>
 *   <li><b>检查记录</b>：体格检查、辅助检查</li>
 *   <li><b>诊断治疗</b>：诊断、诊断编码、治疗方案、医嘱</li>
 *   <li><b>状态管理</b>：草稿、已提交、已审核</li>
 *   <li><b>版本控制</b>：使用@Version字段实现乐观锁，防止并发修改冲突</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>recordNo</b>：病历编号全局唯一，由序列生成，接诊时自动分配</li>
 *   <li><b>状态流转</b>：草稿(0) → 已提交(1) → 已审核(2)</li>
 *   <li><b>挂号关联</b>：一次挂号对应一份病历，一对一关系</li>
 *   <li><b>就诊时间</b>：记录实际就诊时间，默认为创建时间</li>
 *   <li><b>诊断编码</b>：使用ICD-10标准编码，便于统计和分析</li>
 *   <li><b>版本控制</b>：每次修改版本号递增，用于并发控制</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>挂号记录</b>：一对一关系，一次挂号对应一份病历</li>
 *   <li><b>患者</b>：多对一关系，记录病历归属患者</li>
 *   <li><b>医生</b>：多对一关系，记录接诊医生</li>
 *   <li><b>处方列表</b>：一对多关系，一份病历可以有多张处方</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>接诊记录</b>：医生接诊时编写病历，记录病情和诊断</li>
 *   <li><b>处方开立</b>：根据诊断结果开具处方</li>
 *   <li><b>病历查询</b>：患者和医生查询历史病历</li>
 *   <li><b>统计分析</b>：按诊断、科室、医生等维度统计分析</li>
 *   <li><b>病历审核</b>：资深医生审核病历，确保医疗质量</li>
 *   <li><b>病历归档</b>：病历审核后归档，便于日后查询</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Registration
 * @see Patient
 * @see Doctor
 * @see Prescription
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
     *   <li>设置visitTime为当前时间（记录就诊时间）</li>
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
        visitTime = LocalDateTime.now();
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
