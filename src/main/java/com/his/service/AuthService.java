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
 * 
 * <p>负责处理用户登录、Token 验证等认证相关业务逻辑。
 * 是 HIS 系统认证体系的核心服务之一。
 * 
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>用户登录：</b>验证用户名密码，生成包含完整用户信息的 JWT Token</li>
 *   <li><b>Token 验证：</b>检查 Token 的有效性和过期时间</li>
 *   <li><b>用户信息提取：</b>从 Token 中解析并获取用户实体</li>
 * </ul>
 * 
 * <h3>登录流程与安全设计</h3>
 * <pre>
 * 1. 前端提交登录请求 → POST /auth/login
 *    Body: { "username": "doctor001", "password": "******" }
 * 
 * 2. 后端验证用户名密码（BCrypt 加密比对）
 * 
 * 3. 从数据库查询用户的 relatedId（关键步骤）
 *    - 对于医生：relatedId = his_doctor.main_id
 *    - 对于护士：relatedId = 护士ID
 *    - 对于管理员：relatedId = null
 * 
 * 4. 生成 JWT Token，将 userId、username、role、relatedId 写入 Claims
 *    ↓
 *    Token 示例：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MSwidXNlcm5hbWUiOi...
 * 
 * 5. 返回给前端：
 *    {
 *      "code": 200,
 *      "data": {
 *        "token": "eyJhbGciOi...",
 *        "role": "DOCTOR",
 *        "realName": "张医生",
 *        "userId": 1,
 *        "relatedId": 10    ← 医生ID（his_doctor.main_id）
 *      }
 *    }
 * 
 * 6. 前端存储 Token 并在后续请求中携带（Authorization: Bearer {token}）
 * 
 * 7. 后端通过 {@link com.his.config.JwtAuthenticationFilter} 拦截请求，
 *    解析 Token，将用户信息设置到 SecurityContext
 * 
 * 8. 业务代码通过 {@link com.his.common.SecurityUtils} 获取当前用户信息
 * </pre>
 * 
 * <h3>relatedId 的核心作用（防止 IDOR 攻击）</h3>
 * <p><b>问题背景：</b>如果后端接口直接信任前端传递的 doctorId 参数，
 * 攻击者可以修改参数值查看其他医生的数据（水平越权）。
 * 
 * <p><b>解决方案：</b>在登录时将 relatedId 写入 JWT Token，
 * 业务代码从 Token 中提取此值，而非信任前端传参。
 * 
 * <pre>
 * // ❌ 不安全：直接使用前端传递的 doctorId
 * {@code @GetMapping("/api/doctor/waiting-list")}
 * public Result getWaitingList(@RequestParam Long doctorId) {
 *     return doctorService.getWaitingList(doctorId); // 可被攻击者修改
 * }
 * 
 * // ✅ 安全：从 JWT Token 获取 doctorId
 * {@code @GetMapping("/api/doctor/waiting-list")}
 * public Result getWaitingList() {
 *     Long doctorId = SecurityUtils.getCurrentDoctorId(); // 无法伪造
 *     return doctorService.getWaitingList(doctorId);
 * }
 * </pre>
 * 
 * <h3>依赖的组件</h3>
 * <ul>
 *   <li>{@link JwtUtils} - JWT Token 生成和解析</li>
 *   <li>{@link SysUserRepository} - 用户数据查询</li>
 *   <li>{@link PasswordEncoder} - BCrypt 密码加密验证</li>
 * </ul>
 * 
 * @author HIS 开发团队
 * @version 1.0
 * @see com.his.common.JwtUtils
 * @see com.his.common.SecurityUtils
 * @see com.his.config.JwtAuthenticationFilter
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
     * <p><b>核心业务逻辑：</b>验证用户身份并生成包含完整用户信息的 JWT Token
     * 
     * <p><b>登录流程：</b>
     * <ol>
     *   <li>根据用户名查询用户记录（his_sysuser表）</li>
     *   <li>检查账号状态（是否被停用）</li>
     *   <li>验证密码（BCrypt 加密比对）</li>
     *   <li><b>【关键步骤】</b>生成 JWT Token，将 relatedId（医生ID）写入 Token Claims</li>
     *   <li>返回 Token 和用户基本信息</li>
     * </ol>
     * 
     * <p><b>relatedId 的安全意义：</b>
     * <ul>
     *   <li>relatedId 来源于数据库（his_sysuser.department_main_id），对于医生角色即 his_doctor.main_id</li>
     *   <li>这个值由服务端控制，前端无法篡改</li>
     *   <li>后续业务接口通过 {@link com.his.common.SecurityUtils#getCurrentDoctorId()} 获取此值</li>
     *   <li>即使攻击者修改请求参数，也无法伪造 Token 中的 relatedId</li>
     * </ul>
     * 
     * <p><b>返回数据示例：</b>
     * <pre>
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "role": "DOCTOR",
     *   "realName": "张医生",
     *   "userId": 1,
     *   "relatedId": 10    ← 医生ID，从 his_sysuser.department_main_id 查询得到
     * }
     * </pre>
     * 
     * <p><b>异常处理：</b>
     * <ul>
     *   <li>用户不存在 → RuntimeException("用户名或密码错误")</li>
     *   <li>账号被停用 → RuntimeException("账号已被停用，请联系管理员")</li>
     *   <li>密码错误 → RuntimeException("用户名或密码错误")</li>
     * </ul>
     *
     * @param request 登录请求（包含 username 和 password）
     * @return 登录响应（包含 JWT Token、用户角色、姓名、ID、relatedId）
     * @throws RuntimeException 如果登录失败（用户名密码错误、账号被停用等）
     * 
     * @see JwtUtils#generateToken(Long, String, String, Long) 生成包含 relatedId 的 Token
     * @see com.his.common.SecurityUtils#getCurrentDoctorId() 业务代码中获取医生ID的方式
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

        // 4. 生成 JWT Token（关键：将 relatedId 写入 Token）
        // relatedId 对于医生角色即医生ID（his_doctor.main_id）
        // 后续业务接口通过 SecurityUtils.getCurrentDoctorId() 获取此值，防止水平越权
        String token = jwtUtils.generateToken(
                user.getId(),           // 系统用户ID
                user.getUsername(),     // 用户名
                user.getRole(),         // 角色（DOCTOR/ADMIN/NURSE等）
                user.getRelatedId()     // 关联ID（医生ID/护士ID等），来源于数据库，无法伪造
        );

        log.info("用户登录成功，用户名: {}, 角色: {}, relatedId: {}", 
                username, user.getRole(), user.getRelatedId());

        // 5. 构建返回结果（前端将 Token 存储并在后续请求中携带）
        return LoginVO.builder()
                .token(token)
                .role(user.getRole())
                .realName(user.getRealName())
                .userId(user.getId())
                .relatedId(user.getRelatedId())  // 也返回给前端展示，但后端不信任此值
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
