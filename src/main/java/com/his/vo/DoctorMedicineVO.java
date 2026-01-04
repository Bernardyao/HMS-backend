package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 药品视图对象（医生工作站）
 * <p>
 * 为医生工作站提供药品查询视图，包含医生开处方时需要的信息。
 * 相比药师视图，医生视图不包含进货价等敏感商业信息。
 * </p>
 *
 * <h3>主要字段</h3>
 * <ul>
 *   <li><b>基本信息</b>：药品编码、名称、通用名、规格、剂型等</li>
 *   <li><b>价格信息</b>：仅包含零售价，不包含进货价</li>
 *   <li><b>库存信息</b>：库存数量 + 自动计算的库存状态</li>
 *   <li><b>分类信息</b>：药品分类、是否处方药</li>
 *   <li><b>其他信息</b>：生产厂家、状态</li>
 * </ul>
 *
 * <h3>库存状态说明</h3>
 * <ul>
 *   <li><b>IN_STOCK</b>: 有货（库存 > 最低库存）</li>
 *   <li><b>LOW_STOCK</b>: 低库存（库存 <= 最低库存，但 > 0）</li>
 *   <li><b>OUT_OF_STOCK</b>: 缺货（库存 = 0）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see PharmacistMedicineVO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药品视图对象（医生工作站）")
public class DoctorMedicineVO {

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
}
