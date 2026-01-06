package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应视图对象
 *
 * <p>用于用户登录成功后，返回登录信息和认证令牌给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>身份认证</b>：提供JWT Token用于后续接口的身份验证</li>
 *   <li><b>用户信息</b>：返回用户的基本身份信息</li>
 *   <li><b>角色权限</b>：返回用户角色，前端据此控制菜单和功能权限</li>
 *   <li><b>关联信息</b>：返回用户关联的业务ID（如医生ID）</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从登录验证逻辑生成，包含用户认证信息和JWT Token</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>用户登录</b>：登录接口的返回数据</li>
 *   <li><b>Token刷新</b>：Token刷新接口的返回数据</li>
 * <li><b>身份验证</b>：前端将Token存储在localStorage中，后续请求携带在Header中</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>JWT Token</b>：有效期24小时，过期后需要重新登录或刷新</li>
 *   <li><b>role字段</b>：枚举值为 ADMIN、DOCTOR、NURSE、PHARMACIST、CASHIER</li>
 *   <li><b>relatedId字段</b>：根据role不同，关联不同的业务实体ID</li>
 *   <li><b>安全性</b>：Token应该妥善保管，避免XSS攻击泄露</li>
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
@Schema(description = "登录响应")
public class LoginVO {

    /**
     * JWT Token
     *
     * <p>用于后续接口调用的身份认证令牌</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：JWT（JSON Web Token）</li>
     *   <li>示例："eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."</li>
     *   <li>有效期：24小时</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>前端应在请求Header中添加：Authorization: Bearer {token}</li>
     *   <li>Token过期后需要重新登录</li>
     *   <li>不要将Token存储在Cookie中，建议使用localStorage</li>
     * </ul>
     */
    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    /**
     * 用户角色
     *
     * <p>用户的系统角色，决定用户的权限范围</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值：</li>
     *   <li>ADMIN - 系统管理员</li>
     *   <li>DOCTOR - 医生</li>
     *   <li>NURSE - 护士</li>
     *   <li>PHARMACIST - 药师</li>
     *   <li>CASHIER - 收费员</li>
     *   <li>示例："DOCTOR"</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>前端根据角色控制菜单显示</li>
     *   <li>前端根据角色控制页面权限</li>
     * </ul>
     */
    @Schema(description = "用户角色",
            example = "DOCTOR",
            allowableValues = {"ADMIN", "DOCTOR", "NURSE", "PHARMACIST", "CASHIER"})
    private String role;

    /**
     * 真实姓名
     *
     * <p>用户的真实姓名，用于界面展示</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："张三"</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>前端在顶部导航栏显示用户名</li>
     *   <li>用于日志记录的审计信息</li>
     * </ul>
     */
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 用户ID
     *
     * <p>用户在系统中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：1</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>用于后续接口的用户身份关联</li>
     *   <li>用于审计日志记录</li>
     * </ul>
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 关联ID
     *
     * <p>用户关联的业务实体ID，根据角色不同关联不同的实体</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>关联规则：</li>
     *   <li>DOCTOR - 关联医生ID</li>
     *   <li>NURSE - 关联护士ID</li>
     *   <li>PHARMACIST - 关联药师ID</li>
     *   <li>CASHIER - 关联收费员ID</li>
     *   <li>ADMIN - 可能为null</li>
     *   <li>示例：10（医生ID）</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>用于获取角色对应的业务数据</li>
     *   <li>医生角色用于查询医生排班、挂号等信息</li>
     * </ul>
     */
    @Schema(description = "关联ID（医生ID等）", example = "10")
    private Long relatedId;
}
