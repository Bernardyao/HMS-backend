package com.his.service;

import com.his.common.JwtUtils;
import com.his.dto.LoginRequest;
import com.his.entity.SysUser;
import com.his.repository.SysUserRepository;
import com.his.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含 Token）
     */
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        log.info("用户登录尝试，用户名: {}", username);

        // 1. 查询用户
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("登录失败：用户不存在，用户名: {}", username);
                    return new RuntimeException("用户名或密码错误");
                });

        // 2. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("登录失败：账号已停用，用户名: {}", username);
            throw new RuntimeException("账号已被停用，请联系管理员");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败：密码错误，用户名: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        String token = jwtUtils.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getRelatedId()
        );

        log.info("用户登录成功，用户名: {}, 角色: {}", username, user.getRole());

        // 5. 构建返回结果
        return LoginVO.builder()
                .token(token)
                .role(user.getRole())
                .realName(user.getRealName())
                .userId(user.getId())
                .relatedId(user.getRelatedId())
                .build();
    }

    /**
     * 验证 Token
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            return jwtUtils.validateToken(token);
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取用户信息
     *
     * @param token JWT Token
     * @return 用户信息
     */
    public SysUser getUserFromToken(String token) {
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            return sysUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
        } catch (Exception e) {
            log.error("从 Token 获取用户信息失败: {}", e.getMessage());
            throw new RuntimeException("Token 无效");
        }
    }
}
