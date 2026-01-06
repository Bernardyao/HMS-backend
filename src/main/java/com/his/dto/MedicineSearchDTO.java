package com.his.dto;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;

import lombok.Data;

/**
 * 药品搜索DTO
 *
 * @author HIS Team
 * @since 2025-01-06
 */
@Data
public class MedicineSearchDTO {

    private String keyword;

    private String category;

    private Short isPrescription;

    private String stockStatus;

    private String manufacturer;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Pageable pageable;
}
