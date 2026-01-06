package com.his.test.base;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.his.repository.AuditLogRepository;

/**
 * 审计日志测试基类
 *
 * <p>为所有审计日志相关测试提供公共功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>环境配置</b>：使用test环境配置</li>
 *   <li><b>自动清理</b>：每个测试方法执行后自动清理审计日志数据</li>
 *   <li><b>依赖注入</b>：自动注入AuditLogRepository供子类使用</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * class MyAuditLogTest extends BaseAuditLogTest {
 *     @Test
 *     void testSomething() {
 *         // 可以直接使用 auditLogRepository
 *         AuditLogEntity entity = new AuditLogEntity();
 *         entity.setModule("测试模块");
 *         auditLogRepository.save(entity);
 *     }
 *
 *     // 测试结束后自动清理
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseAuditLogTest {

    /**
     * 审计日志数据访问接口
     *
     * <p>子类可以直接使用此repository进行测试数据准备和验证</p>
     */
    @Autowired
    protected AuditLogRepository auditLogRepository;

    /**
     * 清理测试数据
     *
     * <p>每个测试方法执行后自动清理所有审计日志数据</p>
     * <p>确保测试之间相互独立,避免数据污染</p>
     */
    @AfterEach
    void cleanUp() {
        auditLogRepository.deleteAll();
    }
}
