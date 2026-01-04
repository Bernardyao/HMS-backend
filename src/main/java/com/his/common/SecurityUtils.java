package com.his.common;

import com.his.config.JwtAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * 
 * <p>提供快速访问当前登录用户信息的静态方法，主要用于防止水平越权（IDOR）攻击。
 * 
 * <h3>核心功能</h3>
 * <ul>
 *   <li>从 JWT Token 中提取当前用户的 ID、角色、关联ID（如医生ID）</li>
 *   <li>避免直接信任前端传递的用户标识参数</li>
 *   <li>为业务层提供统一的用户上下文访问入口</li>
 * </ul>
 * 
 * <h3>IDOR（Insecure Direct Object Reference）防护</h3>
 * <p>水平越权是一种常见的安全漏洞，攻击者通过修改请求参数（如 doctorId）来访问其他用户的数据。
 * 例如：
 * <pre>
 * // ❌ 不安全的实现（易受IDOR攻击）
 * {@code @GetMapping("/api/doctor/waiting-list")}
 * public Result getWaitingList(@RequestParam Long doctorId) {
 *     // 直接使用前端传递的 doctorId，攻击者可以修改参数查看其他医生的数据
 *     return doctorService.getWaitingList(doctorId);
 * }
 * 
 * // ✅ 安全的实现（使用 SecurityUtils）
 * {@code @GetMapping("/api/doctor/waiting-list")}
 * public Result getWaitingList() {
 *     Long doctorId = SecurityUtils.getCurrentDoctorId(); // 从 JWT Token 获取，无法伪造
 *     return doctorService.getWaitingList(doctorId);
 * }
 * </pre>
 * 
 * <h3>架构设计</h3>
 * <pre>
 * 1. 用户登录 → {@link com.his.service.AuthService#login}
 *    ↓
 * 2. 生成 JWT Token（包含 userId, username, role, relatedId）→ {@link JwtUtils#generateToken}
 *    ↓
 * 3. 前端在请求头中携带 Token：Authorization: Bearer {token}
 *    ↓
 * 4. {@link com.his.config.JwtAuthenticationFilter} 拦截请求，解析 Token，设置到 SecurityContext
 *    ↓
 * 5. 业务代码调用 {@link SecurityUtils} 获取当前用户信息（从 SecurityContext 中提取）
 *    ↓
 * 6. 执行业务逻辑，确保用户只能访问自己的数据
 * </pre>
 * 
 * <h3>使用场景示例</h3>
 * 
 * <h4>1. 医生工作站 - 查询候诊列表</h4>
 * <pre>
 * {@code @GetMapping("/api/doctor/waiting-list")}
 * public Result{@code <List<RegistrationVO>>} getWaitingList() {
 *     // ✅ 从 JWT Token 获取医生ID，防止查看其他医生的患者
 *     Long doctorId = SecurityUtils.getCurrentDoctorId();
 *     return Result.success(doctorService.getWaitingList(doctorId));
 * }
 * </pre>
 * 
 * <h4>2. 医生个人信息 - 更新资料</h4>
 * <pre>
 * {@code @PutMapping("/api/doctor/profile")}
 * public Result updateProfile({@code @RequestBody} DoctorProfileDTO dto) {
 *     // ✅ 确保只能更新自己的资料
 *     Long doctorId = SecurityUtils.getCurrentDoctorId();
 *     return Result.success(doctorService.updateProfile(doctorId, dto));
 * }
 * </pre>
 * 
 * <h4>3. 处方管理 - 创建处方</h4>
 * <pre>
 * {@code @PostMapping("/api/prescriptions")}
 * public Result createPrescription({@code @RequestBody} PrescriptionDTO dto) {
 *     // ✅ 记录开处方的医生（从 Token 获取，不信任前端传参）
 *     Long doctorId = SecurityUtils.getCurrentDoctorId();
 *     String doctorName = SecurityUtils.getCurrentUsername();
 *     return Result.success(prescriptionService.create(doctorId, doctorName, dto));
 * }
 * </pre>
 * 
 * <h3>错误处理</h3>
 * <ul>
 *   <li>如果用户未登录或 Token 无效，这些方法会抛出 {@link IllegalStateException}</li>
 *   <li>前端应捕获 401 响应，引导用户重新登录</li>
 * </ul>
 * 
 * @author HIS 开发团队
 * @version 1.0
 * @see JwtUtils
 * @see com.his.config.JwtAuthenticationToken
 * @see com.his.config.JwtAuthenticationFilter
 */
@Slf4j
public class SecurityUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取当前认证信息
     *
     * @return 当前认证对象
     * @throws IllegalStateException 如果用户未登录或认证信息无效
     */
    private static JwtAuthenticationToken getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            log.error("安全上下文中未找到认证信息，用户可能未登录");
            throw new IllegalStateException("用户未登录，请先登录");
        }
        
        if (!(authentication instanceof JwtAuthenticationToken)) {
            log.warn("认证信息类型异常，期望 JwtAuthenticationToken，实际: {}, 可能是未登录用户访问受保护资源",
                    authentication.getClass().getName());
            throw new IllegalStateException("认证信息无效，请重新登录");
        }
        
        return (JwtAuthenticationToken) authentication;
    }

    /**
     * 获取当前登录用户的 ID
     * 
     * <p><b>用途：</b>用于标识系统用户（his_sysuser 表的主键）
     *
     * @return 用户ID（his_sysuser.main_id）
     * @throws IllegalStateException 如果用户未登录
     */
    public static Long getCurrentUserId() {
        JwtAuthenticationToken token = getAuthentication();
        Long userId = token.getUserId();
        
        if (userId == null) {
            log.error("JWT Token 中缺少 userId 字段");
            throw new IllegalStateException("用户ID不存在，Token可能已损坏");
        }
        
        log.debug("获取当前用户ID: {}", userId);
        return userId;
    }

    /**
     * 获取当前登录用户的用户名
     * 
     * <p><b>用途：</b>用于日志记录、审计跟踪、界面展示
     *
     * @return 用户名（his_sysuser.username）
     * @throws IllegalStateException 如果用户未登录
     */
    public static String getCurrentUsername() {
        JwtAuthenticationToken token = getAuthentication();
        String username = token.getUsername();
        
        if (username == null || username.isEmpty()) {
            log.error("JWT Token 中缺少 username 字段");
            throw new IllegalStateException("用户名不存在，Token可能已损坏");
        }
        
        log.debug("获取当前用户名: {}", username);
        return username;
    }

    /**
     * 获取当前登录用户的角色
     * 
     * <p><b>用途：</b>用于业务逻辑中的角色判断（例如：医生可以开处方，护士不可以）
     * 
     * <p><b>角色列表：</b>
     * <ul>
     *   <li>ADMIN - 管理员</li>
     *   <li>DOCTOR - 医生</li>
     *   <li>NURSE - 护士</li>
     *   <li>PHARMACIST - 药师</li>
     *   <li>CASHIER - 收费员</li>
     * </ul>
     *
     * @return 角色代码（his_sysuser.role_code）
     * @throws IllegalStateException 如果用户未登录
     */
    public static String getCurrentRole() {
        JwtAuthenticationToken token = getAuthentication();
        String role = token.getRole();
        
        if (role == null || role.isEmpty()) {
            log.error("JWT Token 中缺少 role 字段");
            throw new IllegalStateException("用户角色不存在，Token可能已损坏");
        }
        
        log.debug("获取当前用户角色: {}", role);
        return role;
    }

    /**
     * 获取当前登录用户的关联ID（业务实体ID）
     * 
     * <p><b>核心安全方法 - 防止IDOR攻击的关键</b>
     * 
     * <p><b>用途：</b>根据用户角色，返回对应的业务实体主键：
     * <ul>
     *   <li>DOCTOR 角色 → 医生ID（his_doctor.main_id）</li>
     *   <li>NURSE 角色 → 护士ID（如果有护士表）</li>
     *   <li>PHARMACIST 角色 → 药师ID（如果有药师表）</li>
     *   <li>ADMIN 角色 → 可能为 null（管理员通常无关联业务实体）</li>
     * </ul>
     * 
     * <p><b>安全保障：</b>
     * <ul>
     *   <li>relatedId 存储在 JWT Token 中，由服务端生成，前端无法篡改</li>
     *   <li>即使攻击者修改请求参数，也无法伪造 relatedId</li>
     *   <li>业务代码应<b>始终</b>使用此方法获取操作者身份，而非信任前端传参</li>
     * </ul>
     * 
     * <p><b>使用示例：</b>
     * <pre>
     * // Controller 层
     * {@code @GetMapping("/api/doctor/my-patients")}
     * public Result getMyPatients() {
     *     Long doctorId = SecurityUtils.getCurrentDoctorId(); // ✅ 强制从 Token 获取
     *     return Result.success(patientService.findByDoctorId(doctorId));
     * }
     * </pre>
     *
     * @return 关联ID（his_sysuser.department_main_id，对于医生角色即 his_doctor.main_id）
     * @throws IllegalStateException 如果用户未登录
     */
    public static Long getRelatedId() {
        JwtAuthenticationToken token = getAuthentication();
        Long relatedId = token.getRelatedId();
        
        // relatedId 可能为 null（例如管理员账号），这是合法的
        log.debug("获取当前用户关联ID: {}", relatedId);
        return relatedId;
    }

    /**
     * 获取当前登录医生的 ID（便捷方法）
     * 
     * <p><b>用途：</b>医生工作站、处方管理、病历管理等需要医生身份的场景
     * 
     * <p><b>与 {@link #getRelatedId()} 的区别：</b>
     * <ul>
     *   <li>{@link #getRelatedId()} - 通用方法，返回值可能为 null</li>
     *   <li>{@link #getCurrentDoctorId()} - 专用方法，如果为 null 会抛出异常，确保医生身份有效</li>
     * </ul>
     * 
     * <p><b>安全说明：</b>
     * <ul>
     *   <li>仅在已确认用户角色为 DOCTOR 的接口中调用</li>
     *   <li>配合 {@code @PreAuthorize("hasRole('DOCTOR')")} 使用，确保角色正确</li>
     * </ul>
     * 
     * <p><b>使用示例：</b>
     * <pre>
     * {@code @PreAuthorize("hasRole('DOCTOR')")}
     * {@code @GetMapping("/api/doctor/waiting-list")}
     * public Result getWaitingList() {
     *     Long doctorId = SecurityUtils.getCurrentDoctorId(); // ✅ 安全获取医生ID
     *     return Result.success(doctorService.getWaitingList(doctorId));
     * }
     * </pre>
     *
     * @return 医生ID（his_doctor.main_id）
     * @throws IllegalStateException 如果用户未登录或不是医生角色
     */
    public static Long getCurrentDoctorId() {
        Long relatedId = getRelatedId();
        
        if (relatedId == null) {
            String role = getCurrentRole();
            log.error("尝试获取医生ID失败，当前用户角色: {}, relatedId 为 null", role);
            throw new IllegalStateException(
                    "当前用户不是医生或未关联医生信息，角色: " + role);
        }
        
        log.debug("获取当前医生ID: {}", relatedId);
        return relatedId;
    }

    /**
     * 检查当前用户是否拥有指定角色
     * 
     * <p><b>用途：</b>在业务逻辑中进行角色判断
     *
     * @param role 角色代码（例如：DOCTOR、ADMIN）
     * @return 如果当前用户拥有该角色返回 true，否则返回 false
     */
    public static boolean hasRole(String role) {
        try {
            String currentRole = getCurrentRole();
            boolean hasRole = role != null && role.equals(currentRole);
            log.debug("角色检查 - 期望: {}, 实际: {}, 结果: {}", role, currentRole, hasRole);
            return hasRole;
        } catch (IllegalStateException e) {
            log.warn("角色检查失败，用户未登录: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查当前用户是否为医生
     * 
     * <p><b>用途：</b>快速判断用户是否有医生权限
     *
     * @return 如果是医生返回 true，否则返回 false
     */
    public static boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    /**
     * 检查当前用户是否为管理员
     * 
     * <p><b>用途：</b>快速判断用户是否有管理员权限
     *
     * @return 如果是管理员返回 true，否则返回 false
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 验证当前用户是否有权访问指定资源
     * 
     * <p><b>用途：</b>防止水平越权，确保用户只能访问自己的资源
     * 
     * <p><b>使用示例：</b>
     * <pre>
     * // 检查医生是否有权查看指定病历
     * {@code @GetMapping("/api/medical-records/{id}")}
     * public Result getMedicalRecord(@PathVariable Long id) {
     *     MedicalRecord record = medicalRecordService.findById(id);
     *     
     *     // ✅ 验证：只有创建该病历的医生才能查看
     *     SecurityUtils.validateResourceAccess(
     *         record.getDoctorId(), 
     *         "您无权查看此病历"
     *     );
     *     
     *     return Result.success(record);
     * }
     * </pre>
     *
     * @param resourceOwnerId 资源所有者ID（例如：病历的 doctorId）
     * @param errorMessage 无权访问时的错误消息
     * @throws IllegalStateException 如果当前用户不是资源所有者
     */
    public static void validateResourceAccess(Long resourceOwnerId, String errorMessage) {
        Long currentDoctorId = getCurrentDoctorId();
        
        if (!currentDoctorId.equals(resourceOwnerId)) {
            log.warn("权限验证失败 - 当前医生ID: {}, 资源所有者ID: {}", 
                    currentDoctorId, resourceOwnerId);
            throw new IllegalStateException(errorMessage);
        }
        
        log.debug("权限验证通过 - 医生ID: {} 有权访问资源", currentDoctorId);
    }

    /**
     * 获取完整的认证令牌对象（高级用法）
     * 
     * <p><b>用途：</b>需要访问更多认证信息时使用
     *
     * @return JWT认证令牌对象
     * @throws IllegalStateException 如果用户未登录
     */
    public static JwtAuthenticationToken getAuthenticationToken() {
        return getAuthentication();
    }
}
