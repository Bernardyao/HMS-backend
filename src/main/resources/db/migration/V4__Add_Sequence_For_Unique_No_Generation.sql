-- ================================================================================
-- V4: 唯一编号生成优化 - 数据库序列方案
-- ================================================================================
-- 目的: 解决并发场景下编号重复问题
-- 问题: generateChargeNo()使用秒级时间戳+随机数，高频调用时重复
-- 方案: 使用PostgreSQL序列+函数，保证线程安全和唯一性
--
-- 编号格式:
--   ChargeNo:       CHG + yyyyMMdd + 6位序列号 (例: CHG20260103000001)
--   PrescriptionNo: PRE + yyyyMMdd + 6位序列号
--   RegNo:          R   + yyyyMMdd + 4位序列号
--   PatientNo:      P   + yyyyMMdd + 4位序列号
--
-- 作者: HIS开发团队
-- 日期: 2026-01-03
-- ================================================================================

-- ================================================================================
-- 1. 生成收费单号函数
-- ================================================================================

/**
 * 生成收费单号
 *
 * 功能说明:
 *   - 使用日期+序列号保证唯一性
 *   - 每日自动创建新序列，序列号从1开始
 *   - 格式: CHG + yyyyMMdd + 6位序列号
 *   - 示例: CHG20260103000001
 *
 * 并发安全:
 *   - PostgreSQL序列原子性保证
 *   - 高并发场景下无冲突
 *
 * @return 唯一的收费单号（20字符）
 */
CREATE OR REPLACE FUNCTION generate_charge_no()
RETURNS VARCHAR(20) AS $$
DECLARE
    date_prefix TEXT;
    seq_name TEXT;
    seq_value BIGINT;
    result_no VARCHAR(20);
BEGIN
    -- 获取日期前缀（yyyyMMdd）
    date_prefix := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    seq_name := 'seq_charge_no_' || date_prefix;

    -- 检查序列是否存在，不存在则创建
    IF NOT EXISTS (
        SELECT 1
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename = seq_name
    ) THEN
        EXECUTE format('CREATE SEQUENCE %s START WITH 1', seq_name);
    END IF;

    -- 获取序列值并格式化
    EXECUTE format('SELECT nextval(%L)', seq_name) INTO seq_value;
    result_no := 'CHG' || date_prefix || LPAD(seq_value::TEXT, 6, '0');

    RETURN result_no;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generate_charge_no() IS '生成收费单号（线程安全，每日重置）';

-- ================================================================================
-- 2. 生成处方号函数
-- ================================================================================

/**
 * 生成处方号
 *
 * @return 唯一的处方号（20字符）
 */
CREATE OR REPLACE FUNCTION generate_prescription_no()
RETURNS VARCHAR(20) AS $$
DECLARE
    date_prefix TEXT;
    seq_name TEXT;
    seq_value BIGINT;
    result_no VARCHAR(20);
BEGIN
    date_prefix := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    seq_name := 'seq_prescription_no_' || date_prefix;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename = seq_name
    ) THEN
        EXECUTE format('CREATE SEQUENCE %s START WITH 1', seq_name);
    END IF;

    EXECUTE format('SELECT nextval(%L)', seq_name) INTO seq_value;
    result_no := 'PRE' || date_prefix || LPAD(seq_value::TEXT, 6, '0');

    RETURN result_no;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generate_prescription_no() IS '生成处方号（线程安全，每日重置）';

-- ================================================================================
-- 3. 生成挂号流水号函数
-- ================================================================================

/**
 * 生成挂号流水号
 *
 * @return 唯一的挂号流水号（15字符）
 */
CREATE OR REPLACE FUNCTION generate_reg_no()
RETURNS VARCHAR(15) AS $$
DECLARE
    date_prefix TEXT;
    seq_name TEXT;
    seq_value BIGINT;
    result_no VARCHAR(15);
BEGIN
    date_prefix := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    seq_name := 'seq_reg_no_' || date_prefix;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename = seq_name
    ) THEN
        EXECUTE format('CREATE SEQUENCE %s START WITH 1', seq_name);
    END IF;

    EXECUTE format('SELECT nextval(%L)', seq_name) INTO seq_value;
    result_no := 'R' || date_prefix || LPAD(seq_value::TEXT, 4, '0');

    RETURN result_no;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generate_reg_no() IS '生成挂号流水号（线程安全，每日重置）';

-- ================================================================================
-- 4. 生成患者病历号函数
-- ================================================================================

/**
 * 生成患者病历号
 *
 * @return 唯一的患者病历号（15字符）
 */
CREATE OR REPLACE FUNCTION generate_patient_no()
RETURNS VARCHAR(15) AS $$
DECLARE
    date_prefix TEXT;
    seq_name TEXT;
    seq_value BIGINT;
    result_no VARCHAR(15);
BEGIN
    date_prefix := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    seq_name := 'seq_patient_no_' || date_prefix;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename = seq_name
    ) THEN
        EXECUTE format('CREATE SEQUENCE %s START WITH 1', seq_name);
    END IF;

    EXECUTE format('SELECT nextval(%L)', seq_name) INTO seq_value;
    result_no := 'P' || date_prefix || LPAD(seq_value::TEXT, 4, '0');

    RETURN result_no;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generate_patient_no() IS '生成患者病历号（线程安全，每日重置）';

-- ================================================================================
-- 5. 定期清理历史序列的函数（可选）
-- ================================================================================

/**
 * 清理N天前的序列（释放数据库资源）
 *
 * 说明:
 *   - 序列会随着时间累积（每天创建新序列）
 *   - 建议每月执行一次清理，保留30天的序列
 *   - 可通过定时任务调用此函数
 *
 * @param days_to_keep 保留天数（默认30天）
 * @return 删除的序列数量
 */
CREATE OR REPLACE FUNCTION cleanup_old_sequences(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    seq_record RECORD;
    drop_count INTEGER := 0;
    cutoff_date DATE;
    seq_date DATE;
BEGIN
    cutoff_date := CURRENT_DATE - days_to_keep;

    FOR seq_record IN
        SELECT sequencename
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename LIKE 'seq_%_no_%'
    LOOP
        -- 从序列名提取日期（格式: seq_charge_no_20260103）
        BEGIN
            seq_date := TO_DATE(
                SUBSTRING(seq_record.sequencename FROM '_\d{8}$'),
                'YYYYMMDD'
            );

            IF seq_date < cutoff_date THEN
                EXECUTE format('DROP SEQUENCE IF EXISTS %s', seq_record.sequencename);
                drop_count := drop_count + 1;
                RAISE NOTICE '已删除序列: %', seq_record.sequencename;
            END IF;
        EXCEPTION WHEN OTHERS THEN
            -- 忽略日期解析错误（不符合命名格式的序列）
            CONTINUE;
        END;
    END LOOP;

    RETURN drop_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_sequences(INTEGER) IS '清理N天前的编号序列（建议每月执行一次）';

-- ================================================================================
-- 6. 回滚脚本（仅在需要回滚时使用）
-- ================================================================================
/*
-- 删除函数
DROP FUNCTION IF EXISTS generate_charge_no();
DROP FUNCTION IF EXISTS generate_prescription_no();
DROP FUNCTION IF EXISTS generate_reg_no();
DROP FUNCTION IF EXISTS generate_patient_no();
DROP FUNCTION IF EXISTS cleanup_old_sequences(INTEGER);

-- 删除所有相关序列（可选，也可以保留）
DO $$
DECLARE
    seq_name TEXT;
BEGIN
    FOR seq_name IN
        SELECT sequencename
        FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename LIKE 'seq_%_no_%'
    LOOP
        EXECUTE format('DROP SEQUENCE IF EXISTS %s', seq_name);
        RAISE NOTICE '已删除序列: %', seq_name;
    END LOOP;
END $$;
*/

-- ================================================================================
-- 验证脚本（测试用）
-- ================================================================================
/*
-- 测试函数是否正常工作
SELECT generate_charge_no() AS charge_no;
SELECT generate_prescription_no() AS prescription_no;
SELECT generate_reg_no() AS reg_no;
SELECT generate_patient_no() AS patient_no;

-- 验证生成的编号格式
SELECT
    generate_charge_no() AS charge_no,
    LENGTH(generate_charge_no()) AS length,
    generate_charge_no() ~ '^CHG\d{14}$' AS format_valid;

-- 查看所有创建的序列
SELECT schemaname, sequencename
FROM pg_sequences
WHERE schemaname = 'public'
  AND sequencename LIKE 'seq_%_no_%'
ORDER BY sequencename;
*/

-- ================================================================================
-- 迁移完成
-- ================================================================================
-- 执行后请验证:
--   1. 函数创建成功
--   2. 生成的编号格式正确
--   3. 多次调用编号递增且唯一
--   4. 跨日期边界序列正确重置
-- ================================================================================
