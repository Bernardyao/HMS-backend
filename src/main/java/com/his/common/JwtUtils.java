package com.his.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 
 * <p>负责生成和解析 JWT Token，是 HIS 系统认证体系的核心组件之一。
 * 
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>Token 生成：</b>将用户信息（userId, username, role, relatedId）编码为 JWT</li>
 *   <li><b>Token 解析：</b>从 JWT 中提取用户信息</li>
 *   <li><b>Token 验证：</b>检查 Token 的有效性和过期时间</li>
 * </ul>
 * 
 * <h3>JWT Claims 结构</h3>
 * <pre>
 * {
 *   "id": 1,                    // 用户ID（his_sysuser.main_id）
 *   "username": "doctor001",    // 用户名（his_sysuser.username）
 *   "role": "DOCTOR",           // 角色（his_sysuser.role_code）
 *   "relatedId": 10,            // 关联ID（his_sysuser.department_main_id，对于医生即 his_doctor.main_id）
 *   "sub": "doctor001",         // JWT 标准字段：Subject（与 username 相同）
 *   "iat": 1640000000,          // JWT 标准字段：签发时间
 *   "exp": 1640086400           // JWT 标准字段：过期时间
 * }
 * </pre>
 * 
 * <h3>relatedId 的重要性</h3>
 * <p><b>relatedId</b> 是防止水平越权（IDOR）攻击的关键字段：
 * <ul>
 *   <li>对于 DOCTOR 角色：relatedId = 医生ID（his_doctor.main_id）</li>
 *   <li>对于 NURSE 角色：relatedId = 护士ID（如果有护士表）</li>
 *   <li>对于 ADMIN 角色：relatedId 可能为 null</li>
 * </ul>
 * 
 * <p><b>安全原则：</b>
 * <ul>
 *   <li>relatedId 由服务端在用户登录时从数据库查询并写入 Token</li>
 *   <li>Token 使用 HMAC-SHA256 签名，前端无法伪造或篡改</li>
 *   <li>业务代码应通过 {@link SecurityUtils#getCurrentDoctorId()} 获取此值，而非信任前端传参</li>
 * </ul>
 * 
 * <h3>配置要求</h3>
 * <pre>
 * # application.yml
 * jwt:
 *   secret: your-256-bit-secret-key-here  # 密钥长度至少32字符
 *   expiration: 86400000                   # Token有效期（毫秒），24小时
 * </pre>
 * 
 * @author HIS 开发团队
 * @version 1.0
 * @see SecurityUtils
 * @see com.his.config.JwtAuthenticationFilter
 * @see com.his.service.AuthService
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT 密钥（从配置文件读取）
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Token 有效期（从配置文件读取，单位：毫秒）
     */
    @Value("${jwt.expiration}")
    private long expirationTime;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * 
     * <p><b>关键安全点：</b>将 <code>relatedId</code> 嵌入 Token，防止水平越权攻击
     * 
     * <p><b>使用场景：</b>用户登录成功后调用，生成包含用户完整身份信息的 Token
     * 
     * <p><b>Token 结构示例：</b>
     * <pre>
     * Header:
     * {
     *   "alg": "HS256",
     *   "typ": "JWT"
     * }
     * 
     * Payload:
     * {
     *   "id": 1,
     *   "username": "doctor001",
     *   "role": "DOCTOR",
     *   "relatedId": 10,        ← 关键字段：医生ID，由服务端从数据库查询
     *   "sub": "doctor001",
     *   "iat": 1640000000,
     *   "exp": 1640086400
     * }
     * 
     * Signature:
     * HMACSHA256(
     *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
     *   secret
     * )
     * </pre>
     * 
     * <p><b>安全说明：</b>
     * <ul>
     *   <li>relatedId 来源于数据库（his_sysuser.department_main_id），不允许用户自定义</li>
     *   <li>Token 使用 HMAC-SHA256 签名，任何修改都会导致验证失败</li>
     *   <li>前端将 Token 存储在 localStorage/sessionStorage，并在请求头中携带</li>
     *   <li>后续请求中，业务代码通过 {@link SecurityUtils#getCurrentDoctorId()} 提取 relatedId</li>
     * </ul>
     *
     * @param userId    用户ID（his_sysuser.main_id）
     * @param username  用户名（his_sysuser.username）
     * @param role      角色（his_sysuser.role_code，如：DOCTOR、ADMIN、NURSE）
     * @param relatedId 关联ID（his_sysuser.department_main_id），对于医生即 his_doctor.main_id；对于管理员可能为 null
     * @return JWT Token 字符串（格式：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...）
     * 
     * @see com.his.service.AuthService#login 登录时调用此方法生成 Token
     */
    public String generateToken(Long userId, String username, String role, Long relatedId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .claim("id", userId)
                .claim("username", username)
                .claim("role", role)
                .claim("relatedId", relatedId)
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中解析 Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            log.debug("开始解析Token，Token长度: {}, 前20字符: {}...", 
                token.length(), 
                token.substring(0, Math.min(20, token.length())));
            
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析 Token 失败: {}", e.getMessage());
            log.error("Token内容(前50字符): {}...", 
                token.substring(0, Math.min(50, token.length())));
            log.error("异常类型: {}", e.getClass().getName());
            throw new RuntimeException("Token 无效或已过期");
        }
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("id", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token
     * @return 角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从 Token 中获取关联ID（核心安全方法）
     * 
     * <p><b>用途：</b>提取 JWT 中的 relatedId 字段，用于识别当前操作的业务实体
     * 
     * <p><b>业务含义：</b>
     * <ul>
     *   <li><b>DOCTOR 角色：</b>relatedId = 医生ID（his_doctor.main_id）</li>
     *   <li><b>NURSE 角色：</b>relatedId = 护士ID（如果系统有护士管理）</li>
     *   <li><b>PHARMACIST 角色：</b>relatedId = 药师ID</li>
     *   <li><b>ADMIN 角色：</b>relatedId 可能为 null（管理员通常不关联业务实体）</li>
     * </ul>
     * 
     * <p><b>防止 IDOR 攻击的关键：</b>
     * <pre>
     * 场景：医生查询候诊列表
     * 
     * ❌ 不安全的做法：
     * {@code @GetMapping("/api/doctor/waiting-list")}
     * public Result getWaitingList(@RequestParam Long doctorId) {
     *     // 直接信任前端传参，攻击者可以修改 doctorId 查看其他医生的患者
     *     return doctorService.getWaitingList(doctorId);
     * }
     * 
     * ✅ 安全的做法：
     * {@code @GetMapping("/api/doctor/waiting-list")}
     * public Result getWaitingList() {
     *     // 从 Token 获取医生ID，无法伪造
     *     Long doctorId = SecurityUtils.getCurrentDoctorId();
     *     return doctorService.getWaitingList(doctorId);
     * }
     * </pre>
     * 
     * <p><b>推荐使用方式：</b>
     * <ul>
     *   <li>业务代码应使用 {@link SecurityUtils#getCurrentDoctorId()} 而非直接调用此方法</li>
     *   <li>{@link SecurityUtils} 提供了更好的错误处理和日志记录</li>
     * </ul>
     *
     * @param token JWT Token
     * @return 关联ID（可能为 null，例如管理员账号）
     * @throws RuntimeException 如果 Token 无效或已过期
     * 
     * @see SecurityUtils#getCurrentDoctorId() 推荐使用的安全方法
     * @see SecurityUtils#getRelatedId() 通用的关联ID获取方法
     */
    public Long getRelatedIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object relatedId = claims.get("relatedId");
        return relatedId != null ? Long.valueOf(relatedId.toString()) : null;
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
