package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品信息表
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
