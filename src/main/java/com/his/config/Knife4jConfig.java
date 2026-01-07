package com.his.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

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
 * Knife4j (Swagger) 配置类.
 *
 * <p>访问地址：http://localhost:{server.port}/doc.html</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.swagger",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class Knife4jConfig {

    private final Environment environment;

    private static final String SECURITY_SCHEME_NAME = "JWT";
    private static final String DESCRIPTION_FILE = "swagger/api-description.md";

    @Value("${server.port:8080}")
    private int serverPort;

    @PostConstruct
    public void init() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("Knife4j enabled | Profiles: {} | URL: http://localhost:{}/doc.html",
                Arrays.toString(activeProfiles), serverPort);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HIS 医院信息管理系统 API")
                        .version("1.0.0")
                        .description(loadDescription())
                        .contact(new Contact()
                                .name("HIS Team")
                                .email("his-dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Authorization: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private String loadDescription() {
        try {
            ClassPathResource resource = new ClassPathResource(DESCRIPTION_FILE);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to load {}, using default", DESCRIPTION_FILE);
            return "HIS API Documentation";
        }
    }
}
