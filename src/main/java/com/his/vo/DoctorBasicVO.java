package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 医生基础信息视图对象
 *
 * <p>用于挂号界面的医生选择下拉框，封装医生的基础信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>医生选择</b>：提供前端挂号界面所需的医生基础数据</li>
 *   <li><b>挂号费展示</b>：包含医生挂号费信息，方便患者选择</li>
 *   <li><b>医生状态</b>：仅显示启用状态的医生</li>
 *   <li><b>详细信息</b>：包含职称、专长等关键信息</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Doctor} 实体转换而来，关联科室和挂号费信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>患者挂号</b>：挂号界面医生下拉选择</li>
 *   <li><b>医生查询</b>：查看科室出诊医生列表</li>
 *   <li><b>排班查询</b>：查看医生排班信息</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>genderText字段</b>：前端可直接显示，无需转换</li>
 *   <li><b>statusText字段</b>：前端可直接显示，无需转换</li>
 *   <li><b>挂号费</b>：BigDecimal类型，精度为2位小数</li>
 *   <li><b>与DoctorVO区别</b>：包含挂号费和科室信息，用于挂号场景</li>
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
@Schema(description = "医生基础信息")
public class DoctorBasicVO {

    /**
     * 医生ID
     *
     * <p>医生在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "医生ID")
    private Long id;

    /**
     * 医生工号
     *
     * <p>医生的工作编号，用于唯一标识医生身份</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-20字符</li>
     *   <li>示例："D001"、"DOC2023001"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "医生工号")
    private String doctorNo;

    /**
     * 医生姓名
     *
     * <p>医生的真实姓名</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："张三"、"李四"</li>
     * </ul>
     */
    @Schema(description = "医生姓名")
    private String name;

    /**
     * 性别
     *
     * <p>医生的性别信息</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=女, 1=男, 2=未知</li>
     *   <li>示例：1（男）</li>
     * </ul>
     */
    @Schema(description = "性别（0=女, 1=男, 2=未知）")
    private Short gender;

    /**
     * 性别文本
     *
     * <p>性别的文字描述，前端可直接使用</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："女"、"男"、"未知"</li>
     *   <li>示例："男"</li>
     * </ul>
     */
    @Schema(description = "性别文本")
    private String genderText;

    /**
     * 职称
     *
     * <p>医生的专业职称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例："主任医师"、"副主任医师"、"主治医师"</li>
     * </ul>
     */
    @Schema(description = "职称")
    private String title;

    /**
     * 专长
     *
     * <p>医生的专业擅长领域</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-500字符</li>
     *   <li>示例："心血管疾病、高血压、冠心病"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "专长")
    private String specialty;

    /**
     * 状态
     *
     * <p>医生的启用状态</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=停用, 1=启用</li>
     *   <li>示例：1（启用）</li>
     * </ul>
     */
    @Schema(description = "状态（0=停用, 1=启用）")
    private Short status;

    /**
     * 状态文本
     *
     * <p>状态的文字描述，前端可直接使用</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："停用"、"启用"</li>
     *   <li>示例："启用"</li>
     * </ul>
     */
    @Schema(description = "状态文本")
    private String statusText;

    /**
     * 所属科室ID
     *
     * <p>医生所属科室的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>示例：10（内科）</li>
     * </ul>
     */
    @Schema(description = "所属科室ID")
    private Long departmentId;

    /**
     * 所属科室名称
     *
     * <p>医生所属科室的名称</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>示例："内科"、"外科"</li>
     * </ul>
     */
    @Schema(description = "所属科室名称")
    private String departmentName;

    /**
     * 挂号费
     *
     * <p>该医生的挂号费用，单位为元</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：50.00（50元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "挂号费（元）", example = "50.00")
    private BigDecimal registrationFee;
}
