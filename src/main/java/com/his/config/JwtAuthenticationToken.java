package com.his.config;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

/**
 * JWT 认证Token
 * 扩展 Spring Security 的 Authentication，包含用户完整信息
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;
    private final String role;
    private final Long relatedId;

    public JwtAuthenticationToken(Long userId, String username, String role, Long relatedId) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.relatedId = relatedId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public String getName() {
        return username;
    }
}
