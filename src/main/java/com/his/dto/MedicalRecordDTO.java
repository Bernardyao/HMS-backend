package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

/**
 * 病历 DTO（接收前端参数）
 */
@Data
@Schema(description = "病历数据传输对象")
public class MedicalRecordDTO {

    @Schema(description = "挂号单ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    @Schema(description = "主诉", example = "头痛、发热3天")
    private String chiefComplaint;

    @Schema(description = "现病史", example = "患者3天前无明显诱因出现头痛...")
    private String presentIllness;

    @Schema(description = "既往史", example = "既往体健，无慢性病史")
    private String pastHistory;

    @Schema(description = "个人史", example = "无吸烟饮酒史")
    private String personalHistory;

    @Schema(description = "家族史", example = "无遗传病史")
    private String familyHistory;

    @Schema(description = "体格检查", example = "T: 38.5°C, P: 90次/分...")
    private String physicalExam;

    @Schema(description = "辅助检查", example = "血常规：WBC 12.5×10^9/L")
    private String auxiliaryExam;

    @Schema(description = "诊断", example = "上呼吸道感染")
    private String diagnosis;

    @Schema(description = "诊断编码", example = "J06.9")
    private String diagnosisCode;

    @Schema(description = "治疗方案", example = "1. 抗感染治疗 2. 对症处理")
    private String treatmentPlan;

    @Schema(description = "医嘱", example = "注意休息，多饮水")
    private String doctorAdvice;

    @Schema(description = "状态（0=草稿, 1=已提交, 2=已审核）", example = "1")
    private Short status;
}
