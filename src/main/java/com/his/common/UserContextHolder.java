package com.his.common;

import com.his.dto.CurrentUser;

/**
 * 用户上下文持有者
 *
 * <p>使用ThreadLocal存储当前请求的登录用户信息，为整个请求处理过程提供用户上下文访问</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>用户信息存储</b>：在请求处理过程中存储当前登录用户的完整信息</li>
 *   <li><b>线程安全</b>：使用ThreadLocal确保每个请求线程拥有独立的用户上下文</li>
 *   <li><b>便捷访问</b>：提供静态方法快速获取当前用户信息</li>
 *   <li><b>资源管理</b>：提供clear()方法清理ThreadLocal，防止内存泄漏</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>审计日志</b>：记录操作者信息，追踪谁在何时执行了什么操作</li>
 *   <li><b>业务逻辑</b>：根据当前用户角色或科室进行权限判断和数据过滤</li>
 *   <li><b>数据隔离</b>：确保用户只能访问自己科室或角色的数据</li>
 *   <li><b>工作流</b>：在复杂业务流程中传递用户上下文，避免层层传递参数</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <h4>1. 在拦截器中设置用户上下文</h4>
 * <pre>
 * {@code @Override}
 * public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
 *     String token = request.getHeader("Authorization");
 *     CurrentUser user = authService.parseToken(token);
 *     UserContextHolder.setCurrentUser(user);  // 设置用户上下文
 *     return true;
 * }
 * </pre>
 *
 * <h4>2. 在Service层访问当前用户</h4>
 * <pre>
 * {@code @Service}
 * public class RegistrationService {
 *     public void cancel(Long registrationId, String reason) {
 *         Long currentUserId = UserContextHolder.getCurrentUserId();  // 获取当前用户ID
 *         log.info("用户{}取消挂号，原因：{}", currentUserId, reason);
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <h4>3. 在拦截器中清理用户上下文</h4>
 * <pre>
 * {@code @Override}
 * public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
 *                             Object handler, Exception ex) {
 *     UserContextHolder.clear();  // 清理用户上下文，防止内存泄漏
 * }
 * </pre>
 *
 * <h3>ThreadLocal注意事项</h3>
 * <ul>
 *   <li><b>必须清理</b>：请求结束时必须调用clear()，否则线程池复用会导致数据污染</li>
 *   <li><b>线程隔离</b>：每个线程有独立的用户上下文，子线程无法继承父线程的上下文</li>
 *   <li><b>内存泄漏</b>：如果忘记清理，会导致用户对象无法被GC回收</li>
 *   <li><b>异步任务</b>：异步任务中无法直接访问父线程的用户上下文，需要显式传递</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>在请求拦截器（preHandle）中设置用户上下文</li>
 *   <li>在请求完成后（afterCompletion）中清理用户上下文</li>
 *   <li>使用finally块确保清理一定会执行</li>
 *   <li>不要在ThreadLocal中存储大量数据，仅存储必要的用户信息</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.dto.CurrentUser
 */
public class UserContextHolder {

    private static final ThreadLocal<CurrentUser> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前用户信息
     *
     * @param user 当前用户
     */
    public static void setCurrentUser(CurrentUser user) {
        CONTEXT.set(user);
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前用户，如果未登录则返回 null
     */
    public static CurrentUser getCurrentUser() {
        return CONTEXT.get();
    }

    /**
     * 清除当前用户信息
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录则返回 null
     */
    public static Long getCurrentUserId() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户所属科室ID
     *
     * @return 科室ID，如果未登录则返回 null
     */
    public static Long getCurrentUserDeptId() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getDeptId() : null;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return true-是管理员，false-不是管理员
     */
    public static boolean isCurrentUserAdmin() {
        CurrentUser user = getCurrentUser();
        return user != null && user.isAdmin();
    }
}
