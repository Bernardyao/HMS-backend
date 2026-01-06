package com.his.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 患者详细信息 VO（医生工作站专用）
 *
 * <p><b>数据脱敏说明：</b>
 * <ul>
 *   <li>身份证号：显示为 320***********1234</li>
 *   <li>手机号：显示为 138****5678</li>
 *   <li>紧急联系人电话：显示为 139****8765</li>
 * </ul>
 *
 * <p><b>权限说明：</b>
 * <ul>
 *   <li>仅医生和管理员角色可访问</li>
 *   <li>从JWT Token获取医生身份，防止IDOR攻击</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "患者详细信息（医生工作站）")
public class PatientDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "患者ID", example = "1")
    private Long patientId;

    @Schema(description = "病历号", example = "P20231201001")
    private String patientNo;

    @Schema(description = "患者姓名", example = "张三")
    private String name;

    @Schema(description = "性别（0=女, 1=男, 2=未知）",
            example = "1",
            allowableValues = {"0", "1", "2"})
    private Short gender;

    @Schema(description = "性别描述", example = "男")
    private String genderDesc;

    @Schema(description = "年龄", example = "35")
    private Short age;

    @Schema(description = "出生日期", example = "1988-05-15")
    private LocalDate birthDate;

    @Schema(description = "联系电话（脱敏）", example = "138****5678")
    private String phone;

    @Schema(description = "身份证号（脱敏）", example = "320***********1234")
    private String idCard;

    @Schema(description = "联系地址", example = "江苏省南京市鼓楼区XX路XX号")
    private String address;

    @Schema(description = "医保卡号", example = "MED001")
    private String medicalCardNo;

    @Schema(description = "血型", example = "A")
    private String bloodType;

    @Schema(description = "过敏史", example = "青霉素过敏")
    private String allergyHistory;

    @Schema(description = "既往病史", example = "高血压3年")
    private String medicalHistory;

    @Schema(description = "紧急联系人", example = "李四")
    private String emergencyContact;

    @Schema(description = "紧急联系电话（脱敏）", example = "139****8765")
    private String emergencyPhone;

    @Schema(description = "创建时间", example = "2023-12-01T08:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2023-12-01T08:30:00")
    private LocalDateTime updatedAt;
}
