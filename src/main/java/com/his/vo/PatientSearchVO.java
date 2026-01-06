package com.his.vo;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 患者搜索结果 VO（护士工作站专用）
 *
 * <p><b>数据脱敏说明：</b>
 * <ul>
 *   <li>此VO不进行数据脱敏，用于护士核对患者信息</li>
 *   <li>身份证号和手机号为完整格式，便于准确核对</li>
 * </ul>
 *
 * <p><b>权限说明：</b>
 * <ul>
 *   <li>仅护士和管理员角色可访问</li>
 *   <li>所有查询操作会记录审计日志</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "患者搜索结果（护士工作站）")
public class PatientSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "患者ID", example = "1")
    private Long patientId;

    @Schema(description = "病历号", example = "P20231201001")
    private String patientNo;

    @Schema(description = "患者姓名", example = "张三")
    private String name;

    @Schema(description = "身份证号（完整，未脱敏）", example = "320106199001011234")
    private String idCard;

    @Schema(description = "性别（0=女, 1=男, 2=未知）", example = "1")
    private Short gender;

    @Schema(description = "性别描述", example = "男")
    private String genderDesc;

    @Schema(description = "年龄", example = "34")
    private Short age;

    @Schema(description = "手机号（完整，未脱敏）", example = "13812345678")
    private String phone;
}
