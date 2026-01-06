package com.his.test.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Service层测试基类
 * <p>
 * 统一配置MockitoExtension和测试环境，标准化Mockito使用
 * <p>
 * 使用方式：
 * <pre>
 * {@code
 * @DisplayName("收费服务测试")
 * class ChargeServiceImplTest extends BaseServiceTest {
 *     @Mock(lenient = true)
 *     private ChargeRepository chargeRepository;
 *
 *     @InjectMocks
 *     private ChargeServiceImpl chargeService;
 *     // 自动继承 @ExtendWith 和 @ActiveProfiles
 * }
 * }
 * </pre>
 *
 * @author HIS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public abstract class BaseServiceTest {

    @BeforeEach
    void setUp() throws Exception {
        // 子类可覆盖此方法进行额外设置
        setUpTest();
    }

    /**
     * 子类可覆盖的初始化方法
     * <p>
     * 用于子类添加特定的初始化逻辑，如Mock对象的行为设置
     *
     * @throws Exception 初始化异常
     */
    protected void setUpTest() throws Exception {
        // 默认空实现，子类可覆盖
    }

    /**
     * 子类可覆盖的清理方法
     * <p>
     * 用于子类在测试后进行资源清理
     *
     * @throws Exception 清理异常
     */
    protected void tearDown() throws Exception {
        // 默认空实现，子类可覆盖
    }
}
