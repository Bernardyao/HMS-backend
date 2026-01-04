package com.his.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品视图对象（统一版本，支持JsonView）
 *
 * <p>用于药品管理界面和开方界面，封装药品的完整信息返回给前端</p>
 * <p>使用JsonView控制不同角色可见的字段，避免敏感信息泄露</p>
 *
 * <h3>JsonView分层</h3>
 * <ul>
 *   <li><b>Public</b>: 公共视图 - 所有认证用户可见（基础信息）</li>
 *   <li><b>Doctor</b>: 医生视图 - 医生和药师可见（含用法用量等）</li>
 *   <li><b>Pharmacist</b>: 药师视图 - 仅药师可见（含进货价等敏感信息）</li>
 * </ul>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品管理</b>：提供药品列表、药品详情的完整数据</li>
 *   <li><b>开方选药</b>：医生开处方时选择药品</li>
 *   <li><b>库存管理</b>：显示药品库存信息</li>
 *   <li><b>药品信息</b>：包含药品的规格、剂型、生产厂家等详细信息</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药品视图对象")
public class MedicineVO {

    // ========== Public视图 - 所有角色可见 ==========

    /**
     * 药品ID
     */
    
    @Schema(description = "药品ID", example = "1")
    private Long mainId;

    /**
     * 药品编码
     */
    
    @Schema(description = "药品编码", example = "MED001")
    private String medicineCode;

    /**
     * 药品名称
     */
    
    @Schema(description = "药品名称", example = "阿莫西林胶囊")
    private String name;

    /**
     * 通用名称
     */
    
    @Schema(description = "通用名称", example = "阿莫西林")
    private String genericName;

    /**
     * 零售价格
     */
    
    @Schema(description = "零售价格（元）", example = "25.80")
    private BigDecimal retailPrice;

    /**
     * 库存数量
     */
    
    @Schema(description = "库存数量", example = "100")
    private Integer stockQuantity;

    /**
     * 药品分类
     */
    
    @Schema(description = "药品分类", example = "抗生素")
    private String category;

    /**
     * 是否处方药
     *
     * <p><b>类型说明：</b></p>
     * <ul>
     *   <li>使用 Integer 而非 Short，保证与 JSON 序列化的一致性</li>
     *   <li>JavaScript 中的数字类型统一为 Number，避免类型转换问题</li>
     *   <li>测试断言可直接使用整数字面量，无需强制类型转换</li>
     * </ul>
     *
     * <p><b>取值范围：</b></p>
     * <ul>
     *   <li>0 = 非处方药</li>
     *   <li>1 = 处方药</li>
     * </ul>
     */

    @Schema(description = "是否处方药（0=否, 1=是）", example = "1")
    private Integer isPrescription;

    /**
     * 状态
     *
     * <p><b>类型说明：</b></p>
     * <ul>
     *   <li>使用 Integer 而非 Short，保证与 JSON 序列化的一致性</li>
     *   <li>JavaScript 中的数字类型统一为 Number，避免类型转换问题</li>
     *   <li>测试断言可直接使用整数字面量，无需强制类型转换</li>
     * </ul>
     *
     * <p><b>取值范围：</b></p>
     * <ul>
     *   <li>0 = 停用</li>
     *   <li>1 = 启用</li>
     * </ul>
     */

    @Schema(description = "状态（0=停用, 1=启用）", example = "1")
    private Integer status;

    // ========== Doctor视图 - 医生和药师可见 ==========

    /**
     * 规格
     */
    
    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    /**
     * 单位
     */
    
    @Schema(description = "单位", example = "盒")
    private String unit;

    /**
     * 剂型
     */
    
    @Schema(description = "剂型", example = "胶囊")
    private String dosageForm;

    /**
     * 生产厂家
     */
    
    @Schema(description = "生产厂家", example = "XX制药")
    private String manufacturer;

    /**
     * 库存状态（自动计算）
     */
    
    @Schema(description = "库存状态", example = "IN_STOCK",
            allowableValues = {"IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"})
    private String stockStatus;

    // ========== Pharmacist视图 - 仅药师可见（敏感信息）==========

    /**
     * 进货价格（敏感）
     */
    
    @Schema(description = "进货价格（元）", example = "18.50")
    private BigDecimal purchasePrice;

    /**
     * 最低库存（预警线）
     */
    
    @Schema(description = "最低库存（预警线）", example = "50")
    private Integer minStock;

    /**
     * 最高库存（上限）
     */
    
    @Schema(description = "最高库存（上限）", example = "500")
    private Integer maxStock;

    /**
     * 储存条件
     */
    
    @Schema(description = "储存条件", example = "密闭，在阴凉干燥处保存")
    private String storageCondition;

    /**
     * 批准文号
     */
    
    @Schema(description = "批准文号", example = "国药准字H12345678")
    private String approvalNo;

    /**
     * 过期预警天数
     */
    
    @Schema(description = "过期预警天数", example = "90")
    private Integer expiryWarningDays;

    /**
     * 利润率（%）
     */
    
    @Schema(description = "利润率（%）", example = "39.46")
    private BigDecimal profitMargin;

    /**
     * 创建时间
     */
    
    @Schema(description = "创建时间", example = "2023-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    
    @Schema(description = "更新时间", example = "2023-12-01T15:30:00")
    private LocalDateTime updatedAt;
}
