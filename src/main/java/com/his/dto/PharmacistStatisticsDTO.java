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
    private Integer dispensedCount;

    /**
     * 今日发药总金额
     */
    private BigDecimal totalAmount;

    /**
     * 今日发药药品总数
     */
    private Integer totalItems;

    public PharmacistStatisticsDTO() {
        this.dispensedCount = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.totalItems = 0;
    }
    
    public PharmacistStatisticsDTO(Integer dispensedCount, BigDecimal totalAmount, Integer totalItems) {
        this.dispensedCount = dispensedCount != null ? dispensedCount : 0;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.totalItems = totalItems != null ? totalItems : 0;
    }
}
