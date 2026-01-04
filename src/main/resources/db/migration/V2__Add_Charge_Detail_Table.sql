-- ================================================================================
-- HIS System - 收费明细表
-- ================================================================================
-- Flyway Version: V2
-- Description: 添加收费明细表，支持分阶段收费
-- Author: HIS Development Team
-- Date: 2025-12-27
-- ================================================================================

-- ============================================
-- 1. 创建收费明细表 (his_charge_detail)
-- ============================================
CREATE TABLE IF NOT EXISTS his_charge_detail (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    charge_main_id      BIGINT          NOT NULL,
    item_type           VARCHAR(20)     NOT NULL,
    item_id             BIGINT          NOT NULL,
    item_name           VARCHAR(100)    NOT NULL,
    item_amount         DECIMAL(10, 2)  NOT NULL,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),

    quantity            DECIMAL(10, 2)  DEFAULT 1,
    unit_price          DECIMAL(10, 4)  DEFAULT NULL,
    remarks             VARCHAR(500)    DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,

    CONSTRAINT fk_charge_detail_charge FOREIGN KEY (charge_main_id) REFERENCES his_charge(main_id)
);

COMMENT ON TABLE his_charge_detail IS '收费明细表';
COMMENT ON COLUMN his_charge_detail.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_charge_detail.charge_main_id IS '收费主表ID';
COMMENT ON COLUMN his_charge_detail.item_type IS '项目类型（REGISTRATION=挂号费, PRESCRIPTION=处方药费）';
COMMENT ON COLUMN his_charge_detail.item_id IS '项目关联ID（挂号ID或处方明细ID）';
COMMENT ON COLUMN his_charge_detail.item_name IS '项目名称';
COMMENT ON COLUMN his_charge_detail.item_amount IS '项目金额';
COMMENT ON COLUMN his_charge_detail.is_deleted IS '软删除标记（0=未删除, 1=已删除）';
COMMENT ON COLUMN his_charge_detail.created_at IS '创建时间';
COMMENT ON COLUMN his_charge_detail.quantity IS '数量';
COMMENT ON COLUMN his_charge_detail.unit_price IS '单价';
COMMENT ON COLUMN his_charge_detail.remarks IS '备注';
COMMENT ON COLUMN his_charge_detail.created_by IS '创建人ID';

-- ============================================
-- 2. 创建索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_his_charge_detail_charge_main_id
    ON his_charge_detail(charge_main_id)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_his_charge_detail_item_type_id
    ON his_charge_detail(item_type, item_id)
    WHERE is_deleted = 0;

COMMENT ON INDEX idx_his_charge_detail_charge_main_id IS '收费明细按收费主表ID索引';
COMMENT ON INDEX idx_his_charge_detail_item_type_id IS '收费明细按项目类型和ID索引（用于防止重复收费）';

-- ============================================
-- 3. 更新现有表注释
-- ============================================
COMMENT ON COLUMN his_prescription.status IS '状态（0=草稿, 1=已开方, 2=已审核, 3=已发药, 4=已退费, 5=已缴费）';
