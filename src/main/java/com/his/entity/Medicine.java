package com.his.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 药品信息实体类
 *
 * <p>映射数据库表 his_medicine，存储药房药品管理的核心数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品基本信息</b>：药品编码、名称、通用名、规格、剂型等</li>
 *   <li><b>价格管理</b>：进货价格、零售价格，支持价格调整</li>
 *   <li><b>库存管理</b>：当前库存、最低库存、最高库存，支持库存预警</li>
 *   <li><b>供应商管理</b>：生产厂家、批准文号等资质信息</li>
 *   <li><b>分类管理</b>：药品分类、是否处方药等</li>
 *   <li><b>储存管理</b>：储存条件、过期预警天数</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>medicineCode</b>：药品编码全局唯一，用于标识和区分药品</li>
 *   <li><b>库存控制</b>：库存低于minStock时预警，高于maxStock时告警</li>
 *   <li><b>价格管理</b>：retailPrice为零售价，purchasePrice为进货价</li>
 *   <li><b>状态管理</b>：0=停用, 1=启用，停用药品不能开处方</li>
 *   <li><b>处方药标识</b>：isPrescription=1表示处方药，需要医生开具处方</li>
 *   <li><b>过期预警</b>：距离过期日期不足expiryWarningDays天时预警</li>
 *   <li><b>乐观锁</b>：使用@Version字段实现并发控制，防止库存超卖</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>处方明细</b>：一对多关系，一个药品可以在多张处方中被使用</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>开处方</b>：医生开具处方时选择药品，系统自动校验库存</li>
 *   <li><b>发药</b>：药房发药时扣减库存，更新药品数量</li>
 *   <li><b>退药</b>：患者退药时增加库存</li>
 *   <li><b>库存预警</b>：定时任务检查库存，低于预警线时提醒采购</li>
 *   <li><b>价格管理</b>：调整药品零售价，影响后续处方收费</li>
 *   <li><b>药品采购</b>：根据库存情况和销售数据制定采购计划</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see PrescriptionDetail
 */
@Data
@Entity
@Table(name = "his_medicine")
public class Medicine {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 药品编码
     */
    @Column(name = "medicine_code", nullable = false, length = 50)
    private String medicineCode;

    /**
     * 药品名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 零售价格
     */
    @Column(name = "retail_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal retailPrice;

    /**
     * 库存数量
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

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
     * 通用名称
     */
    @Column(name = "generic_name", length = 200)
    private String genericName;

    /**
     * 规格
     */
    @Column(name = "specification", length = 100)
    private String specification;

    /**
     * 剂型
     */
    @Column(name = "dosage_form", length = 50)
    private String dosageForm;

    /**
     * 生产厂家
     */
    @Column(name = "manufacturer", length = 200)
    private String manufacturer;

    /**
     * 批准文号
     */
    @Column(name = "approval_no", length = 100)
    private String approvalNo;

    /**
     * 药品分类
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 单位
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 进货价格
     */
    @Column(name = "purchase_price", precision = 10, scale = 4)
    private BigDecimal purchasePrice;

    /**
     * 最低库存
     */
    @Column(name = "min_stock")
    private Integer minStock = 0;

    /**
     * 最高库存
     */
    @Column(name = "max_stock")
    private Integer maxStock;

    /**
     * 储存条件
     */
    @Column(name = "storage_condition", length = 100)
    private String storageCondition;

    /**
     * 过期预警天数
     */
    @Column(name = "expiry_warning_days")
    private Integer expiryWarningDays = 90;

    /**
     * 是否处方药（0=否, 1=是）
     */
    @Column(name = "is_prescription")
    private Short isPrescription = 0;

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
     * 处方明细列表
     */
    @OneToMany(mappedBy = "medicine", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<PrescriptionDetail> prescriptionDetails;

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
