package com.his.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Knife4j (Swagger) 配置类
 * 
 * 功能：
 * 1. 配置 API 文档的基本信息
 * 2. 配置 JWT 认证方式
 * 3. 环境感知：只在允许的环境下启用
 * 
 * 开发环境说明：
 * - dev环境下所有接口完全开放，无需认证
 * - 用户可直接在Swagger UI中测试所有API
 * - 如需测试需要认证的功能，可先调用登录接口获取token，然后点击Authorize按钮添加token
 * 
 * 访问地址：http://localhost:8080/doc.html
 * 
 * @author HIS Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.swagger",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false  // 如果未配置，默认不加载
)
public class Knife4jConfig {
    
    private final Environment environment;

    private static final String SECURITY_SCHEME_NAME = "JWT";
    
    @PostConstruct
    public void init() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║       Knife4j (Swagger) 已启用                          ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ 当前环境: {}", Arrays.toString(activeProfiles));
        log.info("║ 开发模式: 所有接口完全开放");
        log.info("║ 访问地址: http://localhost:8080/doc.html");
        log.info("╚════════════════════════════════════════════════════════╝");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HIS 医院信息管理系统 API 文档")
                        .version("1.0.0")
                        .description("## 🏥 HIS 医院信息管理系统\n\n" +
                                "本系统采用JWT认证机制，开发环境下所有接口完全开放供测试。\n\n" +
                                "---\n\n" +
                                "### 🔐 如何使用API文档\n\n" +
                                "#### 开发环境（当前）\n" +
                                "- 所有接口完全开放，无需认证即可直接测试\n" +
                                "- 如需测试认证相关功能，可按以下步骤操作\n\n" +
                                "#### 认证测试步骤（可选）\n" +
                                "1. 调用【认证管理】->【用户登录】接口 `POST /auth/login`\n" +
                                "2. 复制返回的 `token` 字段\n" +
                                "3. 点击右上角【Authorize】按钮\n" +
                                "4. 在弹出框中输入: `Bearer 你的token`\n" +
                                "5. 点击【Authorize】完成认证\n\n" +
                                "**注意**：Token 有效期 24 小时，过期后需要重新登录。\n\n" +
                                "---\n\n" +
                                "### 👥 测试账号\n\n" +
                                "| 角色 | 用户名 | 密码 | 说明 |\n" +
                                "|------|--------|------|------|\n" +
                                "| 🔱 管理员 | admin | admin123 | 系统所有功能 |\n" +
                                "| 👨‍⚕️ 医生 | doctor001 | admin123 | 医生工作站、药品查询 |\n" +
                                "| 👩‍⚕️ 护士 | nurse001 | admin123 | 挂号、患者管理 |\n" +
                                "| 💊 药师 | pharmacist001 | admin123 | 药品管理、处方审核 |\n" +
                                "| 💰 收费员 | cashier001 | admin123 | 收费、退费 |\n\n" +
                                "---\n\n" +
                                "### ⚙️ 环境信息\n\n" +
                                "- **当前环境**：" + String.join(", ", environment.getActiveProfiles()) + "\n" +
                                "- **认证方式**：JWT (JSON Web Token)\n" +
                                "- **Token位置**：HTTP Header: `Authorization: Bearer <token>`\n" +
                                "- **开发模式**：所有接口完全开放\n\n" +
                                "---\n\n" +
                                "### 📝 常见问题\n\n" +
                                "**Q: 如何测试需要认证的接口？**\n\n" +
                                "A: 开发环境下所有接口都可以直接访问。如需测试JWT认证，可以先登录获取token，然后使用Authorize功能添加token。\n\n" +
                                "**Q: 接口返回403 Forbidden？**\n\n" +
                                "A: 开发环境不应出现此问题。如果出现，请检查SecurityConfig配置。")
                        .contact(new Contact()
                                .name("HIS 开发团队")
                                .email("his-dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
                // 配置JWT安全方案
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT认证令牌：Authorization: Bearer <token>")
                        )
                )
                // 全局应用JWT安全方案
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
