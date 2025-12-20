package com.his.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 科室信息表
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
