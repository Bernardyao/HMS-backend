package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号信息视图对象
 *
 * <p>用于患者挂号和挂号查询，封装挂号信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>患者挂号</b>：支持患者在线挂号</li>
 *   <li><b>挂号查询</b>：查询挂号记录和状态</li>
 *   <li><b>排队管理</b>：显示排队号和预约时间</li>
 *   <li><b>状态跟踪</b>：跟踪挂号状态（待就诊、已就诊、已取消）</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Registration} 实体转换而来，关联患者、科室、医生信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>患者挂号</b>：患者进行在线挂号</li>
 *   <li><b>挂号查询</b>：查询历史挂号记录</li>
 *   <li><b>医生工作站</b>：医生查看待诊患者</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>状态流转</b>：待就诊(0) -> 已就诊(1) 或 已取消(2)</li>
 *   <li><b>registrationFee</b>：BigDecimal类型，精度为2位小数，单位为元</li>
 *   <li><b>queueNo</b>：系统自动生成的排队号码</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "挂号信息")
public class RegistrationVO implements Serializable {

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
     * <p>挂号的业务编号，系统自动生成</p>
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
     * 患者性别
     *
     * <p>挂号患者的性别</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=女, 1=男, 2=未知</li>
     *   <li>示例：1（男）</li>
     * </ul>
     */
    @Schema(description = "患者性别（0=女, 1=男, 2=未知）", example = "1")
    private Short gender;

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
     * <p>挂号状态的文字描述，前端可直接显示</p>
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
}
