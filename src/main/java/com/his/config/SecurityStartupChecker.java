package com.his.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 安全启动检查器
 *
 * <p>在应用启动时检查环境配置，防止生产环境意外使用开发模式的不安全配置</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>环境检测</b>：检查当前激活的Profile（dev/test/prod）</li>
 *   <li><b>生产环境识别</b>：通过环境变量、系统属性、数据库配置等多维度识别生产环境</li>
 *   <li><b>配置冲突检测</b>：检测生产环境是否误用了dev profile</li>
 *   <li><b>安全警告</b>：dev环境输出警告日志，提醒不要部署到生产</li>
 *   <li><b>启动阻断</b>：检测到严重安全问题时抛出异常，阻止应用启动</li>
 * </ul>
 *
 * <h3>检测逻辑</h3>
 * <p><b>触发条件</b>：以下情况会抛出IllegalStateException，阻止应用启动：</p>
 * <ul>
 *   <li>当前Profile为dev（开发模式）</li>
     <li>且检测到生产环境特征（环境变量、系统属性、数据库URL等）</li>
 * </ul>
 *
 * <p><b>生产环境特征检测：</b></p>
 * <ol>
 *   <li>环境变量SPRING_PROFILES_ACTIVE为prod或production</li>
 *   <li>系统属性spring.profiles.active为prod或production</li>
 *   <li>数据库URL包含prod-db或production关键字</li>
 * </ol>
 *
 * <h3>配置项</h3>
 * <p><b>app.security.check-production</b>（默认true）</p>
 * <ul>
 *   <li>true：启用生产环境安全检查</li>
 *   <li>false：禁用检查（不推荐，仅用于特殊场景）</li>
 * </ul>
 *
 * <h3>警告信息示例</h3>
 * <pre>
 * ===============================================
 * 🔴 严重安全警告！
 * ===============================================
 * 检测到生产环境但使用了 dev profile！
 *
 * 当前配置: dev (开发模式)
 * 环境特征: 生产环境
 *
 * 这会导致：
 * 1. 接口可能暴露未授权访问
 * 2. 安全策略过于宽松
 * 3. 不符合生产安全要求
 * ...
 * ===============================================
 * </pre>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>启动时执行</b>：实现ApplicationRunner接口，在应用完全启动前执行检查</li>
 *   <li><b>快速失败</b>：检测到安全问题立即抛出异常，阻止应用继续启动</li>
 *   <li><b>可配置</b>：支持通过配置禁用检查（特殊场景需要）</li>
 *   <li><b>日志分级</b>：dev环境使用WARN提醒，生产冲突使用ERROR并抛出异常</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see org.springframework.boot.ApplicationRunner
 */
@Slf4j
@Component
public class SecurityStartupChecker implements ApplicationRunner {

    private final Environment environment;

    @Value("${app.security.check-production:true}")
    private boolean enableProductionCheck;

    public SecurityStartupChecker(Environment environment) {
        this.environment = environment;
    }

    /**
     * 应用启动时执行安全检查
     *
     * <p><b>检查流程：</b></p>
     * <ol>
     *   <li>检查是否启用生产环境检查（配置项：app.security.check-production）</li>
     *   <li>获取当前激活的Profile（dev/test/prod）</li>
     *   <li>检测是否为dev环境</li>
     *   <li>检测是否具有生产环境特征（调用detectProductionEnvironment方法）</li>
     *   <li>如果dev环境+生产特征：
     *     <ul>
     *       <li>输出ERROR日志（包含详细的安全警告和修复建议）</li>
     *       <li>抛出IllegalStateException，阻止应用启动</li>
     *     </ul>
     *   </li>
     *   <li>如果dev环境：
     *     <ul>
     *       <li>输出WARN日志（提醒不要部署到生产）</li>
     *       <li>应用正常启动</li>
     *     </ul>
     *   </li>
     *   <li>如果其他环境：
     *     <ul>
     *       <li>输出INFO日志（检查通过）</li>
     *       <li>应用正常启动</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param args 应用启动参数（包含命令行参数和配置项）
     * @throws Exception 当检测到严重安全问题时抛出IllegalStateException
     * @since 1.0
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enableProductionCheck) {
            log.info("生产环境安全检查已禁用");
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        // 检查是否在 dev 环境运行
        boolean isDevProfile = "dev".equals(currentProfile);

        // 检查是否在生产环境迹象（通过环境变量或系统属性）
        boolean isProductionEnvironment = detectProductionEnvironment();

        if (isDevProfile && isProductionEnvironment) {
            String errorMsg = """
                    ===============================================
                    🔴 严重安全警告！
                    ===============================================
                    检测到生产环境但使用了 dev profile！

                    当前配置: dev (开发模式)
                    环境特征: 生产环境

                    这会导致：
                    1. 接口可能暴露未授权访问
                    2. 安全策略过于宽松
                    3. 不符合生产安全要求

                    请立即修改 application.yml 中的 spring.profiles.active 为:
                    - test (测试环境) 或
                    - prod (生产环境)

                    应用即将停止运行...
                    ===============================================
                    """;

            log.error(errorMsg);
            throw new IllegalStateException(
                "生产环境不能使用 dev profile！请修改配置文件使用正确的环境配置。"
            );
        }

        if (isDevProfile) {
            log.warn("""
                ===============================================
                ⚠️  开发模式安全提醒
                ===============================================
                当前运行在: dev (开发模式)

                请注意：
                1. 此模式仅用于本地开发
                2. 不要将 dev 模式部署到生产环境
                3. 登录和 Swagger 接口无需认证
                4. 业务接口需要认证和角色验证

                如果需要测试完整的认证流程，请使用 test profile
                ===============================================
                """);
        } else {
            log.info("安全启动检查通过 - 当前环境: {}", currentProfile);
        }
    }

    /**
     * 检测是否在生产环境中运行
     *
     * <p>通过多维度检查识别生产环境，避免误判</p>
     *
     * <p><b>检查维度：</b></p>
     * <ol>
     *   <li><b>环境变量检查</b>：SPRING_PROFILES_ACTIVE是否为prod或production</li>
     *   <li><b>系统属性检查</b>：spring.profiles.active是否为prod或production</li>
     *   <li><b>数据库URL检查</b>：是否包含prod-db或production关键字</li>
     *   <li>任一维度匹配即判定为生产环境</li>
     * </ol>
     *
     * <p><b>设计要点：</b></p>
     * <ul>
     *   <li><b>多维度检查</b>：避免单一检查点误判</li>
     *   <li><b>OR逻辑</b>：任一检查点匹配即判定为生产环境（宁可误判，不可漏判）</li>
     *   <li><b>可扩展</b>：可根据需要增加更多检查维度（如服务器IP、域名等）</li>
     * </ul>
     *
     * @return true表示检测到生产环境特征，false表示未检测到
     * @since 1.0
     */
    private boolean detectProductionEnvironment() {
        // 检查环境变量
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if ("prod".equals(env) || "production".equals(env)) {
            return true;
        }

        // 检查系统属性
        String sysProp = System.getProperty("spring.profiles.active");
        if ("prod".equals(sysProp) || "production".equals(sysProp)) {
            return true;
        }

        // 检查常见的生产环境特征
        // 如果在生产数据库网络段
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && (dbUrl.contains("prod-db") || dbUrl.contains("production"))) {
            return true;
        }

        return false;
    }
}
