package com.his.config;

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.Getter;

/**
 * JWT认证Token
 *
 * <p>扩展Spring Security的AbstractAuthenticationToken，封装JWT认证成功后的用户完整信息</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>封装用户信息</b>：包含用户ID、用户名、角色、关联ID等完整信息</li>
 *   <li><b>Spring Security集成</b>：实现Authentication接口，可设置到SecurityContext</li>
 *   <li><b>权限封装</b>：自动将角色转换为GrantedAuthority（添加ROLE_前缀）</li>
 *   <li><b>认证状态管理</b>：构造时自动标记为已认证（setAuthenticated(true)）</li>
 * </ul>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li><b>userId</b>：系统用户表主键ID（SysUser.mainId）</li>
 *   <li><b>username</b>：用户名（登录账号）</li>
 *   <li><b>role</b>：用户角色（ADMIN/DOCTOR/NURSE/PHARMACIST/CASHIER等）</li>
 *   <li><b>relatedId</b>：关联ID（医生/护士/药师/收费员的具体ID），用于业务层权限验证</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>JWT过滤器</b>：{@link JwtAuthenticationFilter}验证Token后创建此对象</li>
 *   <li><b>业务层</b>：通过SecurityUtils获取当前登录用户信息</li>
 *   <li><b>权限验证</b>：通过@PreAuthorize注解进行方法级权限控制</li>
 *   <li><b>IDOR防护</b>：在Service层使用relatedId验证资源归属权</li>
 * </ul>
 *
 * <h3>权限前缀说明</h3>
 * <p>Spring Security的角色要求必须有ROLE_前缀，因此：</p>
 * <ul>
 *   <li>数据库存储：ADMIN</li>
 *   <li>JWT Token：ADMIN（原样存储）</li>
 *   <li>JwtAuthenticationToken：ROLE_ADMIN（自动添加前缀）</li>
 *   <li>@PreAuthorize注解：hasRole('ADMIN') 或 hasAuthority('ROLE_ADMIN')</li>
 * </ul>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>用户登录成功，JwtUtils生成JWT（包含userId, username, role, relatedId）</li>
 *   <li>用户请求接口，JwtAuthenticationFilter从Token中提取用户信息</li>
 *   <li>创建JwtAuthenticationToken对象，传入用户信息</li>
 *   <li>设置到SecurityContext：SecurityContextHolder.getContext().setAuthentication(authentication)</li>
 *   <li>后续业务代码通过SecurityUtils获取当前用户信息</li>
 * </ol>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see JwtAuthenticationFilter
 * @see com.his.common.JwtUtils
 * @see com.his.common.SecurityUtils
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;
    private final String role;
    private final Long relatedId;

    public JwtAuthenticationToken(Long userId, String username, String role, Long relatedId) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.relatedId = relatedId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public String getName() {
        return username;
    }
}
