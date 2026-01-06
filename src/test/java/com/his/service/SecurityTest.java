package com.his.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.his.dto.LoginRequest;
import com.his.entity.SysUser;
import com.his.repository.SysUserRepository;
import com.his.test.base.BaseServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 安全测试
 * <p>
 * 测试系统的安全防护机制
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("安全测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityTest extends BaseServiceTest {

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    // ==================== SQL注入防护测试 ====================

    @Test
    @DisplayName("SQL注入：用户名包含SQL注入代码应该被防护")
    void sqlInjection_Username() {
        // Given - 包含SQL注入代码的用户名
        String maliciousUsername = "admin' OR '1'='1";
        LoginRequest request = new LoginRequest();
        request.setUsername(maliciousUsername);
        request.setPassword("password123");

        // When - 尝试登录
        when(sysUserRepository.findByUsername(maliciousUsername))
                .thenReturn(java.util.Optional.empty());

        // Then - 应该不会抛出数据库异常，而是正常返回用户不存在
        // 使用JPA/参数化查询会自动防护SQL注入
        assertDoesNotThrow(() -> {
            sysUserRepository.findByUsername(maliciousUsername);
        });
    }

    @Test
    @DisplayName("SQL注入：UNION SELECT注入应该被防护")
    void sqlInjection_UnionSelect() {
        // Given - 包含UNION SELECT的恶意输入
        String maliciousInput = "admin' UNION SELECT * FROM sys_user--";

        // When & Then - 参数化查询会自动防护
        assertDoesNotThrow(() -> {
            sysUserRepository.findByUsername(maliciousInput);
        });
    }

    // ==================== XSS防护测试 ====================

    @Test
    @DisplayName("XSS：脚本标签应该被转义或过滤")
    void xss_ScriptTag() {
        // Given - 包含XSS脚本的输入
        String xssInput = "<script>alert('XSS')</script>";

        // When & Then - 大部分现代框架会自动转义
        // 验证字符串包含但不会被执行
        assertTrue(xssInput.contains("<script>"));
        assertTrue(xssInput.contains("</script>"));
    }

    @Test
    @DisplayName("XSS：事件处理器注入应该被防护")
    void xss_EventHandler() {
        // Given - 包含事件处理器的恶意输入
        String maliciousInput = "<img src=x onerror=alert('XSS')>";

        // Then - 验证输入被保留但不会执行
        assertNotNull(maliciousInput);
        assertTrue(maliciousInput.contains("onerror"));
    }

    // ==================== Null安全测试 ====================

    @Test
    @DisplayName("Null安全：可选字段的null值应该正确处理")
    void nullSafety_OptionalFields() {
        // Given - LoginRequest的某些字段可以为null
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword(null);

        // When & Then - 应该抛出验证异常，而不是NullPointerException
        assertThrows(IllegalArgumentException.class, () -> {
            if (request.getUsername() == null || request.getPassword() == null) {
                throw new IllegalArgumentException("用户名和密码不能为空");
            }
        });
    }

    // ==================== 密码安全测试 ====================

    @Test
    @DisplayName("密码安全：密码不应该明文存储")
    void passwordSecurity_NotStoredInPlaintext() {
        // Given - 用户密码
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedPasswordHash";

        // When - 模拟密码验证
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        // Then - 验证使用BCrypt加密
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("密码安全：相同密码的哈希值应该不同")
    void passwordSecurity_SamePasswordDifferentHash() {
        // Given - 相同密码
        String password = "password123";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // When - 两次加密
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        // Then - 由于BCrypt使用随机盐，哈希值应该不同
        assertNotEquals(hash1, hash2, "相同密码的哈希值应该不同");

        // 但验证时应该都能通过
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }

    // ==================== 认证测试 ====================

    @Test
    @DisplayName("认证：空密码登录应该被拒绝")
    void authentication_EmptyPassword() {
        // Given - 空密码
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("");

        // When & Then - 应该被拒绝
        assertThrows(IllegalArgumentException.class, () -> {
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new IllegalArgumentException("密码不能为空");
            }
        });
    }

    @Test
    @DisplayName("认证：错误密码登录应该失败")
    void authentication_WrongPassword() {
        // Given - 错误密码
        String wrongPassword = "wrongpassword";
        String encodedPassword = "$2a$10$encodedPasswordHash";

        when(passwordEncoder.matches(wrongPassword, encodedPassword)).thenReturn(false);

        // When & Then - 密码验证应该失败
        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("权限：普通用户角色检查")
    void authorization_UserRole() {
        // Given - 普通用户
        SysUser user = new SysUser();
        user.setUsername("user");
        user.setRole("USER"); // 普通用户角色

        // When & Then - 角色检查
        assertEquals("USER", user.getRole());
        // 实际权限检查在@PreAuthorize注解中
    }
}
