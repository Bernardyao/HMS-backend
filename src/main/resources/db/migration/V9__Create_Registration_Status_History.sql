-- ================================================================================
-- HIS System - 挂号状态转换审计表
-- ================================================================================
-- Flyway Version: V9
-- Description: 创建挂号状态转换历史审计表，用于记录所有状态变更
-- Author: HIS Development Team
-- Date: 2026-01-06
-- ================================================================================

-- ============================================
-- 挂号状态转换历史表 (his_registration_status_history)
-- ============================================
CREATE TABLE his_registration_status_history (
    main_id                 BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_main_id    BIGINT          NOT NULL,
    from_status             SMALLINT        NOT NULL,
    to_status               SMALLINT        NOT NULL,
    operator_id             BIGINT          DEFAULT NULL,
    operator_name           VARCHAR(50)     DEFAULT NULL,
    operator_type           VARCHAR(20)     DEFAULT NULL, -- 'SYSTEM', 'USER'
    reason                  VARCHAR(500)    DEFAULT NULL,
    created_at              TIMESTAMP       DEFAULT now(),
    updated_at              TIMESTAMP       DEFAULT now(),

    CONSTRAINT fk_status_history_registration FOREIGN KEY (registration_main_id)
        REFERENCES his_registration(main_id) ON DELETE CASCADE
);

COMMENT ON TABLE his_registration_status_history IS '挂号状态转换历史审计表';
COMMENT ON COLUMN his_registration_status_history.main_id IS '主键ID';
COMMENT ON COLUMN his_registration_status_history.registration_main_id IS '挂号记录ID';
COMMENT ON COLUMN his_registration_status_history.from_status IS '源状态码';
COMMENT ON COLUMN his_registration_status_history.to_status IS '目标状态码';
COMMENT ON COLUMN his_registration_status_history.operator_id IS '操作人ID';
COMMENT ON COLUMN his_registration_status_history.operator_name IS '操作人姓名';
COMMENT ON COLUMN his_registration_status_history.operator_type IS '操作类型（SYSTEM=系统自动, USER=用户手动）';
COMMENT ON COLUMN his_registration_status_history.reason IS '状态转换原因';
COMMENT ON COLUMN his_registration_status_history.created_at IS '转换时间';
COMMENT ON COLUMN his_registration_status_history.updated_at IS '更新时间';

-- 创建索引
CREATE INDEX idx_status_history_registration ON his_registration_status_history (registration_main_id);
CREATE INDEX idx_status_history_created_at ON his_registration_status_history (created_at);
CREATE INDEX idx_status_history_from_status ON his_registration_status_history (from_status);
CREATE INDEX idx_status_history_to_status ON his_registration_status_history (to_status);
CREATE INDEX idx_status_history_operator ON his_registration_status_history (operator_id);

-- 创建更新时间触发器
CREATE TRIGGER t_his_registration_status_history_updated_at
    BEFORE INSERT ON his_registration_status_history
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

-- 插入初始说明数据
COMMENT ON TABLE his_registration_status_history IS '
状态码说明：
0 = WAITING (待就诊)
1 = COMPLETED (已就诊)
2 = CANCELLED (已取消)
3 = REFUNDED (已退费)
4 = PAID_REGISTRATION (已缴挂号费)
5 = IN_CONSULTATION (就诊中)

使用示例：
SELECT * FROM his_registration_status_history WHERE registration_main_id = 123
ORDER BY created_at DESC;
';
