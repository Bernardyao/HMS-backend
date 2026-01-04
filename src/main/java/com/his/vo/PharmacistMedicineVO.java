package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品视图对象（药师工作站）
 * <p>
 * 为药师工作站提供完整的药品信息视图，包含药品管理所需的所有信息。
 * 相比医生视图，药师视图额外包含进货价、库存阈值、储存条件等管理信息。
 * </p>
 *
 * <h3>扩展字段（相比医生视图）</h3>
 * <ul>
 *   <li><b>价格信息</b>：进货价 + 自动计算的利润率</li>
 *   <li><b>库存管理</b>：最低库存、最高库存阈值</li>
 *   <li><b>储存信息</b>：储存条件、批准文号、过期预警天数</li>
 *   <li><b>审计信息</b>：创建时间、更新时间</li>
 * </ul>
 *
 * <h3>利润率计算公式</h3>
 * <pre>
 * 利润率 = ((零售价 - 进货价) / 进货价) × 100%
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see DoctorMedicineVO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药品视图对象（药师工作站-完整信息）")
public class PharmacistMedicineVO {

    // ========== 基础信息（继承自DoctorMedicineVO） ==========

    @Schema(description = "药品ID")
    private Long mainId;

    @Schema(description = "药品编码", example = "MED001")
    private String medicineCode;

    @Schema(description = "药品名称", example = "阿莫西林胶囊")
    private String name;

    @Schema(description = "通用名称", example = "阿莫西林")
    private String genericName;

    @Schema(description = "零售价格（元）", example = "25.80")
    private BigDecimal retailPrice;

    @Schema(description = "库存数量", example = "100")
    private Integer stockQuantity;

    @Schema(description = "库存状态", example = "IN_STOCK",
            allowableValues = {"IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"})
    private String stockStatus;

    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "单位", example = "盒")
    private String unit;

    @Schema(description = "剂型", example = "胶囊")
    private String dosageForm;

    @Schema(description = "药品分类", example = "抗生素")
    private String category;

    @Schema(description = "是否处方药（0=否, 1=是）", example = "1")
    private Short isPrescription;

    @Schema(description = "生产厂家", example = "某某制药有限公司")
    private String manufacturer;

    @Schema(description = "状态（0=停用, 1=启用）", example = "1")
    private Short status;

    // ========== 药师专属扩展字段 ==========

    @Schema(description = "进货价格（元）", example = "18.50")
    private BigDecimal purchasePrice;

    @Schema(description = "最低库存（预警线）", example = "50")
    private Integer minStock;

    @Schema(description = "最高库存（上限）", example = "500")
    private Integer maxStock;

    @Schema(description = "储存条件", example = "密闭，在阴凉干燥处保存")
    private String storageCondition;

    @Schema(description = "批准文号", example = "国药准字H12345678")
    private String approvalNo;

    @Schema(description = "过期预警天数", example = "90")
    private Integer expiryWarningDays;

    @Schema(description = "利润率（%）", example = "39.46")
    private BigDecimal profitMargin;

    @Schema(description = "创建时间", example = "2023-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2023-12-01T15:30:00")
    private LocalDateTime updatedAt;
}
