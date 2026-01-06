package com.his.testutils;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * 测试数据清理工具类
 * <p>
 * 在每个测试方法执行后清理数据库，确保测试隔离
 * <p>
 * 清理策略：
 * <ul>
 *     <li>按外键依赖顺序清理表（明细表→主表）</li>
 *     <li>临时禁用PostgreSQL外键约束</li>
 *     <li>使用TRUNCATE TABLE快速清空数据</li>
 * </ul>
 *
 * @author HIS Team
 * @since 1.0.0
 */
public class DatabaseCleaner extends AbstractTestExecutionListener {

    private JdbcTemplate jdbcTemplate;

    /**
     * 表清理顺序
     * <p>
     * 按照外键依赖关系从子表到父表清理，避免外键约束错误
     */
    private static final List<String> TABLES_IN_ORDER = List.of(
            "his_charge_detail",           // 收费明细表（子表）
            "his_charge",                   // 收费主表
            "his_prescription_medicine",   // 处方药品明细表（子表）
            "his_prescription",            // 处方主表
            "his_medical_record",          // 病历表
            "his_registration",            // 挂号表
            "his_doctor",                  // 医生表
            "his_department",              // 科室表
            "his_patient",                 // 患者表
            "his_user",                    // 用户表
            "his_medicine"                 // 药品表
    );

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        // 初始化JdbcTemplate
        initJdbcTemplate(testContext);

        // 测试方法前的清理（可选，通常由@Transactional回滚处理）
        // cleanDatabase();
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // 初始化JdbcTemplate
        initJdbcTemplate(testContext);

        // 在测试方法后清理数据
        cleanDatabase();
    }

    /**
     * 初始化JdbcTemplate
     *
     * @param testContext 测试上下文
     */
    private void initJdbcTemplate(TestContext testContext) {
        if (jdbcTemplate == null) {
            try {
                jdbcTemplate = testContext.getApplicationContext()
                        .getBean(JdbcTemplate.class);
            } catch (Exception e) {
                // 如果无法获取JdbcTemplate，静默失败
                // 这可能发生在纯单元测试（不使用Spring上下文）中
            }
        }
    }

    /**
     * 清理所有测试数据
     * <p>
     * 执行步骤：
     * <ol>
     *     <li>禁用PostgreSQL外键约束</li>
     *     <li>按顺序清空所有表</li>
     *     <li>重新启用外键约束</li>
     * </ol>
     */
    public void cleanDatabase() {
        if (jdbcTemplate == null) {
            return;
        }

        try {
            // 禁用外键约束（PostgreSQL）
            jdbcTemplate.execute("SET session_replication_role = 'replica'");

            // 清空所有表
            for (String table : TABLES_IN_ORDER) {
                try {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
                } catch (Exception e) {
                    // 表可能不存在，继续清理下一个表
                    System.err.println("Warning: Failed to truncate table " + table + ": " + e.getMessage());
                }
            }
        } finally {
            // 重新启用外键约束
            try {
                jdbcTemplate.execute("SET session_replication_role = 'origin'");
            } catch (Exception e) {
                System.err.println("Warning: Failed to re-enable foreign key constraints: " + e.getMessage());
            }
        }
    }

    /**
     * 重置所有序列（PostgreSQL）
     * <p>
     * 将所有序列重置为1，确保测试ID的一致性
     */
    public void resetSequences() {
        if (jdbcTemplate == null) {
            return;
        }

        try {
            // 查询所有序列
            List<String> sequences = jdbcTemplate.queryForList(
                    "SELECT sequence_name FROM information_schema.sequences " +
                            "WHERE sequence_schema = 'public'",
                    String.class
            );

            // 重置每个序列
            for (String sequence : sequences) {
                try {
                    jdbcTemplate.execute("ALTER SEQUENCE " + sequence + " RESTART WITH 1");
                } catch (Exception e) {
                    System.err.println("Warning: Failed to reset sequence " + sequence + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to reset sequences: " + e.getMessage());
        }
    }

    /**
     * 清理指定表
     * <p>
     * 用于需要手动清理特定表的场景
     *
     * @param tableName 表名
     */
    public void cleanTable(String tableName) {
        if (jdbcTemplate == null) {
            return;
        }

        try {
            // 禁用外键约束
            jdbcTemplate.execute("SET session_replication_role = 'replica'");

            // 清空指定表
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName + " CASCADE");
        } finally {
            // 重新启用外键约束
            try {
                jdbcTemplate.execute("SET session_replication_role = 'origin'");
            } catch (Exception e) {
                System.err.println("Warning: Failed to re-enable foreign key constraints: " + e.getMessage());
            }
        }
    }
}
