package com.his.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Arrays;

/**
 * Spring Security 生产环境安全配置类
 *
 * <p>配置生产环境和测试环境的安全策略，包括JWT认证、方法级权限控制、CORS跨域等</p>
 *
 * <h3>配置内容</h3>
 * <ul>
 *   <li><b>JWT认证过滤器</b>：拦截所有请求，从请求头中提取和验证JWT令牌</li>
 *   <li><b>权限控制规则</b>：定义哪些接口需要认证，哪些接口完全开放</li>
 *   <li><b>CORS跨域配置</b>：允许跨域请求，支持前后端分离架构</li>
 *   <li><b>会话管理</b>：使用无状态策略，依赖JWT而非Session</li>
 *   <li><b>方法级权限</b>：启用@PreAuthorize和@PostAuthorize注解支持</li>
 * </ul>
 *
 * <h3>生效条件</h3>
 * <p>此配置仅在<b>非dev环境</b>生效（@Profile("!dev")），即test和prod环境</p>
 * <p>dev环境使用{@link DevSecurityConfig}，采用更宽松的安全策略</p>
 *
 * <h3>安全策略</h3>
 * <ul>
 *   <li><b>开放接口</b>：登录接口(/auth/**)、Swagger文档接口无需认证</li>
 *   <li><b>认证接口</b>：所有业务接口需要JWT认证</li>
 *   <li><b>禁用CSRF</b>：前后端分离项目使用JWT，不需要CSRF保护</li>
 *   <li><b>禁用Session</b>：使用JWT进行无状态认证</li>
 * </ul>
 *
 * <h3>环境对比</h3>
 * <table border="1">
 *   <tr><th>环境</th><th>配置类</th><th>安全策略</th></tr>
 *   <tr><td>dev</td><td>{@link DevSecurityConfig}</td><td>宽松，保留方法级权限验证</td></tr>
 *   <tr><td>test/prod</td><td>{@link SecurityConfig}</td><td>严格，完整JWT认证和权限验证</td></tr>
 * </table>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see DevSecurityConfig
 * @see JwtAuthenticationFilter
 * @see JwtAuthenticationToken
 */
@Slf4j
@Configuration
@Profile("!dev")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤链
     *
     * <p>定义生产环境和测试环境的安全策略，包括请求授权、会话管理、JWT过滤器等</p>
     *
     * <p><b>配置策略：</b></p>
     * <ul>
     *   <li>禁用CSRF保护（前后端分离项目使用JWT，不需要CSRF）</li>
     *   <li>配置CORS跨域支持</li>
     *   <li>配置请求授权规则：
     *     <ul>
     *       <li>/auth/** - 登录/认证接口：完全开放</li>
     *       <li>/doc.html, /swagger-ui/**, /v3/api-docs/** - Swagger文档：完全开放</li>
     *       <li>其他所有接口：需要JWT认证</li>
     *     </ul>
     *   </li>
     *   <li>使用无状态会话管理（SessionCreationPolicy.STATELESS）</li>
     *   <li>禁用默认登录页面和HTTP Basic认证</li>
     *   <li>添加JWT认证过滤器（在UsernamePasswordAuthenticationFilter之前执行）</li>
     * </ul>
     *
     * @param http HttpSecurity配置对象
     * @return 配置好的SecurityFilterChain
     * @throws Exception 配置过程中的异常
     * @since 1.0
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离项目使用 JWT，不需要 CSRF 保护）
                .csrf(AbstractHttpConfigurer::disable)
                
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                    // 登录/认证接口：开放
                    .requestMatchers("/auth/**").permitAll()
                    // Swagger/Knife4j 静态资源与 OpenAPI JSON：开放（文档是否启用由 Knife4jConfig 控制）
                    .requestMatchers(
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico"
                    ).permitAll()
                    // 其余接口：需要认证
                    .anyRequest().authenticated()
                )
                
                // 配置会话管理：无状态（使用 JWT，不需要 Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 禁用默认的登录页面
                .formLogin(AbstractHttpConfigurer::disable)
                
                // 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                
                // 添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置密码编码器
     *
     * <p>使用BCrypt强哈希算法对用户密码进行加密和验证</p>
     *
     * <p><b>BCrypt特点：</b></p>
     * <ul>
     *   <li>单向哈希算法，不可逆</li>
     *   <li>自动加盐，每次加密结果不同（即使密码相同）</li>
     *   <li>计算强度可调（默认10轮），抵抗暴力破解</li>
     *   <li>生成的哈希值长度固定为60字符</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>用户注册/修改密码时：使用encoder.encode(plainPassword)加密</li>
     *   <li>用户登录验证时：使用encoder.matches(plainPassword, hashedPassword)验证</li>
     * </ul>
     *
     * @return BCryptPasswordEncoder实例
     * @since 1.0
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置CORS跨域
     *
     * <p>配置跨域资源共享策略，允许前端应用跨域访问后端API</p>
     *
     * <p><b>跨域配置：</b></p>
     * <ul>
     *   <li><b>允许的来源</b>：允许所有来源（生产环境建议限制为具体域名）</li>
     *   <li><b>允许的方法</b>：GET, POST, PUT, DELETE, OPTIONS</li>
     *   <li><b>允许的请求头</b>：所有请求头</li>
     *   <li><b>允许携带凭证</b>：true（支持Cookie和Authorization头）</li>
     *   <li><b>暴露的响应头</b>：Authorization, Content-Type</li>
     *   <li><b>预检请求有效期</b>：3600秒（1小时）</li>
     * </ul>
     *
     * <p><b>生产环境建议：</b></p>
     * <ul>
     *   <li>将setAllowedOriginPatterns从"*"改为具体的前端域名列表</li>
     *   <li>例如：Arrays.asList("https://his.example.com")</li>
     * </ul>
     *
     * @return CorsConfigurationSource配置对象
     * @since 1.0
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许所有来源（生产环境建议指定具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许所有请求方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许发送 Cookie
        configuration.setAllowCredentials(true);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
