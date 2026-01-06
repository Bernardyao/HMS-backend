package com.his.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 用户登录请求数据传输对象
 *
 * <p>用于封装用户登录时提交的身份验证信息，包括用户名和密码</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>身份验证</b>：接收并验证用户的登录凭证</li>
 *   <li><b>参数校验</b>：确保用户名和密码字段不为空</li>
 *   <li><b>接口文档</b>：为Swagger API文档提供登录参数说明</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>用户登录</b>：用户在登录页面提交登录信息时使用</li>
 *   <li><b>API认证</b>：系统外部调用需要认证的API前先进行登录</li>
 *   <li><b>token刷新</b>：部分系统可能用于重新获取访问令牌</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：username（用户名）、password（密码）</li>
 *   <li><b>格式验证</b>：用户名和密码不能为空字符串或纯空格</li>
 *   <li><b>业务验证</b>：需要验证用户名是否存在，密码是否正确</li>
 *   <li><b>安全要求</b>：密码应该在前端进行加密后传输（建议HTTPS）</li>
 * </ul>
 *
 * <h3>安全说明</h3>
 * <ul>
 *   <li>密码在传输过程中应该使用加密通道（HTTPS）</li>
 *   <li>建议前端对密码进行哈希处理后再发送</li>
 *   <li>登录失败应记录日志，防止暴力破解</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "用户登录请求对象")
public class LoginRequest {

    /**
     * 用户名
     *
     * <p>用户登录系统时使用的账号标识，系统通过该字段查找对应的用户记录</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：不能为空字符串或纯空格</li>
     *   <li><b>取值范围</b>：长度范围3-50个字符</li>
     *   <li><b>业务规则</b>：必须是系统中已注册的用户名</li>
     * </ul>
     */
    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 登录密码
     *
     * <p>用户的登录凭证，用于验证用户身份的合法性</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：不能为空字符串或纯空格</li>
     *   <li><b>取值范围</b>：建议长度6-20个字符</li>
     *   <li><b>安全要求</b>：应该通过加密方式传输（HTTPS）</li>
     * </ul>
     *
     * <p><b>安全说明：</b></p>
     * <ul>
     *   <li>建议使用HTTPS协议传输，防止密码泄露</li>
     *   <li>前端应该对密码进行哈希或加密处理</li>
     *   <li>后端会将密码与数据库中的哈希值进行比对验证</li>
     * </ul>
     */
    @Schema(description = "密码", example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;
}
