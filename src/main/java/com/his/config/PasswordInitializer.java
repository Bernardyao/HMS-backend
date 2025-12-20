package com.his.config;

import com.his.entity.SysUser;
import com.his.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码初始化器
 * 应用启动时自动检测并修复损坏或无效的BCrypt密码
 * 
 * 注意：仅在开发和测试环境运行，生产环境不会执行
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class PasswordInitializer implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 默认密码（仅用于开发环境）
     */
    private static final String DEFAULT_PASSWORD = "admin123";

    @Override
    public void run(String... args) {
        log.info("=== 开始检查系统用户密码 ===");
        
        try {
            List<SysUser> users = sysUserRepository.findAll();
            int fixedCount = 0;
            
            for (SysUser user : users) {
                if (needsPasswordReset(user)) {
                    String newHash = passwordEncoder.encode(DEFAULT_PASSWORD);
                    user.setPassword(newHash);
                    sysUserRepository.save(user);
                    fixedCount++;
                    log.warn("已重置用户密码 - 用户名: {}, 角色: {}, 新密码: {}", 
                            user.getUsername(), user.getRole(), DEFAULT_PASSWORD);
                }
            }
            
            if (fixedCount > 0) {
                log.warn("=== 密码初始化完成：已修复 {} 个用户的密码，默认密码: {} ===", fixedCount, DEFAULT_PASSWORD);
            } else {
                log.info("=== 密码检查完成：所有用户密码正常 ===");
            }
            
        } catch (Exception e) {
            log.error("密码初始化失败", e);
        }
    }

    /**
     * 检查密码是否需要重置
     * 判断标准：
     * 1. 密码为空
     * 2. 密码长度异常（BCrypt标准长度为60，但某些实现可能是59-60）
     * 3. 密码格式不正确（不以$2a$或$2b$开头）
     * 4. 密码无法验证（测试失败）
     */
    private boolean needsPasswordReset(SysUser user) {
        String password = user.getPassword();
        
        // 检查1：密码为空
        if (password == null || password.trim().isEmpty()) {
            log.warn("用户 {} 密码为空", user.getUsername());
            return true;
        }
        
        // 检查2：长度不正确（BCrypt应该是59-60字符）
        if (password.length() < 59 || password.length() > 60) {
            log.warn("用户 {} 密码长度异常: {} (正常应为59-60)", user.getUsername(), password.length());
            return true;
        }
        
        // 检查3：格式不正确
        if (!password.startsWith("$2a$") && !password.startsWith("$2b$")) {
            log.warn("用户 {} 密码格式异常: {}", user.getUsername(), password.substring(0, Math.min(10, password.length())));
            return true;
        }
        
        // 检查4：验证测试（使用默认密码测试）
        try {
            if (passwordEncoder.matches(DEFAULT_PASSWORD, password)) {
                // 密码正确，不需要重置
                log.debug("用户 {} 密码验证成功，无需重置", user.getUsername());
                return false;
            } else {
                // 密码验证失败，需要重置
                log.warn("用户 {} 密码验证失败，需要重置", user.getUsername());
                return true;
            }
        } catch (Exception e) {
            log.warn("用户 {} 密码验证出错: {}", user.getUsername(), e.getMessage());
            return true;
        }
    }
}
