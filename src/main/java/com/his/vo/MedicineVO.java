package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药品视图对象")
public class MedicineVO {

    @Schema(description = "药品ID", example = "1")
    private Long mainId;

    @Schema(description = "药品编码", example = "MED001")
    private String medicineCode;

    @Schema(description = "药品名称", example = "阿莫西林胶囊")
    private String name;

    @Schema(description = "通用名称", example = "阿莫西林")
    private String genericName;

    @Schema(description = "零售价格", example = "12.50")
    private BigDecimal retailPrice;

    @Schema(description = "库存数量", example = "1000")
    private Integer stockQuantity;

    @Schema(description = "状态（0=停用, 1=启用）", example = "1")
    private Short status;

    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "单位", example = "盒")
    private String unit;

    @Schema(description = "剂型", example = "胶囊")
    private String dosageForm;

    @Schema(description = "生产厂家", example = "XX制药")
    private String manufacturer;

    @Schema(description = "药品分类", example = "抗生素")
    private String category;

    @Schema(description = "是否处方药（0=否, 1=是）", example = "1")
    private Short isPrescription;

    @Schema(description = "创建时间", example = "2025-12-20T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2025-12-20T10:00:00")
    private LocalDateTime updatedAt;
}
