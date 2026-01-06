package com.his.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处方视图对象
 *
 * <p>用于医生工作站和药房管理，封装处方信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>处方开具</b>：医生为患者开具处方</li>
 *   <li><b>处方审核</b>：药师审核处方的合理性</li>
 *   <li><b>处方发药</b>：药房根据处方发放药品</li>
 *   <li><b>处方查询</b>：查询处方历史记录</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Prescription} 实体转换而来，关联病历、患者、医生和药品信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生工作站</b>：医生开具处方</li>
 *   <li><b>药房管理</b>：药师审核和发药</li>
 *   <li><b>收费管理</b>：处方收费</li>
 *   <li><b>处方查询</b>：查询处方记录</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>状态流转</b>：草稿(0) -> 已开方(1) -> 已审核(2) -> 已发药(3)</li>
 *   <li><b>处方类型</b>：1=西药, 2=中药, 3=中成药</li>
 *   <li><b>validityDays</b>：处方有效天数，超过有效期后不能发药</li>
 *   <li><b>totalAmount</b>：BigDecimal类型，精度为2位小数，单位为元</li>
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
@Schema(description = "处方视图对象")
public class PrescriptionVO {

    /**
     * 处方ID
     *
     * <p>处方在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：100</li>
     * </ul>
     */
    @Schema(description = "处方ID", example = "100")
    private Long mainId;

    /**
     * 处方编号
     *
     * <p>处方的业务编号，系统自动生成</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：PRE+yyyyMMddHHmmss+随机数</li>
     *   <li>长度：固定24位</li>
     *   <li>示例："PRE202512201533456789123"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "处方编号", example = "PRE202512201533456789123")
    private String prescriptionNo;

    /**
     * 病历ID
     *
     * <p>关联的病历记录ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "病历ID", example = "1")
    private Long recordId;

    /**
     * 患者ID
     *
     * <p>处方所属患者的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "患者ID", example = "1")
    private Long patientId;

    /**
     * 患者姓名
     *
     * <p>处方所属患者的姓名</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："张三"</li>
     * </ul>
     */
    @Schema(description = "患者姓名", example = "张三")
    private String patientName;

    /**
     * 医生ID
     *
     * <p>开具处方的医生ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "医生ID", example = "1")
    private Long doctorId;

    /**
     * 医生姓名
     *
     * <p>开具处方的医生姓名</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："李医生"</li>
     * </ul>
     */
    @Schema(description = "医生姓名", example = "李医生")
    private String doctorName;

    /**
     * 处方类型
     *
     * <p>处方药品的类型</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：1=西药, 2=中药, 3=中成药</li>
     *   <li>示例：1（西药）</li>
     * </ul>
     */
    @Schema(description = "处方类型（1=西药, 2=中药, 3=中成药）",
            example = "1",
            allowableValues = {"1", "2", "3"})
    private Short prescriptionType;

    /**
     * 总金额
     *
     * <p>处方的总金额，所有药品费用之和</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：33.80（33.8元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "总金额", example = "33.80")
    private BigDecimal totalAmount;

    /**
     * 药品总数量
     *
     * <p>处方中药品的总数量</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Integer</li>
     *   <li>最小值：1</li>
     *   <li>示例：3（3种药品）</li>
     * </ul>
     */
    @Schema(description = "药品总数量", example = "3")
    private Integer itemCount;

    /**
     * 状态
     *
     * <p>处方的当前状态</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=草稿, 1=已开方, 2=已审核, 3=已发药, 4=已退费, 5=已缴费</li>
     *   <li>示例：1（已开方）</li>
     * </ul>
     */
    @Schema(description = "状态（0=草稿, 1=已开方, 2=已审核, 3=已发药, 4=已退费, 5=已缴费）",
            example = "1",
            allowableValues = {"0", "1", "2", "3", "4", "5"})
    private Short status;

    /**
     * 有效天数
     *
     * <p>处方的有效期，超过此天数后不能发药</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Integer</li>
     *   <li>最小值：1</li>
     *   <li>示例：3（3天）</li>
     *   <li>单位：天</li>
     * </ul>
     */
    @Schema(description = "有效天数", example = "3")
    private Integer validityDays;

    /**
     * 审核医生ID
     *
     * <p>审核处方的药师ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：否（未审核时为null）</li>
     *   <li>示例：2</li>
     * </ul>
     */
    @Schema(description = "审核医生ID", example = "2")
    private Long reviewDoctorId;

    /**
     * 审核医生姓名
     *
     * <p>审核处方的药师姓名</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："王药师"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "审核医生姓名", example = "王药师")
    private String reviewDoctorName;

    /**
     * 审核时间
     *
     * <p>处方审核的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T16:00:00"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "审核时间", example = "2025-12-20T16:00:00")
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     *
     * <p>审核时的备注信息</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-500字符</li>
     *   <li>示例："处方合理，准予发药"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "审核备注", example = "处方合理，准予发药")
    private String reviewRemark;

    /**
     * 发药时间
     *
     * <p>处方发药的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T16:30:00"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "发药时间", example = "2025-12-20T16:30:00")
    private LocalDateTime dispenseTime;

    /**
     * 发药人ID
     *
     * <p>发药人员的ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：否（未发药时为null）</li>
     *   <li>示例：3</li>
     * </ul>
     */
    @Schema(description = "发药人ID", example = "3")
    private Long dispenseBy;

    /**
     * 创建时间
     *
     * <p>处方创建的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T15:34:00"</li>
     * </ul>
     */
    @Schema(description = "创建时间", example = "2025-12-20T15:34:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * <p>处方最后更新的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T15:34:00"</li>
     * </ul>
     */
    @Schema(description = "更新时间", example = "2025-12-20T15:34:00")
    private LocalDateTime updatedAt;

    /**
     * 处方明细列表
     *
     * <p>处方包含的所有药品明细</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：List&lt;PrescriptionDetailVO&gt;</li>
     *   <li>可以为空列表</li>
     * </ul>
     */
    @Schema(description = "处方明细列表")
    private List<PrescriptionDetailVO> details;

    /**
     * 处方明细视图对象
     *
     * <p>处方中的单个药品明细信息</p>
     *
     * @author HIS 开发团队
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "处方明细视图对象")
    public static class PrescriptionDetailVO {

        /**
         * 明细ID
         *
         * <p>处方明细在数据库中的唯一标识</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Long</li>
         *   <li>必填：是</li>
         *   <li>唯一：是</li>
         *   <li>示例：1</li>
         * </ul>
         */
        @Schema(description = "明细ID", example = "1")
        private Long mainId;

        /**
         * 药品ID
         *
         * <p>药品的唯一标识</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Long</li>
         *   <li>必填：是</li>
         *   <li>示例：1</li>
         * </ul>
         */
        @Schema(description = "药品ID", example = "1")
        private Long medicineId;

        /**
         * 药品名称
         *
         * <p>药品的名称</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：1-100字符</li>
         *   <li>示例："阿莫西林胶囊"</li>
         * </ul>
         */
        @Schema(description = "药品名称", example = "阿莫西林胶囊")
        private String medicineName;

        /**
         * 单价
         *
         * <p>药品的单价</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：BigDecimal</li>
         *   <li>精度：2位小数</li>
         *   <li>示例：12.50（12.5元）</li>
         *   <li>单位：元</li>
         * </ul>
         */
        @Schema(description = "单价", example = "12.50")
        private BigDecimal unitPrice;

        /**
         * 数量
         *
         * <p>药品的数量</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Integer</li>
         *   <li>最小值：1</li>
         *   <li>示例：2</li>
         *   <li>单位：与药品单位一致</li>
         * </ul>
         */
        @Schema(description = "数量", example = "2")
        private Integer quantity;

        /**
         * 小计
         *
         * <p>该药品的费用小计（单价 × 数量）</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：BigDecimal</li>
         *   <li>精度：2位小数</li>
         *   <li>示例：25.00（25元）</li>
         *   <li>单位：元</li>
         * </ul>
         */
        @Schema(description = "小计", example = "25.00")
        private BigDecimal subtotal;

        /**
         * 用药频率
         *
         * <p>药品的使用频率</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：1-50字符</li>
         *   <li>示例："一日三次"</li>
         *   <li>可以为空字符串</li>
         * </ul>
         */
        @Schema(description = "用药频率", example = "一日三次")
        private String frequency;

        /**
         * 用量
         *
         * <p>每次的用药量</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：1-50字符</li>
         *   <li>示例："每次2片"</li>
         *   <li>可以为空字符串</li>
         * </ul>
         */
        @Schema(description = "用量", example = "每次2片")
        private String dosage;

        /**
         * 用药途径
         *
         * <p>药品的使用方式</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：1-20字符</li>
         *   <li>示例："口服"、"静脉注射"</li>
         *   <li>可以为空字符串</li>
         * </ul>
         */
        @Schema(description = "用药途径", example = "口服")
        private String route;

        /**
         * 用药天数
         *
         * <p>药品的使用天数</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Integer</li>
         *   <li>最小值：1</li>
         *   <li>示例：7（7天）</li>
         *   <li>单位：天</li>
         * </ul>
         */
        @Schema(description = "用药天数", example = "7")
        private Integer days;

        /**
         * 用药说明
         *
         * <p>药品使用的特殊说明</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：0-500字符</li>
         *   <li>示例："饭后服用"</li>
         *   <li>可以为空字符串</li>
         * </ul>
         */
        @Schema(description = "用药说明", example = "饭后服用")
        private String instructions;
    }
}
