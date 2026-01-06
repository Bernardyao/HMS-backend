package com.his.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.dto.LoginRequest;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.log.utils.LogUtils;
import com.his.service.AuthService;
import com.his.vo.LoginVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证控制器
 *
 * <p>负责用户登录、登出、Token验证等认证相关功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>用户登录</b>：用户名密码登录，返回JWT Token</li>
 *   <li><b>Token验证</b>：验证Token是否有效</li>
 *   <li><b>用户登出</b>：清除客户端Token（服务端无状态）</li>
 * </ul>
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>登录接口：POST /auth/login（无需认证）</li>
 *   <li>Token验证：GET /auth/validate（需要认证）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.AuthService
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统，返回 JWT Token")
    @SecurityRequirements() // 明确标记此接口不需要认证
    @AuditLog(
        module = "认证管理",
        action = "用户登录",
        description = "用户登录系统",
        auditType = AuditType.SENSITIVE_OPERATION
    )
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        try {
            LogUtils.logBusinessOperation("认证管理", "用户登录尝试",
                    "用户名: " + request.getUsername());
            LoginVO loginVO = authService.login(request);
            return Result.success("登录成功", loginVO);
        } catch (Exception e) {
            LogUtils.logSystemError("认证管理", "登录失败", e);
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 验证 Token
     *
     * @param token JWT Token
     * @return 验证结果
     */
    @GetMapping("/validate")
    @Operation(summary = "验证Token", description = "验证 JWT Token 是否有效")
    @SecurityRequirements() // 此接口不需要认证
    public Result<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        try {
            // 移除 "Bearer " 前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            boolean valid = authService.validateToken(token);
            if (!valid) {
                return Result.error(401, "Token 验证失败");
            }
            return Result.success("Token 有效", valid);
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return Result.error(401, "Token 验证失败");
        }
    }

    /**
     * 登出（客户端删除 Token 即可，服务端无需处理）
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统（JWT 模式下客户端删除 Token 即可）")
    @SecurityRequirements() // 此接口不需要认证
    @AuditLog(
        module = "认证管理",
        action = "用户登出",
        description = "用户退出系统",
        auditType = AuditType.BUSINESS
    )
    public Result<String> logout() {
        LogUtils.logBusinessOperation("认证管理", "用户登出", "用户主动登出");
        return Result.success("登出成功", null);
    }
}
