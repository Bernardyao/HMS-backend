package com.his.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import lombok.Data;

/**
 * 创建收费单数据传输对象
 *
 * <p>用于创建收费单时传递挂号和处方信息，是收费流程的核心数据对象</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>收费单创建</b>：根据挂号和处方信息创建收费记录</li>
 *   <li><b>费用计算</b>：系统根据处方信息自动计算应缴费用</li>
 *   <li><b>业务关联</b>：建立收费单与挂号、处方的关联关系</li>
 *   <li><b>流程控制</b>：控制收费流程，避免重复收费</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>门诊收费</b>：医生开具处方后，患者到收费处缴费</li>
 *   <li><b>费用结算</b>：计算患者的药品费用和诊疗费用</li>
 *   <li><b>医保结算</b>：与医保系统集成，计算报销金额</li>
 *   <li><b>退费处理</b>：处理退费申请和退费记录</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：registrationId（挂号记录ID）为必填字段</li>
 *   <li><b>业务验证</b>：挂号记录必须存在且状态为已就诊</li>
 *   <li><b>数据验证</b>：处方ID列表中的处方必须属于该挂号记录</li>
 *   <li><b>状态验证</b>：处方必须是未收费状态，不能重复收费</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>一个挂号单可以创建多张收费单（分批收费）</li>
 *   <li>一张处方只能收费一次</li>
 *   <li>收费单创建后进入待支付状态</li>
 *   <li>收费完成后自动触发库存扣减</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "创建收费单请求对象")
public class CreateChargeDTO {

    /**
     * 挂号记录ID
     *
     * <p>患者就诊的挂号记录唯一标识，关联收费单与就诊记录</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：必须为有效的长整型数值</li>
     *   <li><b>业务规则</b>：必须对应系统中已存在的挂号记录</li>
     *   <li><b>状态要求</b>：挂号状态必须为"已就诊"（已完成诊疗）</li>
     *   <li><b>数据约束</b>：不能重复创建同一挂号的收费单（除非部分收费）</li>
     * </ul>
     *
     * <p><b>业务说明：</b></p>
     * <ul>
     *   <li>收费单必须关联到有效的挂号记录</li>
     *   <li>系统会验证挂号记录的有效性和状态</li>
     *   <li>患者的个人信息从挂号记录中获取</li>
     * </ul>
     */
    @NotNull(message = "挂号记录ID不能为空")
    @Schema(description = "挂号记录ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    /**
     * 处方ID列表
     *
     * <p>需要收费的处方ID集合，用于指定本次收费包含哪些处方</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，为空时收取该挂号的所有费用</li>
     *   <li><b>格式要求</b>：长整型列表，不能为null</li>
     *   <li><b>业务规则</b>：处方ID必须属于该挂号记录</li>
     *   <li><b>状态验证</b>：处方必须存在且为未收费状态</li>
     *   <li><b>去重验证</b>：列表中不能有重复的处方ID</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li><b>传空列表</b>：收取该挂号的所有未收费处方</li>
     *   <li><b>传具体ID</b>：只收费指定的处方（部分收费场景）</li>
     *   <li><b>多次收费</b>：可多次创建收费单，但不能重复收费同一处方</li>
     * </ul>
     *
     * <p><b>业务场景：</b></p>
     * <ul>
     *   <li><b>一次性收费</b>：传入所有处方ID，一次性完成收费</li>
     *   <li><b>分批收费</b>：分多次创建收费单，每次收费部分处方</li>
     *   <li><b>补充收费</b>：医生补充处方后，再次创建收费单</li>
     * </ul>
     *
     * <p><b>示例</b>：[10, 11, 12] 表示收费这三张处方</p>
     */
    @Schema(description = "处方ID列表", example = "[10, 11]")
    private List<Long> prescriptionIds;
}
