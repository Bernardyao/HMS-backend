package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 护士工作站-挂号列表 VO
 */
@Data
@Schema(description = "护士工作站挂号信息")
public class NurseRegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "挂号记录 ID", example = "1")
    private Long id;

    @Schema(description = "挂号流水号", example = "REG20231201001")
    private String regNo;

    @Schema(description = "患者姓名", example = "张三")
    private String patientName;

    @Schema(description = "患者 ID", example = "1001")
    private Long patientId;

    @Schema(description = "患者性别描述", example = "男")
    private String genderDesc;

    @Schema(description = "患者年龄", example = "35")
    private Short age;

    @Schema(description = "患者身份证号（脱敏）", example = "320***********1234")
    private String idCard;

    @Schema(description = "患者联系电话（脱敏）", example = "138****5678")
    private String phone;

    @Schema(description = "科室 ID", example = "10")
    private Long deptId;

    @Schema(description = "科室名称", example = "内科")
    private String deptName;

    @Schema(description = "医生 ID", example = "100")
    private Long doctorId;

    @Schema(description = "医生姓名", example = "李医生")
    private String doctorName;

    @Schema(description = "医生职称", example = "主任医师")
    private String doctorTitle;

    @Schema(description = "挂号状态（0=待就诊, 1=已就诊, 2=已取消）", example = "0")
    private Short status;

    @Schema(description = "状态描述", example = "待就诊")
    private String statusDesc;

    @Schema(description = "就诊类型（1=初诊, 2=复诊, 3=急诊）", example = "1")
    private Short visitType;

    @Schema(description = "就诊类型描述", example = "初诊")
    private String visitTypeDesc;

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

    @Schema(description = "是否有病历", example = "true")
    private Boolean hasMedicalRecord;
}
