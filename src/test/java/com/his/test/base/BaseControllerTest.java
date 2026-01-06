package com.his.test.base;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Controller层测试基类
 * <p>
 * 统一配置MockMvc、ObjectMapper，减少重复代码
 * <p>
 * 使用方式：
 * <pre>
 * {@code
 * @DisplayName("收费控制器测试")
 * class ChargeControllerTest extends BaseControllerTest {
 *     @MockBean
 *     private ChargeService chargeService;
 *     // mockMvc 和 objectMapper 自动注入
 * }
 * }
 * </pre>
 *
 * @author HIS Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    protected void setUp() throws Exception {
        // 子类可覆盖此方法进行额外设置
        setUpTest();
    }

    /**
     * 子类可覆盖的初始化方法
     * <p>
     * 用于子类添加特定的初始化逻辑
     *
     * @throws Exception 初始化异常
     */
    protected void setUpTest() throws Exception {
        // 默认空实现，子类可覆盖
    }

    /**
     * 将对象转换为JSON字节数组
     * <p>
     * 用于MockMvc的POST/PUT请求体
     *
     * @param object 要转换的对象
     * @return JSON字节数组
     * @throws Exception JSON转换异常
     */
    protected byte[] toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将对象转换为JSON字符串
     * <p>
     * 用于需要JSON字符串的场景
     *
     * @param object 要转换的对象
     * @return JSON字符串
     * @throws Exception JSON转换异常
     */
    protected String toJsonString(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}
