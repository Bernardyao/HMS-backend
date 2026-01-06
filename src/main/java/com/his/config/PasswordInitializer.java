package com.his.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.his.entity.SysUser;
import com.his.repository.SysUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 密码初始化器
 *
 * <p>应用启动时自动检测并修复损坏或无效的BCrypt密码，确保开发和测试环境的用户可以正常登录</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>启动时自动运行</b>：实现CommandLineRunner接口，在应用启动后自动执行</li>
 *   <li><b>密码验证</b>：检查所有系统用户的密码是否符合BCrypt标准</li>
 *   <li><b>自动修复</b>：对无效密码自动重置为默认密码</li>
 *   <li><b>环境限制</b>：仅在dev和test环境运行，生产环境不会执行</li>
 * </ul>
 *
 * <h3>密码验证标准</h3>
 * <p>以下情况的密码会被判定为无效并重置：</p>
 * <ol>
 *   <li>密码为空或null</li>
 *   <li>密码长度异常（BCrypt标准长度为60字符，允许范围59-60）</li>
 *   <li>密码格式不正确（必须以$2a$或$2b$开头）</li>
 *   <li>密码无法通过验证（使用默认密码进行BCrypt匹配测试失败）</li>
 * </ol>
 *
 * <h3>重置策略</h3>
 * <ul>
 *   <li><b>默认密码</b>：admin123</li>
 *   <li><b>重置方式</b>：使用当前环境的PasswordEncoder进行BCrypt加密</li>
 *   <li><b>日志记录</b>：记录重置的用户名、角色、新密码等信息（WARN级别）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>数据导入</b>：从其他环境导入用户数据后，密码哈希可能不兼容</li>
 *   <li><b>算法升级</b>：BCrypt版本升级后，旧密码格式可能无法验证</li>
 *   <li><b>数据修复</b>：手动修改数据库导致密码损坏</li>
 *   <li><b>开发测试</b>：快速重置测试账号密码，方便开发调试</li>
 * </ul>
 *
 * <h3>安全注意事项</h3>
 * <ul>
 *   <li><b>环境隔离</b>：使用@Profile({"dev", "test"})，生产环境绝对不会执行</li>
 *   <li><b>默认密码</b>：仅用于开发测试，生产环境必须使用强密码</li>
 *   <li><b>日志记录</b>：所有密码重置操作都会记录WARN日志，便于审计</li>
 *   <li><b>异常处理</b>：捕获所有异常，避免密码检查失败影响应用启动</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see org.springframework.boot.CommandLineRunner
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

    /**
     * 应用启动时执行密码检查和修复
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>查询所有系统用户</li>
     *   <li>逐个检查用户密码是否有效（调用needsPasswordReset方法）</li>
     *   <li>对无效密码进行重置：
     *     <ul>
     *       <li>使用PasswordEncoder.encode()加密默认密码</li>
     *       <li>更新用户记录到数据库</li>
     *       <li>记录WARN日志（包含用户名、角色、新密码）</li>
     *     </ul>
     *   </li>
     *   <li>输出统计信息：修复数量或检查完成信息</li>
     *   <li>捕获所有异常，记录ERROR日志，避免影响应用启动</li>
     * </ol>
     *
     * <p><b>日志输出示例：</b></p>
     * <pre>
     * === 开始检查系统用户密码 ===
     * WARN - 已重置用户密码 - 用户名: admin, 角色: ADMIN, 新密码: admin123
     * WARN - 已重置用户密码 - 用户名: doctor001, 角色: DOCTOR, 新密码: admin123
     * WARN - === 密码初始化完成：已修复 2 个用户的密码，默认密码: admin123 ===
     * </pre>
     *
     * @param args 命令行参数（Spring Boot传入，本方法不使用）
     * @since 1.0
     */
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
     * 检查用户密码是否需要重置
     *
     * <p>通过四个维度的检查判断密码是否有效，任何一项不满足即判定为需要重置</p>
     *
     * <p><b>检查维度：</b></p>
     * <ol>
     *   <li><b>空值检查</b>：密码为null或空字符串 → 需要重置</li>
     *   <li><b>长度检查</b>：BCrypt哈希值标准长度为60字符（允许范围59-60） → 不在范围内需要重置</li>
     *   <li><b>格式检查</b>：BCrypt哈希值必须以$2a$或$2b$开头 → 不匹配需要重置</li>
     *   <li><b>验证检查</b>：使用默认密码进行BCrypt匹配测试 → 验证失败需要重置</li>
     * </ol>
     *
     * <p><b>BCrypt哈希格式：</b></p>
     * <pre>
     * $2a$[轮数]$[盐值][哈希值]
     * 示例：$2a$10$N9qo8uLOickgx2ZMRZoMye.IKN/6v3.1.7
     *       |  |   |                  |
     *    前缀 轮数  盐值(22字符)       哈希值(31字符)
     * </pre>
     *
     * <p><b>检查顺序说明：</b></p>
     * <ul>
     *   <li>先进行简单的空值、长度、格式检查（快速失败）</li>
     *   <li>最后进行耗时的验证检查（BCrypt哈希计算）</li>
     *   <li>每个检查点都会记录WARN日志，便于问题排查</li>
     * </ul>
     *
     * @param user 系统用户实体
     * @return true表示密码需要重置，false表示密码正常
     * @since 1.0
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
            log.warn("用户 {} 密码长度异常，需要重置", user.getUsername());
            return true;
        }

        // 检查3：格式不正确
        if (!password.startsWith("$2a$") && !password.startsWith("$2b$")) {
            log.warn("用户 {} 密码格式异常，需要重置", user.getUsername());
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
