package com.his.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费明细实体类
 *
 * <p>映射数据库表 his_charge_detail，存储收费单的明细项数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>明细管理</b>：记录收费单的具体收费项目</li>
 *   <li><b>项目类型</b>：支持多种项目类型（挂号费、处方药费等）</li>
 *   <li><b>项目关联</b>：通过itemType和itemId关联到具体业务对象</li>
 *   <li><b>金额记录</b>：记录每个明细项的金额</li>
 *   <li><b>审计字段</b>：创建时间等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>项目类型</b>：REGISTRATION=挂号费, PRESCRIPTION=处方药费</li>
 *   <li><b>项目关联</b>：itemId关联到具体业务实体的ID（如挂号ID、处方ID）</li>
 *   <li><b>金额计算</b>：itemAmount为该明细项的金额，所有明细项金额之和等于收费主记录的totalAmount</li>
 *   <li><b>级联删除</b>：删除收费主记录时，级联删除对应的明细项</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>收费主记录</b>：多对一关系，一个明细项归属于一个收费单</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>挂号收费</b>：创建挂号收费明细，itemType=REGISTRATION</li>
 *   <li><b>处方收费</b>：创建处方收费明细，itemType=PRESCRIPTION</li>
 *   <li><b>收费查询</b>：查看收费单的明细项，了解具体收费内容</li>
 *   <li><b>财务统计</b>：按项目类型统计收费情况</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Charge
 * @see Registration
 * @see Prescription
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
    }
}
