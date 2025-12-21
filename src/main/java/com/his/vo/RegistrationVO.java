package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号响应 VO
 */
@Data
@Schema(description = "挂号信息")
public class RegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "挂号记录 ID", example = "1")
    private Long id;

    @Schema(description = "挂号流水号", example = "REG20231201001")
    private String regNo;

    @Schema(description = "患者姓名", example = "张三")
    private String patientName;

    @Schema(description = "患者 ID", example = "1001")
    private Long patientId;

    @Schema(description = "患者性别（0=女, 1=男, 2=未知）", example = "1")
    private Short gender;

    @Schema(description = "患者年龄", example = "35")
    private Short age;

    @Schema(description = "科室 ID", example = "10")
    private Long deptId;

    @Schema(description = "科室名称", example = "内科")
    private String deptName;

    @Schema(description = "医生 ID", example = "100")
    private Long doctorId;

    @Schema(description = "医生姓名", example = "李医生")
    private String doctorName;

    @Schema(description = "挂号状态（0=待就诊, 1=已就诊, 2=已取消）", example = "0")
    private Short status;

    @Schema(description = "状态描述", example = "待就诊")
    private String statusDesc;

    @Schema(description = "就诊日期", example = "2023-12-01", type = "string", format = "date")
    private LocalDate visitDate;

    @Schema(description = "挂号费", example = "15.00")
    private BigDecimal registrationFee;

    @Schema(description = "排队号", example = "A001")
    private String queueNo;

    @Schema(description = "预约时间", example = "2023-12-01T09:00:00", type = "string", format = "date-time")
    private LocalDateTime appointmentTime;

    @Schema(description = "创建时间", example = "2023-12-01T08:30:00", type = "string", format = "date-time")
    private LocalDateTime createdAt;
}
