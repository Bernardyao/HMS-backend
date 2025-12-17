package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 挂号请求 DTO
 */
@Schema(description = "挂号请求数据")
@Data
public class RegistrationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============ 患者信息（用于建档） ============

    /**
     * 患者姓名
     */
    @NotBlank(message = "患者姓名不能为空")
    @Schema(description = "患者姓名", example = "李明")
    private String patientName;

    /**
     * 身份证号
     */
    @NotBlank(message = "身份证号不能为空")
    @Schema(description = "身份证号", example = "110101199001011001")
    private String idCard;

    /**
     * 性别（0=女, 1=男, 2=未知）
     */
    @NotNull(message = "性别不能为空")
    @Schema(description = "性别（0=女, 1=男, 2=未知）", example = "1")
    private Short gender;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Schema(description = "年龄", example = "34")
    private Short age;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话", example = "13912345678")
    private String phone;

    // ============ 挂号信息 ============

    /**
     * 科室 ID
     */
    @NotNull(message = "科室ID不能为空")
    @Schema(description = "科室ID", example = "1")
    private Long deptId;

    /**
     * 医生 ID
     */
    @NotNull(message = "医生ID不能为空")
    @Schema(description = "医生ID", example = "1")
    private Long doctorId;

    /**
     * 挂号费
     */
    @NotNull(message = "挂号费不能为空")
    @Schema(description = "挂号费（单位：元）", example = "20.00")
    private BigDecimal regFee;
}
