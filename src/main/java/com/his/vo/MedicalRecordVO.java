package com.his.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 病历视图对象
 *
 * <p>用于医生工作站，封装患者的病历信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>病历创建</b>：支持医生为患者创建病历记录</li>
 *   <li><b>病历查看</b>：查看患者的历史病历记录</li>
 *   <li><b>病历编辑</b>：支持草稿状态的病历编辑</li>
 *   <li><b>病历审核</b>：支持病历的审核流程</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.MedicalRecord} 实体转换而来，关联挂号、患者和医生信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生工作站</b>：医生查看和编辑患者病历</li>
 *   <li><b>患者就诊</b>：记录患者就诊过程中的诊断和治疗信息</li>
 *   <li><b>病历查询</b>：查询患者的历史病历记录</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>状态流转</b>：草稿(0) -> 已提交(1) -> 已审核(2)</li>
 *   <li><b>草稿状态</b>：允许编辑，医生可多次修改</li>
 *   <li><b>已提交状态</b>：不允许编辑，等待审核</li>
 *   <li><b>已审核状态</b>：正式病历，不允许修改</li>
 *   <li><b>病历编号</b>：自动生成，格式为MR+时间戳+随机数</li>
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
@Schema(description = "病历视图对象")
public class MedicalRecordVO {

    /**
     * 病历ID
     *
     * <p>病历在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "病历ID", example = "1")
    private Long mainId;

    /**
     * 病历编号
     *
     * <p>病历的业务编号，系统自动生成</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：MR+yyyyMMddHHmmss+随机数</li>
     *   <li>长度：固定24位</li>
     *   <li>示例："MR202512201533456789123"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "病历编号", example = "MR202512201533456789123")
    private String recordNo;

    /**
     * 挂号单ID
     *
     * <p>关联的挂号记录ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "挂号单ID", example = "1")
    private Long registrationId;

    /**
     * 患者ID
     *
     * <p>就诊患者的唯一标识</p>
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
     * <p>就诊患者的姓名</p>
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
     * <p>接诊医生的唯一标识</p>
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
     * <p>接诊医生的姓名</p>
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
     * 主诉
     *
     * <p>患者的主要症状描述</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-500字符</li>
     *   <li>示例："头痛、发热3天"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "主诉", example = "头痛、发热3天")
    private String chiefComplaint;

    /**
     * 现病史
     *
     * <p>本次疾病的详细描述，包括起病情况、发展过程、伴随症状等</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："患者3天前无明显诱因出现头痛..."</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "现病史", example = "患者3天前无明显诱因出现头痛...")
    private String presentIllness;

    /**
     * 既往史
     *
     * <p>患者既往疾病史、手术史、外伤史等</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："既往体健，无慢性病史"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "既往史", example = "既往体健，无慢性病史")
    private String pastHistory;

    /**
     * 个人史
     *
     * <p>患者的个人生活习惯，包括吸烟、饮酒、职业等</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："无吸烟饮酒史"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "个人史", example = "无吸烟饮酒史")
    private String personalHistory;

    /**
     * 家族史
     *
     * <p>患者家族成员的疾病史，特别是遗传性疾病</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："无遗传病史"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "家族史", example = "无遗传病史")
    private String familyHistory;

    /**
     * 体格检查
     *
     * <p>医生对患者进行体格检查的结果记录</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："T: 38.5°C, P: 90次/分..."</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "体格检查", example = "T: 38.5°C, P: 90次/分...")
    private String physicalExam;

    /**
     * 辅助检查
     *
     * <p>实验室检查、影像学检查等辅助检查结果</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："血常规：WBC 12.5×10^9/L"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "辅助检查", example = "血常规：WBC 12.5×10^9/L")
    private String auxiliaryExam;

    /**
     * 诊断
     *
     * <p>医生对患者疾病的诊断结论</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-500字符</li>
     *   <li>示例："上呼吸道感染"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "诊断", example = "上呼吸道感染")
    private String diagnosis;

    /**
     * 诊断编码
     *
     * <p>国际疾病分类编码（ICD-10）</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：ICD-10编码</li>
     *   <li>示例："J06.9"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "诊断编码", example = "J06.9")
    private String diagnosisCode;

    /**
     * 治疗方案
     *
     * <p>医生制定的治疗计划和措施</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-2000字符</li>
     *   <li>示例："1. 抗感染治疗 2. 对症处理"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "治疗方案", example = "1. 抗感染治疗 2. 对症处理")
    private String treatmentPlan;

    /**
     * 医嘱
     *
     * <p>医生给患者的建议和注意事项</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-1000字符</li>
     *   <li>示例："注意休息，多饮水"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "医嘱", example = "注意休息，多饮水")
    private String doctorAdvice;

    /**
     * 状态
     *
     * <p>病历的状态，标识病历的处理进度</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=草稿, 1=已提交, 2=已审核</li>
     *   <li>示例：1（已提交）</li>
     * </ul>
     */
    @Schema(description = "状态（0=草稿, 1=已提交, 2=已审核）",
            example = "1",
            allowableValues = {"0", "1", "2"})
    private Short status;

    /**
     * 就诊时间
     *
     * <p>患者就诊的具体时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2025-12-20T15:34:00"</li>
     * </ul>
     */
    @Schema(description = "就诊时间", example = "2025-12-20T15:34:00")
    private LocalDateTime visitTime;

    /**
     * 创建时间
     *
     * <p>病历记录创建的时间</p>
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
     * <p>病历记录最后更新的时间</p>
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
}
