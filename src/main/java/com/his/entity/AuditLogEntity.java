package com.his.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

/**
 * 审计日志实体
 *
 * <p>用于满足HIPAA和等保三级审计要求,记录所有关键业务操作</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>操作追踪</b>：记录谁、在什么时间、做了什么操作</li>
 *   <li><b>链路追踪</b>：通过TraceId关联整个请求链路的日志</li>
 *   <li><b>异常记录</b>：记录操作失败的异常信息</li>
 *   <li><b>审计查询</b>：支持按模块、操作人、时间范围等条件查询</li>
 * </ul>
 *
 * <h3>数据保留策略</h3>
 * <ul>
 *   <li><b>保留期</b>：180天（6个月）,满足合规要求</li>
 *   <li><b>清理方式</b>：定时任务自动清理过期数据</li>
 *   <li><b>归档</b>：可选择性归档到离线存储</li>
 * </ul>
 *
 * <h3>索引设计</h3>
 * <ul>
 *   <li><b>module</b>：按模块查询（如"认证管理"、"挂号管理"）</li>
 *   <li><b>operator_id</b>：按操作人查询（审计某用户的所有操作）</li>
 *   <li><b>trace_id</b>：按TraceId查询（关联整个请求链路）</li>
 *   <li><b>create_time</b>：按时间范围查询（审计日志报表）</li>
 *   <li><b>audit_type</b>：按审计类型查询（敏感操作、业务操作等）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.AuditLogService
 * @see com.his.repository.AuditLogRepository
 */
@Data
@Entity
@Table(name = "sys_audit_log", indexes = {
    @Index(name = "idx_audit_module", columnList = "module"),
    @Index(name = "idx_audit_operator", columnList = "operator_id"),
    @Index(name = "idx_audit_trace_id", columnList = "trace_id"),
    @Index(name = "idx_audit_create_time", columnList = "create_time"),
    @Index(name = "idx_audit_type", columnList = "audit_type")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ====================================================================================
    // 基本信息
    // ====================================================================================

    /**
     * 业务模块名称
     *
     * <p>示例：认证管理、挂号管理、处方管理、药房管理、收费管理</p>
     */
    @Column(nullable = false, length = 50)
    private String module;

    /**
     * 操作描述
     *
     * <p>示例：用户登录、患者挂号、开具处方、发药、收费</p>
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * 审计类型
     *
     * <p>取值范围：</p>
     * <ul>
     *   <li>SENSITIVE_OPERATION - 敏感操作（登录、登出、退费等）</li>
     *   <li>BUSINESS - 业务操作（挂号、开处方、发药等）</li>
     *   <li>DATA_ACCESS - 数据访问（查看患者详细信息等）</li>
     * </ul>
     */
    @Column(length = 20)
    private String auditType;

    /**
     * 操作描述详情
     *
     * <p>详细说明本次操作的具体内容</p>
     */
    @Column(length = 500)
    private String description;

    // ====================================================================================
    // 操作人信息
    // ====================================================================================

    /**
     * 操作人ID
     *
     * <p>系统用户表(sys_user)的主键,为null表示系统操作</p>
     */
    @Column(name = "operator_id")
    private Long operatorId;

    /**
     * 操作人用户名
     *
     * <p>冗余存储用户名,便于查询时快速显示,避免频繁关联查询</p>
     */
    @Column(name = "operator_username", length = 50)
    private String operatorUsername;

    // ====================================================================================
    // 请求信息
    // ====================================================================================

    /**
     * 链路追踪ID
     *
     * <p>32位十六进制字符串,用于关联整个请求链路的所有日志</p>
     * <p>由MDC(Mapped Diagnostic Context)自动生成和传递</p>
     */
    @Column(length = 64)
    private String traceId;

    /**
     * 客户端IP地址
     *
     * <p>支持反向代理(X-Forwarded-For),记录真实客户端IP</p>
     */
    @Column(name = "request_ip", length = 50)
    private String requestIp;

    /**
     * 用户代理(User-Agent)
     *
     * <p>记录客户端浏览器、操作系统等信息</p>
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ====================================================================================
    // 执行结果
    // ====================================================================================

    /**
     * 执行状态
     *
     * <p>取值范围：</p>
     * <ul>
     *   <li>SUCCESS - 操作成功</li>
     *   <li>FAILURE - 操作失败</li>
     * </ul>
     */
    @Column(length = 20)
    private String status;

    /**
     * 执行时间(毫秒)
     *
     * <p>从方法开始到结束的耗时,用于性能监控</p>
     */
    @Column(name = "execution_time")
    private Long executionTime;

    // ====================================================================================
    // 异常信息(仅失败时记录)
    // ====================================================================================

    /**
     * 异常类型
     *
     * <p>示例：BusinessException, NullPointerException, SQLException</p>
     */
    @Column(name = "exception_type", length = 100)
    private String exceptionType;

    /**
     * 异常消息
     *
     * <p>限制1000字符,避免过长影响存储</p>
     */
    @Column(name = "exception_message", length = 1000)
    private String exceptionMessage;

    // ====================================================================================
    // 时间戳
    // ====================================================================================

    /**
     * 创建时间
     *
     * <p>自动设置为当前时间,且不可更新</p>
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createTime;
}
