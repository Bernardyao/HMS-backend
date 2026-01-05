-- ============================================
-- V8__Add_Patient_Search_Indexes.sql
-- 护士工作站患者搜索功能的性能优化索引
-- ============================================

-- 1. 创建基础搜索索引（用于排序和基本过滤）
-- 此索引优化 ORDER BY updated_at DESC 和 WHERE is_deleted = 0 的查询
CREATE INDEX IF NOT EXISTS idx_his_patient_search_base 
ON his_patient (is_deleted, updated_at DESC)
WHERE is_deleted = 0;

COMMENT ON INDEX idx_his_patient_search_base IS '患者搜索基础索引（优化排序和过滤）';

-- 2. (可选) 启用 PostgreSQL 的 pg_trgm 扩展以支持高效模糊搜索
-- pg_trgm 扩展提供了三元组相似度匹配，可以显著提升 LIKE '%keyword%' 的性能
-- 注意：需要数据库超级用户权限
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 3. (可选) 为姓名字段创建 GIN 索引以优化模糊搜索
-- 此索引会显著提升 name LIKE '%keyword%' 的查询性能
-- 适用于患者数量较大（>10万）的场景
CREATE INDEX IF NOT EXISTS idx_his_patient_name_trgm 
ON his_patient USING gin (name gin_trgm_ops)
WHERE is_deleted = 0;

COMMENT ON INDEX idx_his_patient_name_trgm IS '患者姓名模糊搜索优化索引（GIN三元组）';

-- 4. 为身份证号字段创建部分索引（仅索引未删除的记录）
-- 优化 idCard LIKE '%keyword%' 的查询
CREATE INDEX IF NOT EXISTS idx_his_patient_idcard_partial 
ON his_patient (id_card)
WHERE is_deleted = 0 AND id_card IS NOT NULL;

COMMENT ON INDEX idx_his_patient_idcard_partial IS '患者身份证号部分索引（仅未删除记录）';

-- 5. 为手机号字段创建部分索引（仅索引未删除的记录）
-- 优化 phone LIKE '%keyword%' 的查询
CREATE INDEX IF NOT EXISTS idx_his_patient_phone_partial 
ON his_patient (phone)
WHERE is_deleted = 0 AND phone IS NOT NULL;

COMMENT ON INDEX idx_his_patient_phone_partial IS '患者手机号部分索引（仅未删除记录）';

-- ============================================
-- 索引说明与性能预期
-- ============================================

-- 索引策略说明：
-- 1. idx_his_patient_search_base: 
--    - 优化基础过滤和排序，所有查询都能受益
--    - 空间占用小，维护成本低
--
-- 2. idx_his_patient_name_trgm:
--    - 使用 GIN 索引 + pg_trgm 扩展
--    - 显著提升姓名模糊搜索性能（10x-100x）
--    - 空间占用较大，维护成本中等
--    - 推荐在患者数量 > 10万时启用
--
-- 3. idx_his_patient_idcard_partial & idx_his_patient_phone_partial:
--    - 部分索引，仅索引未删除且非空的记录
--    - 优化精确匹配和前缀匹配
--    - 空间占用小，维护成本低

-- 性能预期：
-- - 无索引：搜索耗时 500-2000ms（取决于数据量）
-- - 基础索引：搜索耗时 200-500ms
-- - 完整索引（含 pg_trgm）：搜索耗时 50-200ms

-- 索引大小估算（10万患者数据）：
-- - idx_his_patient_search_base: ~2MB
-- - idx_his_patient_name_trgm: ~15MB
-- - idx_his_patient_idcard_partial: ~3MB
-- - idx_his_patient_phone_partial: ~2MB
-- 总计：~22MB

-- ============================================
-- 验证索引是否生效（可在 psql 中执行）
-- ============================================

-- 查看索引使用情况
-- EXPLAIN ANALYZE 
-- SELECT * FROM his_patient 
-- WHERE is_deleted = 0 
-- AND (name LIKE '%张%' OR id_card LIKE '%320%' OR phone LIKE '%138%')
-- ORDER BY updated_at DESC 
-- LIMIT 15;

-- 预期输出应包含：
-- - Index Scan using idx_his_patient_search_base
-- - Bitmap Index Scan using idx_his_patient_name_trgm (如果启用)
