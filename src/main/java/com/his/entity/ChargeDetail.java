package com.his.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费明细表
 */
@Data
@Entity
@Table(name = "his_charge_detail")
public class ChargeDetail {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 收费主记录
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_main_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Charge charge;

    /**
     * 项目类型（REGISTRATION=挂号费, PRESCRIPTION=处方药费）
     */
    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    /**
     * 项目关联ID
     */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /**
     * 项目名称
     */
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    /**
     * 项目金额
     */
    @Column(name = "item_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemAmount;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
