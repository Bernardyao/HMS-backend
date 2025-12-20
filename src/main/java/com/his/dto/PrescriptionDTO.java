package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

import java.util.List;

/**
 * 处方 DTO（接收前端参数）
 */
@Data
@Schema(description = "处方数据传输对象")
public class PrescriptionDTO {

    @Schema(description = "挂号单ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    @Schema(description = "处方类型（1=西药, 2=中药, 3=中成药）", example = "1")
    private Short prescriptionType;

    @Schema(description = "有效天数", example = "3")
    private Integer validityDays;

    @Schema(description = "药品列表", requiredMode = RequiredMode.REQUIRED)
    private List<PrescriptionItemDTO> items;

    /**
     * 处方明细 DTO
     */
    @Data
    @Schema(description = "处方明细数据传输对象")
    public static class PrescriptionItemDTO {

        @Schema(description = "药品ID", requiredMode = RequiredMode.REQUIRED, example = "1")
        private Long medicineId;

        @Schema(description = "数量", requiredMode = RequiredMode.REQUIRED, example = "10")
        private Integer quantity;

        @Schema(description = "用药频率", example = "一日三次")
        private String frequency;

        @Schema(description = "用量", example = "每次2片")
        private String dosage;

        @Schema(description = "用药途径", example = "口服")
        private String route;

        @Schema(description = "用药天数", example = "7")
        private Integer days;

        @Schema(description = "用药说明", example = "饭后服用")
        private String instructions;
    }
}
