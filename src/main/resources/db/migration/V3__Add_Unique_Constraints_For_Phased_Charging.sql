-- ================================================================================
-- 分阶段收费功能 - 数据库唯一约束和索引
-- 目的：防止并发情况下的重复挂号和数据不一致
-- ================================================================================

-- ================================================================================
-- 1. 挂号表唯一索引 - 防止重复挂号
-- ================================================================================
-- 说明：防止同一患者同一天同一医生重复挂号（待就诊状态）
CREATE UNIQUE INDEX uk_his_registration_patient_doctor_date_status
ON his_registration(patient_main_id, doctor_main_id, visit_date, status)
WHERE is_deleted = 0 AND status = 0;

COMMENT ON INDEX uk_his_registration_patient_doctor_date_status IS '防止重复挂号（同一天同一医生同一患者不能重复待就诊挂号）';

-- ================================================================================
-- 2. 收费表唯一索引 - 交易流水号唯一性
-- ================================================================================
-- 说明：确保交易流水号全局唯一，防止重复支付
CREATE UNIQUE INDEX uk_his_charge_transaction_no
ON his_charge(transaction_no)
WHERE is_deleted = 0 AND transaction_no IS NOT NULL AND transaction_no != '';

COMMENT ON INDEX uk_his_charge_transaction_no IS '交易流水号唯一性约束（防止重复支付）';

-- ================================================================================
-- 3. 收费表复合索引 - 优化挂号收费查询
-- ================================================================================
-- 说明：优化根据挂号ID和收费类型查询收费记录的性能
-- 用途：分阶段收费场景下频繁查询
-- 注意：收费明细表索引已在V2中创建，此处跳过

CREATE INDEX IF NOT EXISTS idx_his_charge_registration_type
ON his_charge(registration_main_id, charge_type, status)
WHERE is_deleted = 0;

COMMENT ON INDEX idx_his_charge_registration_type IS '挂号收费查询索引（分阶段收费优化）';

-- ================================================================================
-- 回滚脚本（如需回滚，执行以下SQL）
-- ================================================================================
/*
-- 删除索引
DROP INDEX IF EXISTS uk_his_registration_patient_doctor_date_status;
DROP INDEX IF EXISTS uk_his_charge_transaction_no;
DROP INDEX IF EXISTS idx_his_charge_detail_item_type_id;
DROP INDEX IF EXISTS idx_his_charge_registration_type;

-- 注意：回滚前请评估对系统性能和数据一致性的影响
*/
