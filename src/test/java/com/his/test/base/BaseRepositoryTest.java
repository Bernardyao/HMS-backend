package com.his.test.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository层测试基类
 * <p>
 * 统一配置数据库测试环境，使用@DataJpaTest进行切片测试
 * <p>
 * 使用方式：
 * <pre>
 * {@code
 * @DisplayName("Repository层集成测试")
 * class PatientRepositoryTest extends BaseRepositoryTest {
 *     @Autowired
 *     private PatientRepository patientRepository;
 *     // 自动继承所有数据库测试配置
 * }
 * }
 * </pre>
 *
 * @author HIS Team
 * @since 1.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class BaseRepositoryTest {

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
}
