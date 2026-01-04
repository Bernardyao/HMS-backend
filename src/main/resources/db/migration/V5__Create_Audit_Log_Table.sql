-- ========================================
-- 审计日志表 (V5)
-- 用于满足HIPAA和等保三级审计要求
--
-- 创建日期: 2025-01-03
-- 版本: 1.0
-- ========================================

-- 创建审计日志表
CREATE TABLE sys_audit_log (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 基本信息
    module VARCHAR(50) NOT NULL,                -- 模块名 (认证管理、挂号管理、处方管理、药房管理、收费管理)
    action VARCHAR(100) NOT NULL,               -- 操作名 (用户登录、患者挂号、开具处方、发药、收费)
    audit_type VARCHAR(20),                     -- 审计类型 (SENSITIVE_OPERATION, BUSINESS, DATA_ACCESS)
    description VARCHAR(500),                   -- 操作描述详情

    -- 操作人信息
    operator_id BIGINT,                         -- 操作人ID (sys_user.id)
    operator_username VARCHAR(50),              -- 操作人用户名 (冗余存储,便于查询)

    -- 请求信息
    trace_id VARCHAR(64),                       -- 链路追踪ID (32位十六进制字符串)
    request_ip VARCHAR(50),                     -- 客户端IP地址 (支持反向代理)
    user_agent VARCHAR(500),                    -- User-Agent (浏览器、操作系统等信息)

    -- 执行结果
    status VARCHAR(20),                         -- 执行状态 (SUCCESS, FAILURE)
    execution_time BIGINT,                      -- 执行时间(毫秒)

    -- 异常信息(仅失败时记录)
    exception_type VARCHAR(100),                -- 异常类型 (BusinessException, NullPointerException, etc.)
    exception_message VARCHAR(1000),            -- 异常消息 (限制1000字符)

    -- 时间戳
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

-- ====================================================================================
-- 创建索引 (提升查询性能)
-- ====================================================================================

-- 模块名索引 (用于按模块查询)
CREATE INDEX idx_audit_module ON sys_audit_log(module);

-- 操作人ID索引 (用于审计某用户的所有操作)
CREATE INDEX idx_audit_operator ON sys_audit_log(operator_id);

-- TraceId索引 (用于关联整个请求链路的日志)
CREATE INDEX idx_audit_trace_id ON sys_audit_log(trace_id);

-- 创建时间索引 (用于按时间范围查询和生成报表)
CREATE INDEX idx_audit_create_time ON sys_audit_log(create_time);

-- 审计类型索引 (用于查询敏感操作、业务操作等)
CREATE INDEX idx_audit_type ON sys_audit_log(audit_type);

-- ====================================================================================
-- 添加注释 (便于理解字段含义)
-- ====================================================================================

-- 表注释
COMMENT ON TABLE sys_audit_log IS '系统审计日志表,满足HIPAA和等保三级要求,记录所有关键业务操作';

-- 基本信息字段注释
COMMENT ON COLUMN sys_audit_log.module IS '业务模块名称 (认证管理、挂号管理、处方管理、药房管理、收费管理)';
COMMENT ON COLUMN sys_audit_log.action IS '操作描述 (用户登录、患者挂号、开具处方、发药、收费)';
COMMENT ON COLUMN sys_audit_log.audit_type IS '审计类型: SENSITIVE_OPERATION(敏感操作)、BUSINESS(业务操作)、DATA_ACCESS(数据访问)';
COMMENT ON COLUMN sys_audit_log.description IS '操作描述详情,说明本次操作的具体内容';

-- 操作人信息字段注释
COMMENT ON COLUMN sys_audit_log.operator_id IS '操作人ID,关联sys_user表,为null表示系统操作';
COMMENT ON COLUMN sys_audit_log.operator_username IS '操作人用户名,冗余存储便于查询,避免频繁关联查询';

-- 请求信息字段注释
COMMENT ON COLUMN sys_audit_log.trace_id IS '链路追踪ID,32位十六进制字符串,用于关联整个请求链路的所有日志';
COMMENT ON COLUMN sys_audit_log.request_ip IS '客户端IP地址,支持反向代理(X-Forwarded-For),记录真实客户端IP';
COMMENT ON COLUMN sys_audit_log.user_agent IS '用户代理(User-Agent),记录客户端浏览器、操作系统等信息';

-- 执行结果字段注释
COMMENT ON COLUMN sys_audit_log.status IS '执行状态: SUCCESS(成功)、FAILURE(失败)';
COMMENT ON COLUMN sys_audit_log.execution_time IS '执行时间(毫秒),从方法开始到结束的耗时,用于性能监控';

-- 异常信息字段注释
COMMENT ON COLUMN sys_audit_log.exception_type IS '异常类型,如: BusinessException, NullPointerException, SQLException';
COMMENT ON COLUMN sys_audit_log.exception_message IS '异常消息,限制1000字符,避免过长影响存储';

-- 时间戳字段注释
COMMENT ON COLUMN sys_audit_log.create_time IS '创建时间,自动设置为当前时间且不可更新';

-- ====================================================================================
-- 数据保留策略
-- ====================================================================================
-- 保留期: 180天 (6个月)
-- 清理方式: 定时任务每天凌晨2点自动清理过期数据
-- 归档: 可选择性归档到离线存储
-- 相关配置: application-common.yml -> audit.log.retention.days
-- 相关任务: AuditLogCleanupTask.java
