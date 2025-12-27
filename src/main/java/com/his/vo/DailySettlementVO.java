package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 每日结算报表 VO
 */
@Data
@Schema(description = "每日结算报表")
public class DailySettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报表日期")
    private LocalDate date;

    @Schema(description = "收费员姓名")
    private String cashierName;

    @Schema(description = "总收费笔数")
    private Long totalCharges;

    @Schema(description = "总应收金额")
    private BigDecimal totalAmount;

    @Schema(description = "支付方式分解")
    private Map<String, PaymentBreakdownVO> paymentBreakdown;

    @Schema(description = "退费统计")
    private RefundStatsVO refunds;

    @Schema(description = "实收净额 (总金额 - 退费金额)")
    private BigDecimal netCollection;

    @Data
    public static class PaymentBreakdownVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    public static class RefundStatsVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long count;
        private BigDecimal amount;
    }
}
