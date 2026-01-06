package com.his.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * dev 环境安全配置：启用宽松的安全策略用于开发调试。
 *
 * <p><b>重要说明：</b>
 * <ul>
 *   <li>{@code @EnableMethodSecurity(prePostEnabled = true)} - 启用方法级别安全检查</li>
 *   <li>保留 @PreAuthorize 注解的验证，确保安全代码在开发环境也生效</li>
 *   <li>登录和 Swagger 接口无需认证，方便开发调试</li>
 *   <li>业务接口需要认证，但可以使用测试账号进行开发</li>
 *   <li>test/prod 环境使用更严格的 {@code SecurityConfig}</li>
 * </ul>
 *
 * <p><b>安全警告：</b>
 * <ul>
 *   <li>此配置仅用于本地开发，绝不能用于生产环境</li>
 *   <li>生产环境会自动使用 {@code SecurityConfig}（@Profile("!dev")）</li>
 * </ul>
 */
@Slf4j
@Configuration
@Profile("dev")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class DevSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                    log.warn("⚠️  开发模式：登录和 Swagger 无需认证，业务接口需要认证");
                    // 登录接口：开放
                    auth.requestMatchers("/auth/**").permitAll();
                    // Swagger/Knife4j：开放（开发环境）
                    auth.requestMatchers(
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico"
                    ).permitAll();
                    // 其余接口：需要认证（保留 @PreAuthorize 验证）
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
