package com.his.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 药师工作统计 DTO
 */
@Data
public class PharmacistStatisticsDTO {
    /**
     * 今日发药单数
     */
    private Long dispensedCount;

    /**
     * 今日发药总金额
     */
    private BigDecimal totalAmount;

    /**
     * 今日发药药品总数
     */
    private Long totalItems;

    public PharmacistStatisticsDTO() {
        this.dispensedCount = 0L;
        this.totalAmount = BigDecimal.ZERO;
        this.totalItems = 0L;
    }
    
    public PharmacistStatisticsDTO(Long dispensedCount, BigDecimal totalAmount, Long totalItems) {
        this.dispensedCount = dispensedCount != null ? dispensedCount : 0L;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.totalItems = totalItems != null ? totalItems : 0L;
    }
}
