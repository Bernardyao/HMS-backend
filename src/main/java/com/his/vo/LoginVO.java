package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "用户角色", example = "ADMIN")
    private String role;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "关联ID（医生ID等）", example = "10")
    private Long relatedId;
}
