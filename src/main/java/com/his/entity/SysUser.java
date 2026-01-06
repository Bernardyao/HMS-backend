package com.his.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Data;

/**
 * 系统用户实体类
 *
 * <p>映射数据库表 his_sysuser，存储系统用户认证和授权信息</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>用户认证</b>：用户名、密码（BCrypt加密）管理</li>
 *   <li><b>角色管理</b>：支持多种角色（管理员、医生、护士、药师、收费员）</li>
 *   <li><b>关联管理</b>：关联科室和业务实体（医生、护士等）</li>
 *   <li><b>状态管理</b>：用户启用/停用状态控制</li>
 *   <li><b>审计字段</b>：创建时间、更新时间等</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>username</b>：用户名全局唯一，用于系统登录</li>
 *   <li><b>password</b>：使用BCrypt算法加密存储，不可逆</li>
 *   <li><b>角色类型</b>：ADMIN=管理员, DOCTOR=医生, NURSE=护士, PHARMACIST=药师, CASHIER=收费员</li>
 *   <li><b>状态管理</b>：0=停用, 1=启用，停用用户不能登录系统</li>
 *   <li><b>科室关联</b>：departmentId记录用户所属科室（可为空）</li>
 *   <li><b>业务关联</b>：relatedId关联到具体的业务实体（如医生ID、护士ID）</li>
 * </ul>
 *
 * <h3>关联关系</h3>
 * <ul>
 *   <li><b>科室</b>：通过departmentId关联，用于数据权限控制</li>
 *   <li><b>业务实体</b>：通过relatedId关联到医生、护士等具体业务实体</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>用户登录</b>：通过用户名和密码进行身份认证</li>
 *   <li><b>权限控制</b>：根据角色控制用户访问权限</li>
 *   <li><b>数据过滤</b>：根据科室和角色过滤用户可见数据</li>
 *   <li><b>操作审计</b>：记录用户操作日志，追踪操作人员</li>
 *   <li><b>会话管理</b>：管理用户登录会话和在线状态</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Doctor
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
     * 所属科室ID
     */
    @Column(name = "department_main_id")
    private Long departmentId;

    /**
     * 关联业务实体ID（医生ID、护士ID等）
     */
    @Column(name = "related_id")
    private Long relatedId;

    /**
     * 状态（0=停用, 1=启用）
     */
    @Column(name = "status", nullable = false)
    private Short status;

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
