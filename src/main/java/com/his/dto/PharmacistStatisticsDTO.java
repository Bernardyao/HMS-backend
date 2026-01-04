package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 药师工作统计数据传输对象
 *
 * <p>用于封装药师工作站的统计信息，包括发药单数、总金额和药品总数等关键指标</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>工作量统计</b>：统计药师当日处理的处方数量</li>
 *   <li><b>金额统计</b>：统计当日发药的总金额，用于财务核算</li>
 *   <li><b>药品统计</b>：统计当日发出的药品总数，用于库存分析</li>
 *   <li><b>绩效考核</b>：为药师工作绩效考核提供数据支持</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>药师工作站</b>：药师查看当日工作量和统计数据</li>
 *   <li><b>管理报表</b>：生成药房工作报表和财务报表</li>
 *   <li><b>绩效评估</b>：评估药师工作绩效和工作量</li>
 *   <li><b>数据分析</b>：分析药品发放趋势和工作负荷</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>数据类型</b>：所有字段均为数值类型</li>
 *   <li><b>默认值</b>：构造函数初始化所有字段为0</li>
 *   <li><b>非负约束</b>：所有统计数值均为非负数</li>
 *   <li><b>精度要求</b>：金额字段使用BigDecimal确保精度</li>
 * </ul>
 *
 * <h3>统计说明</h3>
 * <ul>
 *   <li><b>统计周期</b>：默认统计当日数据（00:00:00-23:59:59）</li>
 *   <li><b>发药单数</b>：唯一处方ID数量，一个处方可能包含多种药品</li>
 *   <li><b>总金额</b>：所有处方药品金额的累加值</li>
 *   <li><b>药品总数</b>：所有处方中药品数量的累加值</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "药师工作统计信息对象")
public class PharmacistStatisticsDTO {
    /**
     * 今日发药单数
     *
     * <p>药师当日完成发药操作的处方总数，反映药师的工作量</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：自动统计，无需手动填写</li>
     *   <li><b>格式要求</b>：长整型数值</li>
     *   <li><b>取值范围</b>：非负整数（>=0）</li>
     *   <li><b>默认值</b>：0L（未发药时为0）</li>
     * </ul>
     *
     * <p><b>计算规则：</b></p>
     * <ul>
     *   <li>统计当日所有状态为"已发药"的处方记录</li>
     *   <li>按处方ID去重统计</li>
     *   <li>一个处方包含多种药品仍计为1单</li>
     * </ul>
     *
     * <p><b>示例</b>：10（表示当日处理了10张处方）</p>
     */
    @Schema(description = "今日发药单数", example = "10")
    private Long dispensedCount;

    /**
     * 今日发药总金额
     *
     * <p>药师当日发放的所有药品的金额总和，用于财务统计和核算</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：自动计算，无需手动填写</li>
     *   <li><b>格式要求</b>：BigDecimal类型，确保财务数据精度</li>
     *   <li><b>取值范围</b>：非负数（>=0）</li>
     *   <li><b>精度要求</b>：保留2位小数（精确到分）</li>
     *   <li><b>默认值</b>：BigDecimal.ZERO（未发药时为0）</li>
     * </ul>
     *
     * <p><b>计算规则：</b></p>
     * <ul>
     *   <li>累加当日所有已发药处方的金额</li>
     *   <li>处方金额 = Σ(药品单价 × 药品数量)</li>
     *   <li>使用BigDecimal确保运算精度</li>
     * </ul>
     *
     * <p><b>示例</b>：1234.50（表示当日发药总金额为1234元5角）</p>
     */
    @Schema(description = "今日发药总金额", example = "1234.50")
    private BigDecimal totalAmount;

    /**
     * 今日发药药品总数
     *
     * <p>药师当日发放的药品总数量，反映药品流通量和药师工作负荷</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：自动统计，无需手动填写</li>
     *   <li><b>格式要求</b>：长整型数值</li>
     *   <li><b>取值范围</b>：非负整数（>=0）</li>
     *   <li><b>默认值</b>：0L（未发药时为0）</li>
     * </ul>
     *
     * <p><b>计算规则：</b></p>
     * <ul>
     *   <li>累加当日所有处方明细中的药品数量</li>
     *   <li>一个处方包含5种药品，每种2盒，则计为10</li>
     *   <li>不同计量单位的药品分别统计后求和</li>
     * </ul>
     *
     * <p><b>示例</b>：25（表示当日共发出25个单位数量的药品）</p>
     */
    @Schema(description = "今日发药药品总数", example = "25")
    private Long totalItems;

    /**
     * 默认构造函数
     *
     * <p>创建一个统计对象，所有统计字段初始化为0</p>
     *
     * <p>适用于未开始工作时的初始化场景</p>
     */
    public PharmacistStatisticsDTO() {
        this.dispensedCount = 0L;
        this.totalAmount = BigDecimal.ZERO;
        this.totalItems = 0L;
    }

    /**
     * 全参构造函数
     *
     * <p>创建一个指定统计值的对象，用于接收统计结果</p>
     *
     * @param dispensedCount 发药单数，传入null时自动设为0L
     * @param totalAmount 发药总金额，传入null时自动设为BigDecimal.ZERO
     * @param totalItems 药品总数，传入null时自动设为0L
     */
    public PharmacistStatisticsDTO(Long dispensedCount, BigDecimal totalAmount, Long totalItems) {
        this.dispensedCount = dispensedCount != null ? dispensedCount : 0L;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.totalItems = totalItems != null ? totalItems : 0L;
    }
}
