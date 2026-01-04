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
 *
 * <p>用于药品管理界面和开方界面，封装药品的完整信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品管理</b>：提供药品列表、药品详情的完整数据</li>
 *   <li><b>开方选药</b>：医生开处方时选择药品</li>
 *   <li><b>库存管理</b>：显示药品库存信息</li>
 *   <li><b>药品信息</b>：包含药品的规格、剂型、生产厂家等详细信息</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Medicine} 实体转换而来，包含药品的完整信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>药品管理</b>：管理员查看和管理药品信息</li>
 *   <li><b>开方选药</b>：医生开处方时查询和选择药品</li>
 *   <li><b>药品查询</b>：按条件查询药品信息</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>retailPrice字段</b>：BigDecimal类型，精度为2位小数，单位为元</li>
 *   <li><b>stockQuantity字段</b>：实时库存数量，开方时会检查库存</li>
 *   <li><b>isPrescription字段</b>：标识是否为处方药，影响处方开具流程</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药品视图对象")
public class MedicineVO {

    /**
     * 药品ID
     *
     * <p>药品在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "药品ID", example = "1")
    private Long mainId;

    /**
     * 药品编码
     *
     * <p>药品的业务编码，用于药品识别和管理</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："MED001"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "药品编码", example = "MED001")
    private String medicineCode;

    /**
     * 药品名称
     *
     * <p>药品的商品名称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-100字符</li>
     *   <li>示例："阿莫西林胶囊"</li>
     * </ul>
     */
    @Schema(description = "药品名称", example = "阿莫西林胶囊")
    private String name;

    /**
     * 通用名称
     *
     * <p>药品的通用名（化学名）</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-100字符</li>
     *   <li>示例："阿莫西林"</li>
     * </ul>
     */
    @Schema(description = "通用名称", example = "阿莫西林")
    private String genericName;

    /**
     * 零售价格
     *
     * <p>药品的零售价格，单位为元</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：12.50（12.5元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "零售价格", example = "12.50")
    private BigDecimal retailPrice;

    /**
     * 库存数量
     *
     * <p>药品当前库存数量</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Integer</li>
     *   <li>最小值：0</li>
     *   <li>示例：1000</li>
     *   <li>单位：与unit字段一致</li>
     * </ul>
     */
    @Schema(description = "库存数量", example = "1000")
    private Integer stockQuantity;

    /**
     * 状态
     *
     * <p>药品的启用状态</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=停用, 1=启用</li>
     *   <li>示例：1（启用）</li>
     * </ul>
     */
    @Schema(description = "状态（0=停用, 1=启用）", example = "1")
    private Short status;

    /**
     * 规格
     *
     * <p>药品的规格说明</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："0.25g*24粒"</li>
     * </ul>
     */
    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    /**
     * 单位
     *
     * <p>药品的计量单位</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-10字符</li>
     *   <li>示例："盒"、"瓶"、"袋"</li>
     * </ul>
     */
    @Schema(description = "单位", example = "盒")
    private String unit;

    /**
     * 剂型
     *
     * <p>药品的剂型</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-20字符</li>
     *   <li>示例："胶囊"、"片剂"、"注射液"</li>
     * </ul>
     */
    @Schema(description = "剂型", example = "胶囊")
    private String dosageForm;

    /**
     * 生产厂家
     *
     * <p>药品的生产企业名称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-100字符</li>
     *   <li>示例："XX制药"</li>
     * </ul>
     */
    @Schema(description = "生产厂家", example = "XX制药")
    private String manufacturer;

    /**
     * 药品分类
     *
     * <p>药品的分类归属</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："抗生素"、"解热镇痛药"</li>
     * </ul>
     */
    @Schema(description = "药品分类", example = "抗生素")
    private String category;

    /**
     * 是否处方药
     *
     * <p>标识药品是否需要处方才能购买</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=否, 1=是</li>
     *   <li>示例：1（是处方药）</li>
     * </ul>
     */
    @Schema(description = "是否处方药（0=否, 1=是）", example = "1")
    private Short isPrescription;

    /**
     * 创建时间
     *
     * <p>药品信息创建的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T10:00:00"</li>
     * </ul>
     */
    @Schema(description = "创建时间", example = "2025-12-20T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * <p>药品信息最后更新的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T10:00:00"</li>
     * </ul>
     */
    @Schema(description = "更新时间", example = "2025-12-20T10:00:00")
    private LocalDateTime updatedAt;
}
