package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 每日结算报表视图对象
 *
 * <p>用于财务管理和报表统计，封装每日收费结算信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>日报统计</b>：统计每日的收费数据</li>
 *   <li><b>支付分析</b>：按支付方式分解收费金额</li>
 *   <li><b>退费统计</b>：统计退费笔数和金额</li>
 *   <li><b>财务对账</b>：提供财务对账所需的数据</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从收费记录、退费记录汇总统计而来</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>财务管理</b>：财务人员查看每日结算报表</li>
 *   <li><b>收费统计</b>：统计收费员的收费业绩</li>
 *   <li><b>财务对账</b>：与实际收款进行对账</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>netCollection</b>：BigDecimal类型，精度为2位小数，单位为元</li>
 *   <li><b>paymentBreakdown</b>：Map结构，key为支付方式，value为该支付方式的统计</li>
 *   <li><b>计算公式</b>：实收净额 = 总应收金额 - 退费金额</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "每日结算报表")
public class DailySettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报表日期
     *
     * <p>结算报表对应的日期</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDate</li>
     *   <li>格式：yyyy-MM-dd</li>
     *   <li>示例："2023-12-01"</li>
     * </ul>
     */
    @Schema(description = "报表日期")
    private LocalDate date;

    /**
     * 收费员姓名
     *
     * <p>负责收费的收费员姓名</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："张收费员"</li>
     *   <li>可以为空（汇总报表时）</li>
     * </ul>
     */
    @Schema(description = "收费员姓名")
    private String cashierName;

    /**
     * 总收费笔数
     *
     * <p>当日收费的总笔数</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>最小值：0</li>
     *   <li>示例：150（150笔）</li>
     * </ul>
     */
    @Schema(description = "总收费笔数")
    private Long totalCharges;

    /**
     * 总应收金额
     *
     * <p>当日所有收费的总金额</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：15680.50（15680.5元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "总应收金额")
    private BigDecimal totalAmount;

    /**
     * 支付方式分解
     *
     * <p>按支付方式统计的收费明细，key为支付方式，value为该支付方式的统计数据</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Map&lt;String, PaymentBreakdownVO&gt;</li>
     *   <li>key示例：CASH（现金）、CARD（刷卡）、ALIPAY（支付宝）、WECHAT（微信）</li>
     *   <li>value包含：count（笔数）、amount（金额）</li>
     *   <li>可以为空Map</li>
     * </ul>
     */
    @Schema(description = "支付方式分解")
    private Map<String, PaymentBreakdownVO> paymentBreakdown;

    /**
     * 退费统计
     *
     * <p>当日退费的统计数据</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：RefundStatsVO</li>
     *   <li>包含：count（退费笔数）、amount（退费金额）</li>
     *   <li>可以为null</li>
     * </ul>
     */
    @Schema(description = "退费统计")
    private RefundStatsVO refunds;

    /**
     * 实收净额
     *
     * <p>扣除退费后的实际收款金额（总金额 - 退费金额）</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：15230.50（15230.5元）</li>
     *   <li>单位：元</li>
     *   <li>计算公式：totalAmount - refunds.amount</li>
     * </ul>
     */
    @Schema(description = "实收净额 (总金额 - 退费金额)")
    private BigDecimal netCollection;

    /**
     * 支付方式分解视图对象
     *
     * <p>某种支付方式的统计数据</p>
     *
     * @author HIS 开发团队
     * @version 1.0
     * @since 1.0
     */
    @Data
    public static class PaymentBreakdownVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 收费笔数
         *
         * <p>该支付方式的收费笔数</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Long</li>
         *   <li>最小值：0</li>
         *   <li>示例：50（50笔）</li>
         * </ul>
         */
        private Long count;

        /**
         * 收费金额
         *
         * <p>该支付方式的收费总额</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：BigDecimal</li>
         *   <li>精度：2位小数</li>
         *   <li>示例：5000.00（5000元）</li>
         *   <li>单位：元</li>
         * </ul>
         */
        private BigDecimal amount;
    }

    /**
     * 退费统计视图对象
     *
     * <p>退费数据的统计信息</p>
     *
     * @author HIS 开发团队
     * @version 1.0
     * @since 1.0
     */
    @Data
    public static class RefundStatsVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 退费笔数
         *
         * <p>当日退费的总笔数</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：Long</li>
         *   <li>最小值：0</li>
         *   <li>示例：5（5笔）</li>
         * </ul>
         */
        private Long count;

        /**
         * 退费金额
         *
         * <p>当日退费的总金额</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：BigDecimal</li>
         *   <li>精度：2位小数</li>
         *   <li>示例：450.00（450元）</li>
         *   <li>单位：元</li>
         * </ul>
         */
        private BigDecimal amount;
    }
}
