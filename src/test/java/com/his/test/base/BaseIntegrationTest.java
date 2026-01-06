package com.his.test.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.his.testutils.DatabaseCleaner;

/**
 * 集成测试基类
 * <p>
 * 统一配置事务管理和数据清理，确保测试隔离
 * <p>
 * 使用方式：
 * <pre>
 * {@code
 * @DisplayName("收费流程集成测试")
 * class ChargeIntegrationTest extends BaseIntegrationTest {
 *     @Autowired
 *     private ChargeService chargeService;
 *     // 自动继承 @Transactional 和 DatabaseCleaner
 * }
 * }
 * </pre>
 *
 * @author HIS Team
 * @since 1.0.0
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
@TestExecutionListeners(
        listeners = {
                DatabaseCleaner.class,
                DependencyInjectionTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public abstract class BaseIntegrationTest {

    @Autowired(required = false)
    protected DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() throws Exception {
        // 子类可覆盖此方法进行额外设置
        setUpTest();
    }

    /**
     * 子类可覆盖的初始化方法
     * <p>
     * 用于子类添加特定的初始化逻辑，如测试数据准备
     *
     * @throws Exception 初始化异常
     */
    protected void setUpTest() throws Exception {
        // 默认空实现，子类可覆盖
    }

    /**
     * 手动清理数据库
     * <p>
     * 对于非@Transactional的测试方法，可以手动调用此方法清理数据
     * <p>
     * 注意：通常情况下，@Transactional会自动回滚，无需手动清理
     */
    protected void cleanDatabase() {
        if (databaseCleaner != null) {
            databaseCleaner.cleanDatabase();
        }
    }
}
