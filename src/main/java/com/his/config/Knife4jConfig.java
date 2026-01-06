package com.his.config;

import java.util.Arrays;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                                "### 📚 API分组说明\n\n" +
                                "#### 🎯 认证管理 (`/auth/*`)\n" +
                                "- `POST /auth/login` - 用户登录\n" +
                                "- `GET /auth/validate` - Token验证\n" +
                                "- `POST /auth/logout` - 用户登出\n\n" +
                                "#### 👨‍⚕️ 医生工作站 (`/api/doctor/*`)\n" +
                                "- `GET /api/doctor/waiting-list` - 查询候诊列表\n" +
                                "- `PUT /api/doctor/registrations/{id}/status` - 更新就诊状态\n" +
                                "- `GET /api/doctor/patients/{id}` - 查询患者详情\n" +
                                "- `POST /api/doctor/medical-records/save` - 保存/更新病历\n" +
                                "- `GET /api/doctor/medical-records/{id}` - 查询病历详情\n" +
                                "- `POST /api/doctor/prescriptions/create` - 创建处方\n" +
                                "- `POST /api/doctor/prescriptions/{id}/review` - 审核处方\n\n" +
                                "#### 👩‍⚕️ 护士工作站 (`/api/nurse/*`)\n" +
                                "- `POST /api/nurse/registrations` - 患者挂号\n" +
                                "- `GET /api/nurse/registrations/{id}` - 查询挂号记录\n" +
                                "- `PUT /api/nurse/registrations/{id}/cancel` - 取消挂号\n" +
                                "- `PUT /api/nurse/registrations/{id}/refund` - 挂号退费\n" +
                                "- `POST /api/nurse/registrations/today` - 查询今日挂号列表\n\n" +
                                "#### 💊 药师工作站 (`/api/pharmacist/*`)\n" +
                                "- `PUT /api/pharmacist/medicines/{id}/stock` - 更新药品库存\n" +
                                "- `GET /api/pharmacist/medicines/inventory-stats` - 库存统计\n" +
                                "- `GET /api/pharmacist/prescriptions/pending` - 待发药处方列表\n" +
                                "- `POST /api/pharmacist/prescriptions/{id}/review` - 审核处方\n" +
                                "- `POST /api/pharmacist/prescriptions/{id}/dispense` - 发药\n" +
                                "- `POST /api/pharmacist/prescriptions/{id}/return` - 退药\n" +
                                "- `GET /api/pharmacist/prescriptions/statistics/today` - 今日发药统计\n\n" +
                                "#### 💰 收费管理 (`/api/cashier/charges/*`)\n" +
                                "- `POST /api/cashier/charges` - 创建收费单\n" +
                                "- `GET /api/cashier/charges/{id}` - 查询收费单详情\n" +
                                "- `GET /api/cashier/charges` - 查询收费单列表\n" +
                                "- `POST /api/cashier/charges/{id}/pay` - 确认支付\n" +
                                "- `POST /api/cashier/charges/{id}/refund` - 处理退费\n" +
                                "- `POST /api/cashier/charges/registration/{id}` - 创建挂号收费单\n" +
                                "- `POST /api/cashier/charges/prescription` - 创建处方收费单\n" +
                                "- `GET /api/cashier/charges/registration/{id}/payment-status` - 检查挂号费支付状态\n" +
                                "- `GET /api/cashier/charges/registration/{id}/by-type` - 获取挂号单的所有收费记录\n" +
                                "- `GET /api/cashier/charges/statistics/daily` - 每日结算报表\n\n" +
                                "#### 🔗 公共接口 (`/api/common/*`)\n" +
                                "- `GET /api/common/data/departments` - 获取科室列表\n" +
                                "- `GET /api/common/data/doctors?deptId={id}` - 获取医生列表\n" +
                                "- `GET /api/common/medicines` - 查询药品列表（支持分页和多条件筛选）\n" +
                                "- `GET /api/common/medicines/{id}` - 查询药品详情\n" +
                                "- `GET /api/common/prescriptions/{id}` - 查询处方详情\n" +
                                "- `GET /api/common/prescriptions/by-record/{recordId}` - 查询病历的处方列表\n\n" +
                                "#### 📋 审计日志 (`/api/audit-logs/*`)\n" +
                                "- `GET /api/audit-logs/search` - 综合查询审计日志\n" +
                                "- `GET /api/audit-logs/trace/{traceId}` - 根据TraceId查询\n" +
                                "- `GET /api/audit-logs/operator/{operatorId}` - 查询操作人日志\n\n" +
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
                                "A: 开发环境不应出现此问题。如果出现，请检查SecurityConfig配置。\n\n" +
                                "**Q: 处方查询接口为什么在/common路径下？**\n\n" +
                                "A: 为了提高代码复用性，处方查询接口已统一迁移到 `/api/common/prescriptions`，所有角色都可以使用。\n\n" +
                                "**Q: 药品查询接口如何根据角色返回不同字段？**\n\n" +
                                "A: 系统使用 JsonView 机制，根据当前用户角色自动过滤敏感字段（如进货价仅药师可见）。")
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
