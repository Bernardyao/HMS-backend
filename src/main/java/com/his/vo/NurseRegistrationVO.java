package com.his.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 护士工作站挂号列表视图对象
 *
 * <p>用于护士工作站，封装挂号信息返回给前端，包含患者的敏感脱敏信息</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>挂号管理</b>：护士查看和管理挂号信息</li>
 *   <li><b>患者信息</b>：展示患者基本信息（含脱敏）</li>
 *   <li><b>就诊管理</b>：显示就诊类型和状态</li>
 *   <li><b>病历状态</b>：标识患者是否已有病历记录</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Registration} 实体转换而来，关联患者、科室、医生信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>护士工作站</b>：护士查看和管理挂号信息</li>
 *   <li><b>分诊管理</b>：协助患者分诊和引导</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>数据脱敏</b>：身份证号和手机号已脱敏处理</li>
 *   <li><b>visitType</b>：1=初诊, 2=复诊, 3=急诊</li>
 *   <li><b>hasMedicalRecord</b>：标识患者是否有病历记录，方便护士引导</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "护士工作站挂号信息")
public class NurseRegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 挂号记录ID
     *
     * <p>挂号记录在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "挂号记录 ID", example = "1")
    private Long id;

    /**
     * 挂号流水号
     *
     * <p>挂号的业务编号</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：REG+yyyyMMdd+序号</li>
     *   <li>示例："REG20231201001"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "挂号流水号", example = "REG20231201001")
    private String regNo;

    /**
     * 患者姓名
     *
     * <p>挂号患者的姓名</p>
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
     * 患者ID
     *
     * <p>挂号患者的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：1001</li>
     * </ul>
     */
    @Schema(description = "患者 ID", example = "1001")
    private Long patientId;

    /**
     * 患者性别描述
     *
     * <p>患者的性别文字描述</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："女"、"男"、"未知"</li>
     *   <li>示例："男"</li>
     * </ul>
     */
    @Schema(description = "患者性别描述", example = "男")
    private String genderDesc;

    /**
     * 患者年龄
     *
     * <p>挂号患者的年龄</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>范围：0-150</li>
     *   <li>示例：35</li>
     *   <li>单位：岁</li>
     * </ul>
     */
    @Schema(description = "患者年龄", example = "35")
    private Short age;

    /**
     * 患者身份证号（脱敏）
     *
     * <p>患者的身份证号，已脱敏处理</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>脱敏规则：保留前3位和后4位</li>
     *   <li>示例："320***********1234"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "患者身份证号（脱敏）", example = "320***********1234")
    private String idCard;

    /**
     * 患者联系电话（脱敏）
     *
     * <p>患者的联系电话，已脱敏处理</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>脱敏规则：保留前3位和后4位</li>
     *   <li>示例："138****5678"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "患者联系电话（脱敏）", example = "138****5678")
    private String phone;

    /**
     * 科室ID
     *
     * <p>挂号的科室ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：10</li>
     * </ul>
     */
    @Schema(description = "科室 ID", example = "10")
    private Long deptId;

    /**
     * 科室名称
     *
     * <p>挂号的科室名称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-100字符</li>
     *   <li>示例："内科"</li>
     * </ul>
     */
    @Schema(description = "科室名称", example = "内科")
    private String deptName;

    /**
     * 医生ID
     *
     * <p>挂号的医生ID</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：100</li>
     * </ul>
     */
    @Schema(description = "医生 ID", example = "100")
    private Long doctorId;

    /**
     * 医生姓名
     *
     * <p>挂号的医生姓名</p>
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
     * 医生职称
     *
     * <p>挂号医生的专业职称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："主任医师"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "医生职称", example = "主任医师")
    private String doctorTitle;

    /**
     * 挂号状态
     *
     * <p>挂号记录的当前状态</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=待就诊, 1=已就诊, 2=已取消, 3=已退费, 4=已缴挂号费, 5=就诊中</li>
     *   <li>示例：0（待就诊）</li>
     * </ul>
     */
    @Schema(description = "挂号状态（0=待就诊, 1=已就诊, 2=已取消, 3=已退费, 4=已缴挂号费, 5=就诊中）",
            example = "0",
            allowableValues = {"0", "1", "2", "3", "4", "5"})
    private Short status;

    /**
     * 状态描述
     *
     * <p>挂号状态的文字描述</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："待就诊"、"已就诊"、"已取消"、"已退费"、"已缴挂号费"、"就诊中"</li>
     *   <li>示例："待就诊"</li>
     * </ul>
     */
    @Schema(description = "状态描述",
            example = "待就诊",
            allowableValues = {"待就诊", "已就诊", "已取消", "已退费", "已缴挂号费", "就诊中"})
    private String statusDesc;

    /**
     * 就诊类型
     *
     * <p>患者的就诊类型</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：1=初诊, 2=复诊, 3=急诊</li>
     *   <li>示例：1（初诊）</li>
     * </ul>
     */
    @Schema(description = "就诊类型（1=初诊, 2=复诊, 3=急诊）",
            example = "1",
            allowableValues = {"1", "2", "3"})
    private Short visitType;

    /**
     * 就诊类型描述
     *
     * <p>就诊类型的文字描述</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："初诊"、"复诊"、"急诊"</li>
     *   <li>示例："初诊"</li>
     * </ul>
     */
    @Schema(description = "就诊类型描述", example = "初诊")
    private String visitTypeDesc;

    /**
     * 就诊日期
     *
     * <p>患者预约的就诊日期</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDate</li>
     *   <li>格式：yyyy-MM-dd</li>
     *   <li>示例："2023-12-01"</li>
     * </ul>
     */
    @Schema(description = "就诊日期", example = "2023-12-01", type = "string", format = "date")
    private LocalDate visitDate;

    /**
     * 挂号费
     *
     * <p>本次挂号需要支付的费用</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：15.00（15元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "挂号费", example = "15.00")
    private BigDecimal registrationFee;

    /**
     * 排队号
     *
     * <p>挂号后获得的排队号码</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-20字符</li>
     *   <li>示例："A001"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "排队号", example = "A001")
    private String queueNo;

    /**
     * 预约时间
     *
     * <p>患者预约的就诊时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2023-12-01T09:00:00"</li>
     *   <li>可以为空</li>
     * </ul>
     */
    @Schema(description = "预约时间", example = "2023-12-01T09:00:00", type = "string", format = "date-time")
    private LocalDateTime appointmentTime;

    /**
     * 创建时间
     *
     * <p>挂号记录创建的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2023-12-01T08:30:00"</li>
     * </ul>
     */
    @Schema(description = "创建时间", example = "2023-12-01T08:30:00", type = "string", format = "date-time")
    private LocalDateTime createdAt;

    /**
     * 是否有病历
     *
     * <p>标识患者是否已有病历记录</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Boolean</li>
     *   <li>true - 有病历记录</li>
     *   <li>false - 无病历记录</li>
     *   <li>示例：true</li>
     * </ul>
     *
     * <p><b>使用说明：</b></p>
     * <ul>
     *   <li>用于护士判断是否需要为患者建立病历</li>
     *   <li>false时护士应引导患者先建立病历</li>
     * </ul>
     */
    @Schema(description = "是否有病历", example = "true")
    private Boolean hasMedicalRecord;
}
