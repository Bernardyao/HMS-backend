package com.his.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方明细实体类
 *
 * <p>映射数据库表 his_prescription_detail，存储处方中药品使用的详细数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品信息</b>：记录处方中包含的药品名称、规格、单位等</li>
 *   <li><b>用量管理</b>：记录药品数量、单价、小计金额</li>
 *   <li><b>用药指导</b>：记录用药频率、用量、用药途径、用药天数、用药说明</li>
 *   <li><b>排序管理</b>：通过sortOrder控制药品在处方中的显示顺序</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>药品关联</b>：必须关联到有效的药品记录</li>
 *   <li><b>数量控制</b>：quantity为药品数量，必须大于0</li>
 *   <li><b>金额计算</b>：subtotal = unitPrice * quantity</li>
 *   <li><b>用药信息</b>：frequency（如"一日三次"）、dosage（如"每次1片"）、route（如"口服"）</li>
 *   <li><b>排序控制</b>：sortOrder值越小，显示越靠前</li>
 *   <li><b>级联删除</b>：删除处方主记录时，级联删除对应的明细项</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>处方主记录</b>：多对一关系，一个明细项归属于一张处方</li>
 *   <li><b>药品</b>：多对一关系，记录明细项使用的药品</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>开处方</b>：医生为患者开具处方，添加药品明细，填写用药指导</li>
 *   <li><b>处方审核</b>：药师审核处方，检查用药合理性</li>
 *   <li><b>发药</b>：药房根据处方明细发药，扣减药品库存</li>
 *   <li><b>处方查询</b>：患者和医生查看处方明细和用药指导</li>
 *   <li><b>退药</b>：患者退药时，根据处方明细退回药品，增加库存</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Prescription
 * @see Medicine
 */
@Data
@Entity
@Table(name = "his_prescription_detail")
public class PrescriptionDetail {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 处方
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Prescription prescription;

    /**
     * 药品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Medicine medicine;

    /**
     * 药品名称
     */
    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    /**
     * 单价
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice;

    /**
     * 数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 小计
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

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
     * 规格
     */
    @Column(name = "specification", length = 100)
    private String specification;

    /**
     * 单位
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 用药频率
     */
    @Column(name = "frequency", length = 50)
    private String frequency;

    /**
     * 用量
     */
    @Column(name = "dosage", length = 50)
    private String dosage;

    /**
     * 用药途径
     */
    @Column(name = "route", length = 50)
    private String route;

    /**
     * 用药天数
     */
    @Column(name = "days")
    private Integer days;

    /**
     * 用药说明
     */
    @Column(name = "instructions", length = 500)
    private String instructions;

    /**
     * 排序序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
