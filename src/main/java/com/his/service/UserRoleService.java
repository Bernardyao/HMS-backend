package com.his.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.his.vo.views.MedicineViews;

/**
 * 用户角色视图解析服务
 * <p>
 * 负责根据当前用户的角色确定应使用的 JsonView 类型，
 * 实现字段级别的访问控制。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li><b>单一职责</b>：只负责角色到视图的映射，不处理其他业务逻辑</li>
 *   <li><b>可测试性</b>：避免静态依赖，支持 Mock</li>
 *   <li><b>显式状态判断</b>：不使用异常作为控制流</li>
 *   <li><b>防御性编程</b>：对 null、空值等边界情况有明确处理</li>
 * </ul>
 *
 * <h3>角色到视图的映射规则：</h3>
 * <table border="1">
 *   <tr><th>角色</th><th>视图类型</th><th>可见字段</th></tr>
 *   <tr><td>PHARMACIST / ADMIN</td><td>Pharmacist</td><td>所有字段（含进货价、利润率）</td></tr>
 *   <tr><td>DOCTOR</td><td>Doctor</td><td>医生字段（不含进货价）</td></tr>
 *   <tr><td>其他/未认证</td><td>Public</td><td>基础字段</td></tr>
 * </table>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 */
@Service
public class UserRoleService {

    private static final Logger LOG = LoggerFactory.getLogger(UserRoleService.class);

    /**
     * 获取当前用户对应的药品 JsonView 类型
     * <p>
     * 根据用户的 Spring Security 角色确定应返回的字段视图。
     * 使用显式的 null 检查和状态判断，避免异常驱动逻辑。
     * </p>
     *
     * <h3>决策流程：</h3>
     * <ol>
     *   <li>从 SecurityContextHolder 获取 Authentication</li>
     *   <li>如果为 null 或未认证，返回 Public 视图</li>
     *   <li>遍历用户的 granted authorities，提取角色</li>
     *   <li>根据优先级返回视图：Pharmacist > Doctor > Public</li>
     * </ol>
     *
     * @return JsonView 类型（MedicineViews.Public/Doctor/Pharmacist）
     */
    public Class<?> getMedicineViewForCurrentUser() {
        Authentication authentication = getAuthentication();

        // 显式判断：未认证或无认证信息
        if (authentication == null || !isAuthenticated(authentication)) {
            LOG.debug("用户未认证，使用 Public 视图");
            return MedicineViews.Public.class;
        }

        // 提取用户角色并映射到视图
        String highestPriorityRole = extractHighestPriorityRole(authentication);

        if (highestPriorityRole == null) {
            LOG.debug("用户无角色信息，使用默认 Public 视图");
            return MedicineViews.Public.class;
        }

        return mapRoleToMedicineView(highestPriorityRole);
    }

    /**
     * 获取当前用户的处方 JsonView 类型
     * <p>
     * 目前处方不使用 JsonView，返回 null。保留此方法以保持接口一致性。
     * </p>
     * <p>
     * <b>TODO</b>: 未来如需对处方实现字段级权限控制，可参考 getMedicineViewForCurrentUser() 的实现。
     * </p>
     *
     * @return null（处方暂不使用 JsonView）
     */
    @SuppressWarnings("unused") // 保留此方法以供未来扩展，当前暂未使用
    public Class<?> getPrescriptionViewForCurrentUser() {
        return null; // 处方暂不使用 JsonView
    }

    /**
     * 获取 Authentication 对象（可被子类 Mock）
     * <p>
     * protected 方法设计：允许在测试中 override 返回 Mock 对象。
     * </p>
     *
     * @return 当前认证信息，可能为 null
     */
    protected Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断用户是否已认证
     * <p>
     * 显式检查 authentication 的状态和类型，避免依赖异常处理。
     * </p>
     *
     * @param authentication 认证信息
     * @return true=已认证，false=未认证或匿名
     */
    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        // 检查是否为匿名用户（Spring Security 的 AnonymousAuthenticationToken）
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        // 检查是否已认证
        return authentication.isAuthenticated();
    }

    /**
     * 从 Authentication 中提取优先级最高的角色
     * <p>
     * 优先级：PHARMACIST > ADMIN > DOCTOR > 其他
     * </p>
     *
     * @param authentication 认证信息
     * @return 优先级最高的角色，如果没有则返回 null
     */
    private String extractHighestPriorityRole(Authentication authentication) {
        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return null;
        }

        String highestPriorityRole = null;

        // 遍历所有角色，按优先级选择
        for (var authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }

            String role = normalizeRole(authority.getAuthority());

            // 按优先级匹配（高优先级覆盖低优先级）
            if ("PHARMACIST".equals(role) || "ADMIN".equals(role)) {
                LOG.debug("检测到角色: {}，优先级最高，使用 Pharmacist 视图", role);
                return role;
            }

            if ("DOCTOR".equals(role)) {
                highestPriorityRole = role;
            }
        }

        if (highestPriorityRole != null) {
            LOG.debug("检测到角色: {}，使用 Doctor 视图", highestPriorityRole);
        }

        return highestPriorityRole;
    }

    /**
     * 规范化角色名称
     * <p>
     * 移除 Spring Security 默认的 "ROLE_" 前缀。
     * </p>
     *
     * @param authority 原始权限字符串（如 "ROLE_DOCTOR"）
     * @return 规范化后的角色名（如 "DOCTOR"）
     */
    private String normalizeRole(String authority) {
        if (authority == null || authority.isEmpty()) {
            return "";
        }

        // 移除 ROLE_ 前缀（如果有）
        if (authority.startsWith("ROLE_")) {
            return authority.substring(5);
        }

        return authority;
    }

    /**
     * 将角色映射到药品视图类型
     * <p>
     * 映射规则：
     * <ul>
     *   <li>PHARMACIST / ADMIN → Pharmacist</li>
     *   <li>DOCTOR → Doctor</li>
     *   <li>其他 → Public</li>
     * </ul>
     * </p>
     *
     * @param role 角色名称
     * @return JsonView 类型
     */
    private Class<?> mapRoleToMedicineView(String role) {
        if (role == null) {
            return MedicineViews.Public.class;
        }

        switch (role) {
            case "PHARMACIST":
            case "ADMIN":
                return MedicineViews.Pharmacist.class;
            case "DOCTOR":
                return MedicineViews.Doctor.class;
            default:
                LOG.debug("未知角色: {}，使用默认 Public 视图", role);
                return MedicineViews.Public.class;
        }
    }

    /**
     * 检查当前用户是否具有指定角色
     * <p>
     * 提供显式的角色检查方法，避免在 Controller 中直接处理 SecurityContext。
     * </p>
     *
     * @param role 角色名称（不需要 ROLE_ 前缀）
     * @return true=有该角色，false=没有
     */
    public boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        if (authentication == null || !isAuthenticated(authentication)) {
            return false;
        }

        if (authentication.getAuthorities() == null) {
            return false;
        }

        String normalizedTargetRole = role.toUpperCase();

        for (var authority : authentication.getAuthorities()) {
            if (authority != null && authority.getAuthority() != null) {
                String normalizedAuthority = normalizeRole(authority.getAuthority()).toUpperCase();
                if (normalizedAuthority.equals(normalizedTargetRole)) {
                    return true;
                }
            }
        }

        return false;
    }
}
