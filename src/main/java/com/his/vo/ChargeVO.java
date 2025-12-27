package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费响应 VO
 */
@Data
@Schema(description = "收费信息")
public class ChargeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收费 ID", example = "1")
    private Long id;

    @Schema(description = "收费单号", example = "CHG20231201001")
    private String chargeNo;

    @Schema(description = "患者 ID", example = "1001")
    private Long patientId;

    @Schema(description = "患者姓名", example = "张三")
    private String patientName;

    @Schema(description = "应收总金额", example = "156.80")
    private BigDecimal totalAmount;

    @Schema(description = "收费状态（0=未缴费, 1=已缴费, 2=已退费）", example = "0")
    private Short status;

    @Schema(description = "状态描述", example = "未缴费")
    private String statusDesc;

    @Schema(description = "收费明细列表")
    private List<ChargeDetailVO> details;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 收费明细 VO
     */
    @Data
    @Schema(description = "收费明细信息")
    public static class ChargeDetailVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "项目类型", example = "PRESCRIPTION")
        private String itemType;

        @Schema(description = "项目名称", example = "阿莫西林")
        private String itemName;

        @Schema(description = "项目金额", example = "56.80")
        private BigDecimal itemAmount;
    }
}
