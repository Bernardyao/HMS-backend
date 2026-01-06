package com.his.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import lombok.Data;

/**
 * 医疗处方数据传输对象
 *
 * <p>用于封装医生开具的处方信息，包括处方类型、有效期和药品明细</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>处方开具</b>：医生为患者开具处方，记录处方基本信息</li>
 *   <li><b>药品管理</b>：记录处方包含的所有药品及其使用方法</li>
 *   <li><b>分类管理</b>：支持西药、中药、中成药等不同处方类型</li>
 *   <li><b>有效期控制</b>：设置处方有效期，确保药品及时取用</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>门诊诊疗</b>：医生在医生工作站为门诊患者开具处方</li>
 *   <li><b>处方审核</b>：药师对处方进行审核和配药</li>
 *   <li><b>收费结算</b>：根据处方信息计算药品费用</li>
 *   <li><b>处方查询</b>：患者或医生查询历史处方记录</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：registrationId（挂号单ID）、items（药品列表）为必填</li>
 *   <li><b>业务验证</b>：挂号单必须存在且状态有效</li>
 *   <li><b>数据验证</b>：药品列表至少包含一种药品</li>
 *   <li><b>取值范围</b>：处方类型必须是1、2、3之一</li>
 * </ul>
 *
 * <h3>处方类型说明</h3>
 * <ul>
 *   <li><b>1 - 西药</b>：化学药品，如抗生素、止痛药等</li>
 *   <li><b>2 - 中药</b>：传统中药饮片，需要配方煎煮</li>
 *   <li><b>3 - 中成药</b>：中成药制剂，如丸剂、颗粒剂等</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "医疗处方数据传输对象")
public class PrescriptionDTO {

    /**
     * 挂号单ID
     *
     * <p>关联的挂号记录唯一标识，建立处方与就诊的关联关系</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：必须为有效的长整型数值</li>
     *   <li><b>业务规则</b>：必须对应系统中已存在的有效挂号记录</li>
     *   <li><b>数据约束</b>：一个挂号单可以对应多张处方（不同类型）</li>
     * </ul>
     */
    @Schema(description = "挂号单ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    /**
     * 处方类型
     *
     * <p>标识处方的药品类型，影响药品的收费和管理方式</p>
     *
     * <p><b>类型说明：</b></p>
     * <ul>
     *   <li><b>1 - 西药</b>：化学药品和生物制品，按标准规格收费</li>
     *   <li><b>2 - 中药</b>：中药饮片，按重量（克）收费，需要称重配方</li>
     *   <li><b>3 - 中成药</b>：中成药制剂，按盒/瓶收费</li>
     * </ul>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：建议必填，系统可设置默认值</li>
     *   <li><b>取值范围</b>：1、2、3</li>
     *   <li><b>业务规则</b>：同一张处方中的药品类型应与处方类型一致</li>
     * </ul>
     */
    @Schema(description = "处方类型（1=西药, 2=中药, 3=中成药）",
            example = "1",
            allowableValues = {"1", "2", "3"})
    private Short prescriptionType;

    /**
     * 处方有效天数
     *
     * <p>处方的有效期限制，患者必须在此期限内取药，过期需重新开方</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，系统可设置默认值（如3天）</li>
     *   <li><b>取值范围</b>：1-30天，根据医院规定设置</li>
     *   <li><b>业务规则</b>：急诊处方有效期通常较短（1-3天）</li>
     *   <li><b>特殊说明</b>：某些特殊药品可能有不同的有效期要求</li>
     * </ul>
     *
     * <p><b>参考值：</b></p>
     * <ul>
     *   <li>普通门诊处方：3天</li>
     *   <li>急诊处方：1天</li>
     *   <li>慢性病处方：7-14天</li>
     * </ul>
     */
    @Schema(description = "有效天数", example = "3")
    private Integer validityDays;

    /**
     * 药品明细列表
     *
     * <p>处方包含的所有药品及其详细用药信息</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空，至少包含一种药品</li>
     *   <li><b>业务规则</b>：同一处方中同一药品只能出现一次</li>
     *   <li><b>数据约束</b>：药品数量受库存限制</li>
     *   <li><b>格式要求</b>：列表中每个元素必须符合PrescriptionItemDTO的验证规则</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>药品类型应与处方类型一致</li>
     *   <li>需注意药品间的配伍禁忌</li>
     *   <li>特殊药品（麻醉药品、精神药品）需要特殊审批</li>
     * </ul>
     */
    @Schema(description = "药品列表", requiredMode = RequiredMode.REQUIRED)
    private List<PrescriptionItemDTO> items;

    /**
     * 处方药品明细数据传输对象
     *
     * <p>封装单个药品的详细信息和使用说明</p>
     *
     * <h3>主要功能</h3>
     * <ul>
     *   <li><b>药品信息</b>：记录药品ID和数量</li>
     *   <li><b>用药指导</b>：包含用法、用量、频率等关键信息</li>
     *   <li><b>患者告知</b>：提供用药说明和注意事项</li>
     * </ul>
     *
     * @author HIS 开发团队
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Schema(description = "处方药品明细数据传输对象")
    public static class PrescriptionItemDTO {

        /**
         * 药品ID
         *
         * <p>药品库中的唯一标识，关联到具体的药品信息</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填字段</b>：不能为空</li>
         *   <li><b>格式要求</b>：必须为有效的长整型数值</li>
         *   <li><b>业务规则</b>：必须对应药品库中已存在的有效药品</li>
         *   <li><b>数据约束</b>：药品状态必须为可供应（未停用、有库存）</li>
         * </ul>
         */
        @Schema(description = "药品ID", requiredMode = RequiredMode.REQUIRED, example = "1")
        private Long medicineId;

        /**
         * 药品数量
         *
         * <p>该药品的开具数量，单位由药品的规格决定（如盒、瓶、袋等）</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填字段</b>：不能为空</li>
         *   <li><b>格式要求</b>：必须为正整数</li>
         *   <li><b>取值范围</b>：1-999，根据实际需要设置</li>
         *   <li><b>业务规则</b>：不能超过当前库存数量</li>
         *   <li><b>特殊限制</b>：特殊药品（管制药品）有数量限制</li>
         * </ul>
         *
         * <p><b>示例</b>：</p>
         * <ul>
         *   <li>盒装药品：2盒</li>
         *   <li>瓶装药品：1瓶</li>
         *   <li>中药饮片：15（克）</li>
         * </ul>
         */
        @Schema(description = "数量", requiredMode = RequiredMode.REQUIRED, example = "10")
        private Integer quantity;

        /**
         * 用药频率
         *
         * <p>药品的使用频率，指导患者按照正确的时间间隔用药</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填程度</b>：建议必填，对患者用药指导很重要</li>
         *   <li><b>格式要求</b>：文本格式，建议不超过50字符</li>
         *   <li><b>常用表述</b>：一日三次、一日两次、隔日一次、必要时服用等</li>
         * </ul>
         *
         * <p><b>示例</b>：一日三次、一日两次、睡前服用、必要时服用</p>
         */
        @Schema(description = "用药频率", example = "一日三次")
        private String frequency;

        /**
         * 单次用量
         *
         * <p>每次使用的药品数量，指导患者正确用药剂量</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填程度</b>：建议必填，对患者用药指导很重要</li>
         *   <li><b>格式要求</b>：文本格式，建议不超过50字符</li>
         *   <li><b>常用表述</b>：每次1片、每次2粒、每次10ml等</li>
         * </ul>
         *
         * <p><b>示例</b>：每次1片、每次2粒、每次1袋、每次10ml</p>
         */
        @Schema(description = "用量", example = "每次2片")
        private String dosage;

        /**
         * 用药途径
         *
         * <p>药品的给药方式，说明药物如何进入人体</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填程度</b>：建议必填，不同途径影响药效</li>
         *   <li><b>格式要求</b>：文本格式，建议不超过20字符</li>
         *   <li><b>常用途径</b>：口服、静脉注射、肌肉注射、外用、滴眼等</li>
         * </ul>
         *
         * <p><b>常见用药途径：</b></p>
         * <ul>
         *   <li><b>口服</b>：最常见，经口服用</li>
         *   <li><b>静脉注射</b>：直接进入静脉，起效快</li>
         *   <li><b>肌肉注射</b>：注射到肌肉组织</li>
         *   <li><b>外用</b>：涂抹于皮肤表面</li>
         *   <li><b>滴眼/滴耳</b>：局部用药</li>
         * </ul>
         */
        @Schema(description = "用药途径", example = "口服")
        private String route;

        /**
         * 用药天数
         *
         * <p>该药品需要持续使用的天数，用于计算药品总量</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填程度</b>：可选字段，但建议填写</li>
         *   <li><b>格式要求</b>：必须为正整数</li>
         *   <li><b>取值范围</b>：1-365天</li>
         *   <li><b>业务规则</b>：与频率和用量共同计算总用量</li>
         * </ul>
         *
         * <p><b>示例</b>：7天、14天、30天</p>
         */
        @Schema(description = "用药天数", example = "7")
        private Integer days;

        /**
         * 用药说明
         *
         * <p>额外的用药指导和注意事项，帮助患者正确使用药品</p>
         *
         * <p><b>验证规则：</b></p>
         * <ul>
         *   <li><b>必填程度</b>：可选字段</li>
         *   <li><b>格式要求</b>：文本格式，建议不超过200字符</li>
         *   <li><b>内容示例</b>：饭后服用、避光保存、用温水送服等</li>
         * </ul>
         *
         * <p><b>常见说明内容：</b></p>
         * <ul>
         *   <li>服用时间：饭前服用、饭后服用、空腹服用</li>
         *   <li>特殊要求：用温水送服、含服、咀嚼后吞服</li>
         *   <li>注意事项：服药期间忌酒、避免剧烈运动</li>
         *   <li>储存要求：避光保存、冷藏保存</li>
         * </ul>
         */
        @Schema(description = "用药说明", example = "饭后服用")
        private String instructions;
    }
}
