package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求 DTO
 */
@Data
@Schema(description = "支付请求对象")
public class PaymentDTO {

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式（1=现金, 2=银行卡, 3=微信, 4=支付宝, 5=医保）", requiredMode = RequiredMode.REQUIRED, example = "3")
    private Short paymentMethod;

    @Schema(description = "交易流水号（非现金支付必填）", example = "WX202312271001")
    private String transactionNo;

    @NotNull(message = "实付金额不能为空")
    @Schema(description = "实付金额", requiredMode = RequiredMode.REQUIRED, example = "156.80")
    private BigDecimal paidAmount;
}
