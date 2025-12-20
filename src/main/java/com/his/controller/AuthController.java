package com.his.controller;

import com.his.common.Result;
import com.his.dto.LoginRequest;
import com.his.service.AuthService;
import com.his.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
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
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("接收登录请求，用户名: {}", request.getUsername());
            LoginVO loginVO = authService.login(request);
            return Result.success("登录成功", loginVO);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
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
    public Result<String> logout() {
        log.info("用户登出");
        return Result.success("登出成功", null);
    }
}
