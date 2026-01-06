package com.his.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 科室信息实体类
 *
 * <p>映射数据库表 his_department，存储医院的科室组织架构信息</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>科室管理</b>：科室代码、名称、描述等基本信息管理</li>
 *   <li><b>层级结构</b>：支持科室树形结构（父子科室关系）</li>
 *   <li><b>排序管理</b>：通过sortOrder控制科室显示顺序</li>
 *   <li><b>状态管理</b>：科室启用/停用状态管理</li>
 *   <li><b>审计字段</b>：创建时间、更新时间、创建人、更新人等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>deptCode</b>：科室代码全局唯一，用于标识和区分科室</li>
 *   <li><b>层级关系</b>：支持多级科室结构，通过parent_id建立父子关系</li>
 *   <li><b>状态管理</b>：0=停用, 1=启用，停用科室不能挂号和接诊</li>
 *   <li><b>软删除</b>：isDeleted=0表示正常，=1表示已删除（物理记录仍保留）</li>
 *   <li><b>删除限制</b>：科室下有医生或下级科室时不能删除</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>父科室</b>：多对一自关联关系，形成科室树形结构</li>
 *   <li><b>子科室</b>：一对多自关联关系，一个科室可以有多个下级科室</li>
 *   <li><b>医生列表</b>：一对多关系，一个科室可以有多个医生</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>患者挂号</b>：选择科室进行挂号，显示科室树供患者选择</li>
 *   <li><b>医生管理</b>：医生归属到科室，按科室查看和管理医生</li>
 *   <li><b>统计报表</b>：按科室统计挂号量、收费额等业务数据</li>
 *   <li><b>权限管理</b>：根据科室控制用户的数据访问权限</li>
 *   <li><b>排班管理</b>：按科室进行医生排班</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Doctor
 * @see Registration
 */
@Data
@Entity
@Table(name = "his_department")
public class Department {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 科室代码
     */
    @Column(name = "dept_code", nullable = false, length = 50)
    private String deptCode;

    /**
     * 科室名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

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
     * 上级科室
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department parent;

    /**
     * 下级科室列表
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Department> children;

    /**
     * 排序序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 科室描述
     */
    @Column(name = "description", length = 500)
    private String description;

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
     * 科室下的医生列表
     */
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Doctor> doctors;

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
