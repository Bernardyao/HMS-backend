package com.his.dto;

import com.his.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户信息数据传输对象
 *
 * <p>用于在用户认证成功后封装当前登录用户的身份信息和权限数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>身份认证</b>：存储已登录用户的基本身份信息</li>
 *   <li><b>权限管理</b>：通过角色信息判断用户权限级别</li>
 *   <li><b>上下文传递</b>：在系统各层间传递用户上下文信息</li>
 *   <li><b>科室关联</b>：记录用户所属科室信息，用于业务隔离</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>登录认证</b>：用户登录成功后构建当前用户对象</li>
 *   <li><b>权限验证</b>：在业务逻辑中判断用户是否具有管理员权限</li>
 *   <li><b>操作审计</b>：记录操作人信息，用于日志审计和追溯</li>
 *   <li><b>数据隔离</b>：根据科室ID实现科室级别的数据隔离</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：id、username、name、role为必填字段</li>
 *   <li><b>业务验证</b>：role必须为有效的用户角色枚举值</li>
 *   <li><b>数据来源</b>：仅从认证成功的用户会话中获取，不允许前端传递</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {
    
    /**
     * 用户唯一标识（医生ID）
     *
     * <p>系统内部使用的用户唯一标识，用于关联数据库中的用户记录</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>必填字段</li>
     *   <li>必须为有效的长整型数值</li>
     *   <li>对应数据库中用户表的主键</li>
     * </ul>
     */
    private Long id;

    /**
     * 用户名
     *
     * <p>用户登录系统时使用的账号名称，用于身份验证</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>必填字段</li>
     *   <li>在系统中唯一</li>
     *   <li>长度范围：3-50个字符</li>
     * </ul>
     */
    private String username;

    /**
     * 真实姓名
     *
     * <p>用户的真实姓名，用于显示和报表打印</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>必填字段</li>
     *   <li>长度范围：2-50个字符</li>
     *   <li>支持中英文姓名</li>
     * </ul>
     */
    private String name;

    /**
     * 所属科室ID
     *
     * <p>用户所属的科室唯一标识，用于数据权限控制和业务隔离</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>可选字段（部分系统用户可能不归属具体科室）</li>
     *   <li>必须为有效的长整型数值</li>
     *   <li>对应科室表的主键</li>
     * </ul>
     */
    private Long deptId;

    /**
     * 所属科室名称
     *
     * <p>用户所属科室的名称，用于界面显示和报表输出</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>可选字段</li>
     *   <li>长度范围：2-50个字符</li>
     *   <li>与deptId保持一致</li>
     * </ul>
     */
    private String deptName;

    /**
     * 用户角色
     *
     * <p>定义用户在系统中的角色类型，决定用户的权限范围</p>
     *
     * <p><b>角色类型：</b></p>
     * <ul>
     *   <li><b>ADMIN</b>：系统管理员，拥有所有权限</li>
     *   <li><b>DOCTOR</b>：医生，可进行诊疗和开方操作</li>
     *   <li><b>NURSE</b>：护士，负责护理工作</li>
     *   <li><b>PHARMACIST</b>：药师，负责药品管理</li>
     *   <li><b>REGISTRAR</b>：挂号员，负责挂号业务</li>
     * </ul>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>必填字段</li>
     *   <li>必须为UserRole枚举中的有效值</li>
     * </ul>
     */
    private UserRole role;
    
    /**
     * 判断当前用户是否为管理员
     *
     * <p>通过比较用户角色与管理员角色，快速判断当前用户是否具有管理员权限</p>
     *
     * <p><b>返回值说明：</b></p>
     * <ul>
     *   <li><b>true</b>：当前用户是管理员，拥有所有权限</li>
     *   <li><b>false</b>：当前用户不是管理员，仅有角色对应的有限权限</li>
     * </ul>
     *
     * @return 如果用户角色为ADMIN返回true，否则返回false
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }
}
