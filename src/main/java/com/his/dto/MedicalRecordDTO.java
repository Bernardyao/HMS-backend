package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import lombok.Data;

/**
 * 诊疗病历数据传输对象
 *
 * <p>用于接收前端提交的病历信息，封装患者就诊过程中的诊断数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>病历录入</b>：医生通过该对象录入患者诊疗信息</li>
 *   <li><b>数据结构化</b>：将病历信息按照标准医疗格式组织</li>
 *   <li><b>信息完整</b>：包含主诉、现病史、既往史、诊断、治疗方案等完整诊疗信息</li>
 *   <li><b>状态管理</b>：支持草稿、已提交、已审核等状态流转</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生诊疗</b>：医生在医生工作站为患者创建或更新病历</li>
 *   <li><b>病历查询</b>：查询患者的历次诊疗记录</li>
 *   <li><b>统计分析</b>：基于病历数据进行医疗统计和分析</li>
 *   <li><b>质控审核</b>：质控人员对病历进行审核和质量控制</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li><b>必填字段</b>：registrationId（挂号单ID）为必填字段</li>
 *   <li><b>业务验证</b>：挂号单必须存在且状态有效</li>
 *   <li><b>数据完整性</b>：提交时（状态=1）必须包含诊断信息</li>
 *   <li><b>格式要求</b>：各项文本字段建议不超过2000字符</li>
 * </ul>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li><b>0 - 草稿</b>：医生正在编辑中，尚未完成</li>
 *   <li><b>1 - 已提交</b>：医生已完成病历填写，等待审核</li>
 *   <li><b>2 - 已审核</b>：病历通过质控审核，正式归档</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "诊疗病历数据传输对象")
public class MedicalRecordDTO {

    /**
     * 挂号单ID
     *
     * <p>关联的挂号记录唯一标识，用于建立病历与就诊的关联关系</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填字段</b>：不能为空</li>
     *   <li><b>格式要求</b>：必须为有效的长整型数值</li>
     *   <li><b>业务规则</b>：必须对应系统中已存在的有效挂号记录</li>
     *   <li><b>数据约束</b>：一个挂号单只能对应一条有效病历记录</li>
     * </ul>
     */
    @Schema(description = "挂号单ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    /**
     * 主诉
     *
     * <p>患者就诊时的主要症状描述，包括症状、部位、持续时间等关键信息</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：建议必填，但在草稿状态可以为空</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过500字符</li>
     *   <li><b>内容要求</b>：应包含症状、部位、性质、持续时间等要素</li>
     * </ul>
     *
     * <p><b>示例</b>：头痛、发热3天</p>
     */
    @Schema(description = "主诉", example = "头痛、发热3天")
    private String chiefComplaint;

    /**
     * 现病史
     *
     * <p>详细记录本次疾病的起病情况、发展经过、诊疗经过等信息</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，但建议填写</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过2000字符</li>
     *   <li><b>内容要求</b>：应包含起病情况、症状演变、诊疗经过等</li>
     * </ul>
     *
     * <p><b>示例</b>：患者3天前无明显诱因出现头痛，呈持续性胀痛...</p>
     */
    @Schema(description = "现病史", example = "患者3天前无明显诱因出现头痛...")
    private String presentIllness;

    /**
     * 既往史
     *
     * <p>患者过去的疾病史、手术史、外伤史、输血史等历史医疗记录</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过2000字符</li>
     *   <li><b>内容要求</b>：应记录重要的既往疾病和诊疗历史</li>
     * </ul>
     *
     * <p><b>示例</b>：既往体健，无慢性病史，否认手术外伤史</p>
     */
    @Schema(description = "既往史", example = "既往体健，无慢性病史")
    private String pastHistory;

    /**
     * 个人史
     *
     * <p>患者的个人生活习惯、职业、嗜好等信息，包括吸烟、饮酒、疫区居住史等</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过1000字符</li>
     * </ul>
     *
     * <p><b>示例</b>：无吸烟饮酒史，无疫区居住史</p>
     */
    @Schema(description = "个人史", example = "无吸烟饮酒史")
    private String personalHistory;

    /**
     * 家族史
     *
     * <p>患者直系亲属的遗传病史、传染病史等可能具有遗传倾向的疾病信息</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过1000字符</li>
     * </ul>
     *
     * <p><b>示例</b>：无遗传病史，父母体健</p>
     */
    @Schema(description = "家族史", example = "无遗传病史")
    private String familyHistory;

    /**
     * 体格检查
     *
     * <p>医生对患者进行身体检查时记录的各项体征数据，包括体温、脉搏、血压等</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，但建议填写</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过2000字符</li>
     *   <li><b>内容要求</b>：应记录重要的阳性体征和有鉴别意义的阴性体征</li>
     * </ul>
     *
     * <p><b>示例</b>：T: 38.5°C, P: 90次/分, R: 20次/分, BP: 120/80mmHg</p>
     */
    @Schema(description = "体格检查", example = "T: 38.5°C, P: 90次/分...")
    private String physicalExam;

    /**
     * 辅助检查
     *
     * <p>患者进行的各项辅助检查结果，如化验、影像、心电图等检查数据</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过2000字符</li>
     *   <li><b>内容要求</b>：应记录检查项目名称和关键结果数据</li>
     * </ul>
     *
     * <p><b>示例</b>：血常规：WBC 12.5×10^9/L, N 85%</p>
     */
    @Schema(description = "辅助检查", example = "血常规：WBC 12.5×10^9/L")
    private String auxiliaryExam;

    /**
     * 诊断
     *
     * <p>医生根据患者症状和检查结果做出的诊断结论，是病历的核心信息</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：提交状态时必填</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过500字符</li>
     *   <li><b>业务规则</b>：应使用规范的疾病名称，建议配合诊断编码使用</li>
     * </ul>
     *
     * <p><b>示例</b>：上呼吸道感染、急性支气管炎</p>
     */
    @Schema(description = "诊断", example = "上呼吸道感染")
    private String diagnosis;

    /**
     * 诊断编码
     *
     * <p>按照国际疾病分类标准（ICD-10）的疾病编码，便于统计和医保结算</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，但建议填写</li>
     *   <li><b>格式要求</b>：符合ICD-10编码规范</li>
     *   <li><b>业务规则</b>：应与诊断内容对应匹配</li>
     * </ul>
     *
     * <p><b>示例</b>：J06.9（急性上呼吸道感染，未特指）</p>
     */
    @Schema(description = "诊断编码", example = "J06.9")
    private String diagnosisCode;

    /**
     * 治疗方案
     *
     * <p>医生制定的治疗计划和具体措施，包括用药、检查、手术等治疗手段</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，但建议填写</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过2000字符</li>
     *   <li><b>内容要求</b>：应清晰列出治疗措施和用药方案</li>
     * </ul>
     *
     * <p><b>示例</b>：1. 抗感染治疗（阿莫西林 0.5g tid） 2. 对症处理（退烧药）</p>
     */
    @Schema(description = "治疗方案", example = "1. 抗感染治疗 2. 对症处理")
    private String treatmentPlan;

    /**
     * 医嘱
     *
     * <p>医生对患者的生活指导、用药注意事项、复查时间等建议</p>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段</li>
     *   <li><b>格式要求</b>：文本格式，建议不超过1000字符</li>
     * </ul>
     *
     * <p><b>示例</b>：注意休息，多饮水，清淡饮食，3天后复查</p>
     */
    @Schema(description = "医嘱", example = "注意休息，多饮水")
    private String doctorAdvice;

    /**
     * 病历状态
     *
     * <p>标识病历在诊疗流程中的当前状态，用于工作流控制和权限管理</p>
     *
     * <p><b>状态值说明：</b></p>
     * <ul>
     *   <li><b>0 - 草稿</b>：医生正在编辑，尚未提交</li>
     *   <li><b>1 - 已提交</b>：医生已完成病历，等待审核</li>
     *   <li><b>2 - 已审核</b>：通过质控审核，正式归档</li>
     * </ul>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li><b>必填程度</b>：可选字段，系统默认为0</li>
     *   <li><b>取值范围</b>：0、1、2</li>
     *   <li><b>业务规则</b>：状态流转：草稿→已提交→已审核，不可逆</li>
     * </ul>
     */
    @Schema(description = "状态（0=草稿, 1=已提交, 2=已审核）",
            example = "1",
            allowableValues = {"0", "1", "2"})
    private Short status;
}
