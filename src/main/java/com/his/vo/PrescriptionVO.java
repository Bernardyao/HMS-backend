package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "处方视图对象")
public class PrescriptionVO {

    @Schema(description = "处方ID", example = "100")
    private Long mainId;

    @Schema(description = "处方编号", example = "PRE202512201533456789123")
    private String prescriptionNo;

    @Schema(description = "病历ID", example = "1")
    private Long recordId;

    @Schema(description = "患者ID", example = "1")
    private Long patientId;

    @Schema(description = "患者姓名", example = "张三")
    private String patientName;

    @Schema(description = "医生ID", example = "1")
    private Long doctorId;

    @Schema(description = "医生姓名", example = "李医生")
    private String doctorName;

    @Schema(description = "处方类型（1=西药, 2=中药, 3=中成药）", example = "1")
    private Short prescriptionType;

    @Schema(description = "总金额", example = "33.80")
    private BigDecimal totalAmount;

    @Schema(description = "药品总数量", example = "3")
    private Integer itemCount;

    @Schema(description = "状态（0=草稿, 1=已开方, 2=已审核, 3=已发药）", example = "1")
    private Short status;

    @Schema(description = "有效天数", example = "3")
    private Integer validityDays;

    @Schema(description = "审核医生ID", example = "2")
    private Long reviewDoctorId;

    @Schema(description = "审核医生姓名", example = "王药师")
    private String reviewDoctorName;

    @Schema(description = "审核时间", example = "2025-12-20T16:00:00")
    private LocalDateTime reviewTime;

    @Schema(description = "审核备注", example = "处方合理，准予发药")
    private String reviewRemark;

    @Schema(description = "发药时间", example = "2025-12-20T16:30:00")
    private LocalDateTime dispenseTime;

    @Schema(description = "发药人ID", example = "3")
    private Long dispenseBy;

    @Schema(description = "创建时间", example = "2025-12-20T15:34:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2025-12-20T15:34:00")
    private LocalDateTime updatedAt;

    @Schema(description = "处方明细列表")
    private List<PrescriptionDetailVO> details;

    /**
     * 处方明细视图对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "处方明细视图对象")
    public static class PrescriptionDetailVO {

        @Schema(description = "明细ID", example = "1")
        private Long mainId;

        @Schema(description = "药品ID", example = "1")
        private Long medicineId;

        @Schema(description = "药品名称", example = "阿莫西林胶囊")
        private String medicineName;

        @Schema(description = "单价", example = "12.50")
        private BigDecimal unitPrice;

        @Schema(description = "数量", example = "2")
        private Integer quantity;

        @Schema(description = "小计", example = "25.00")
        private BigDecimal subtotal;

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
