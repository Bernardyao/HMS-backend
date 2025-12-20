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
 * JWT 认证过滤器
 * 从请求头中提取 Token，验证并设置到 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            log.debug("请求路径: {}, Authorization Header: {}", 
                request.getRequestURI(), 
                authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "null");
            
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
     * 从请求头中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
