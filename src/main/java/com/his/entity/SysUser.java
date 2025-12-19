package com.his.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 映射数据库表 his_sysuser
 */
@Data
@Entity
@Table(name = "his_sysuser")
public class SysUser {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long id;

    /**
     * 用户名（登录账号）
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 真实姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String realName;

    /**
     * 角色（ADMIN=管理员, DOCTOR=医生, NURSE=护士, PHARMACIST=药师, CASHIER=收费员）
     */
    @Column(name = "role_code", nullable = false, length = 50)
    private String role;

    /**
     * 关联ID（医生ID或其他业务ID）
     */
    @Column(name = "department_main_id")
    private Long relatedId;

    /**
     * 状态（0=停用, 1=启用）
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
