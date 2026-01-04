package com.his.config;

import com.his.common.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * <p>拦截所有HTTP请求，从请求头中提取JWT令牌进行验证，并将用户信息设置到Spring Security上下文中</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>Token提取</b>：从Authorization请求头中提取JWT令牌（支持Bearer前缀和直接传递两种格式）</li>
 *   <li><b>Token验证</b>：验证令牌的签名、有效期等</li>
 *   <li><b>用户信息提取</b>：从令牌中提取用户ID、用户名、角色、关联ID等信息</li>
 *   <li><b>认证上下文设置</b>：将用户信息封装为JwtAuthenticationToken并设置到SecurityContext</li>
 *   <li><b>异常处理</b>：认证失败时清空SecurityContext，记录警告日志</li>
 * </ul>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>拦截HTTP请求（继承OncePerRequestFilter确保每个请求只执行一次）</li>
 *   <li>从Authorization请求头中提取JWT令牌</li>
 *   <li>如果令牌存在且有效，使用JwtUtils提取用户信息</li>
 *   <li>创建JwtAuthenticationToken认证对象并设置到SecurityContext</li>
 *   <li>如果令牌无效或不存在，不做处理（让后续的Security配置处理）</li>
 *   <li>继续过滤链，传递请求给下一个过滤器</li>
 * </ol>
 *
 * <h3>Token格式支持</h3>
 * <ul>
 *   <li><b>标准格式</b>：Authorization: Bearer &lt;token&gt; （推荐）</li>
 *   <li><b>兼容格式</b>：Authorization: &lt;token&gt; （无Bearer前缀，兼容性处理）</li>
 * </ul>
 *
 * <h3>异常处理策略</h3>
 * <ul>
 *   <li>Token验证失败：记录Debug日志，继续过滤链（不抛出异常）</li>
 *   <li>处理过程异常：记录Warn日志，清空SecurityContext，继续过滤链</li>
 *   <li>不中断请求处理，由后续的AuthorizationDecision处理认证失败</li>
 * </ul>
 *
 * <h3>日志级别说明</h3>
 * <ul>
 *   <li><b>Debug级别</b>：记录Token提取、验证结果、用户信息（用于问题排查）</li>
 *   <li><b>Warn级别</b>：记录认证处理异常</li>
 *   <li><b>静态资源过滤</b>：排除Swagger静态资源的Debug日志，减少日志噪音</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.common.JwtUtils
 * @see JwtAuthenticationToken
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    /**
     * 执行JWT认证过滤
     *
     * <p>拦截每个HTTP请求，从请求头中提取和验证JWT令牌，并设置认证信息到SecurityContext</p>
     *
     * <p><b>处理流程：</b></p>
     * <ol>
     *   <li>排除静态资源路径的Debug日志（Swagger相关）</li>
     *   <li>从Authorization请求头中提取Token（调用extractToken方法）</li>
     *   <li>如果Token存在：
     *     <ul>
     *       <li>验证Token有效性（调用JwtUtils.validateToken）</li>
     *       <li>从Token中提取用户信息（userId, username, role, relatedId）</li>
     *       <li>创建JwtAuthenticationToken认证对象</li>
     *       <li>设置认证信息到SecurityContextHolder</li>
     *       <li>记录Debug日志</li>
     *     </ul>
     *   </li>
     *   <li>如果Token不存在或验证失败：记录Debug日志，不做处理</li>
     *   <li>捕获所有异常：记录Warn日志，清空SecurityContext，防止异常中断请求</li>
     *   <li>继续过滤链：调用filterChain.doFilter传递请求给下一个过滤器</li>
     * </ol>
     *
     * <p><b>设计要点：</b></p>
     * <ul>
     *   <li>继承OncePerRequestFilter：确保每个请求只执行一次（避免转发场景重复执行）</li>
     *   <li>不抛出异常：认证失败时由后续的AuthorizationDecision处理，避免过滤器链中断</li>
     *   <li>静默处理：没有Token时不做任何处理，让Spring Security根据配置决定是否允许访问</li>
     *   <li>日志优化：排除静态资源的Debug日志，减少日志噪音</li>
     * </ul>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     * @since 1.0
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            // 排除静态资源和Swagger相关路径的Debug日志，减少噪音
            boolean isStaticResource = requestURI.startsWith("/webjars/") || 
                                     requestURI.startsWith("/doc.html") || 
                                     requestURI.startsWith("/v3/api-docs") ||
                                     requestURI.startsWith("/favicon.ico") ||
                                     requestURI.startsWith("/swagger-resources");

            String authHeader = request.getHeader("Authorization");
            if (!isStaticResource) {
                log.debug("请求路径: {}, Authorization Header: {}", 
                    requestURI, 
                    authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "null");
            }
            
            // 从请求头中获取 Token
            String token = extractToken(request);

            // 只有当 Token 存在时才进行验证和处理
            if (token != null) {
                log.debug("提取的Token(前30字符): {}...", token.substring(0, Math.min(30, token.length())));
                
                // 验证 Token 是否有效
                if (jwtUtils.validateToken(token)) {
                    // 从 Token 中提取用户信息
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);
                    String role = jwtUtils.getRoleFromToken(token);
                    Long relatedId = jwtUtils.getRelatedIdFromToken(token);

                    // 创建自定义的认证对象，包含用户信息
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                            userId, username, role, relatedId
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("设置认证信息成功：用户ID={}, 用户名={}, 角色={}", userId, username, role);
                } else {
                    log.debug("Token 验证失败，请求路径: {}", request.getRequestURI());
                }
            }
            // 没有 Token 时不做任何处理，让 Spring Security 处理
        } catch (Exception e) {
            log.warn("JWT 认证处理异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
            // 认证失败时清空 SecurityContext
            SecurityContextHolder.clearContext();
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT令牌
     *
     * <p>支持两种Token格式：</p>
     * <ul>
     *   <li><b>标准格式</b>：Authorization: Bearer &lt;token&gt; （符合OAuth 2.0规范，推荐使用）</li>
     *   <li><b>兼容格式</b>：Authorization: &lt;token&gt; （无Bearer前缀，兼容性处理）</li>
     * </ul>
     *
     * <p><b>提取逻辑：</b></p>
     * <ol>
     *   <li>从请求头获取Authorization字段</li>
     *   <li>如果值为空或仅包含空白字符，返回null</li>
     *   <li>如果以"Bearer "（注意空格）开头，去除前缀返回剩余部分</li>
     *   <li>如果不以"Bearer "开头，直接返回整个值（兼容性处理）</li>
     * </ol>
     *
     * <p><b>示例：</b></p>
     * <pre>
     * // 输入：Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * // 输出：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * // 输入：Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * // 输出：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (兼容性处理)
     * </pre>
     *
     * @param request HTTP请求对象
     * @return 提取的JWT令牌字符串，如果不存在则返回null
     * @since 1.0
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            // 兼容性处理：如果前端未发送 "Bearer " 前缀，直接使用 Header 值
            // 这种情况下日志可能会提示格式警告，但允许通过
            return bearerToken;
        }
        return null;
    }
}
