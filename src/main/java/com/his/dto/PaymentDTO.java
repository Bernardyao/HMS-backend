package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求数据传输对象
 *
 * <p>用于收费支付操作时传递支付信息，包括支付方式、交易流水号和实付金额</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>支付处理</b>：处理患者的支付操作，完成收费流程</li>
 *   <li><b>支付方式</b>：支持现金、银行卡、微信、支付宝、医保等多种支付方式</li>
 *   <li><b>交易记录</b>：记录交易流水号，用于对账和追溯</li>
 *   <li><b>金额确认</b>：确认患者实际支付的金额</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>窗口收费</b>：收费员在收费窗口收取患者费用</li>
 *   <li><b>自助缴费</b>：患者在自助机上进行缴费操作</li>
 *   <li><b>移动支付</b>：患者通过手机扫码支付</li>
 *   <li><b>医保结算</b>：医保实时结算，患者支付个人承担部分</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：paymentMethod（支付方式）、paidAmount（实付金额）为必填</li>
 *   <li><b>条件必填</b>：非现金支付时，transactionNo（交易流水号）必填</li>
 *   <li><b>业务验证</b>：支付金额必须大于0且不超过应收金额</li>
 *   <li><b>格式验证</b>：交易流水号格式需符合支付平台规范</li>
 * </ul>
 *
 * <h3>支付方式说明</h3>
 * <ul>
 *   <li><b>1 - 现金</b>：现金支付，无需交易流水号</li>
 *   <li><b>2 - 银行卡</b>：银行卡刷卡或插卡支付</li>
 *   <li><b>3 - 微信</b>：微信扫码支付，需要交易流水号</li>
 *   <li><b>4 - 支付宝</b>：支付宝扫码支付，需要交易流水号</li>
 *   <li><b>5 - 医保</b>：医保卡支付，需要医保交易流水号</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "支付请求对象")
public class PaymentDTO {

    /**
     * 支付方式
     *
     * <p>患者选择的支付渠道类型，决定支付流程和记录方式</p>
     *
     * <p><b>支付方式说明：</b></p>
     * <ul>
     *   <li><b>1 - 现金</b>：现金支付，最传统的方式，不需要交易流水号</li>
     *   <li><b>2 - 银行卡</b>：POS机刷卡支付，需要银行授权码</li>
     *   <li><b>3 - 微信</b>：微信扫码支付，需要微信交易单号</li>
     *   <li><b>4 - 支付宝</b>：支付宝扫码支付，需要支付宝交易号</li>
     *   <li><b>5 - 医保</b>：医保卡实时结算，需要医保交易流水号</li>
     * </ul>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>取值范围</b>：1、2、3、4、5</li>
     *   <li><b>业务规则</b>：支付方式必须在系统配置的支持范围内</li>
     * </ul>
     *
     * <p><b>业务说明：</b></p>
     * <ul>
     *   <li>现金支付：不需要第三方交易流水号</li>
     *   <li>线上支付：必须提供对应的交易流水号</li>
     *   <li>医保支付：需要医保接口返回的交易流水号</li>
     *   <li>支持混合支付：部分现金+部分第三方支付（需要多次调用）</li>
     * </ul>
     */
    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式（1=现金, 2=银行卡, 3=微信, 4=支付宝, 5=医保）",
            requiredMode = RequiredMode.REQUIRED,
            example = "3",
            allowableValues = {"1", "2", "3", "4", "5"})
    private Short paymentMethod;

    /**
     * 交易流水号
     *
     * <p>第三方支付平台或银行返回的交易唯一标识，用于对账和查询</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：非现金支付时必填，现金支付时可不填</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过100字符</li>
     *   <li><b>唯一性要求</b>：每个交易流水号在系统中唯一</li>
     *   <li><b>格式规范</b>：需符合对应支付平台的规范</li>
     * </ul>
     *
     * <p><b>不同支付方式的流水号格式：</b></p>
     * <ul>
     *   <li><b>微信支付</b>：微信返回的transaction_id，如"4200001234567890123456789"</li>
     *   <li><b>支付宝</b>：支付宝返回的trade_no，如"2023122722001xxxxxxxx"</li>
     *   <li><b>银行卡</b>：POS机返回的授权码或参考号</li>
     *   <li><b>医保</b>：医保系统返回的交易流水号</li>
     * </ul>
     *
     * <p><b>业务说明：</b></p>
     * <ul>
     *   <li>用于支付结果查询和状态确认</li>
     *   <li>用于与第三方平台进行对账</li>
     *   <li>用于处理退款和冲正操作</li>
     *   <li>必须准确记录，不能虚构或重复使用</li>
     * </ul>
     *
     * <p><b>示例</b>：WX202312271001、ALIPAY2023122712345678</p>
     */
    @Schema(description = "交易流水号（非现金支付必填）", example = "WX202312271001")
    private String transactionNo;

    /**
     * 实付金额
     *
     * <p>患者实际支付的金额，可能等于应收金额，也可能涉及优惠或减免</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：BigDecimal类型，确保精度</li>
     *   <li><b>取值范围</b>：大于0的数值（>0）</li>
     *   <li><b>精度要求</b>：保留2位小数（精确到分）</li>
     *   <li><b>业务规则</b>：不能超过收费单的应收金额</li>
     * </ul>
     *
     * <p><b>金额说明：</b></p>
     * <ul>
     *   <li><b>正常缴费</b>：实付金额 = 应收金额</li>
     *   <li><b>部分缴费</b>：实付金额 < 应收金额（需要多次支付）</li>
     *   <li><b>优惠减免</b>：享受优惠后实付金额可能低于原价</li>
     *   <li><b>超额支付</b>：一般不允许，系统应拒绝超额支付</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>金额单位为人民币元</li>
     *   <li>使用BigDecimal避免浮点运算误差</li>
     *   <li>支付完成后该金额不可修改</li>
     *   <li>涉及退款时按此金额退回</li>
     * </ul>
     *
     * <p><b>示例</b>：156.80（表示支付156元8角）</p>
     */
    @NotNull(message = "实付金额不能为空")
    @Schema(description = "实付金额", requiredMode = RequiredMode.REQUIRED, example = "156.80")
    private BigDecimal paidAmount;
}
