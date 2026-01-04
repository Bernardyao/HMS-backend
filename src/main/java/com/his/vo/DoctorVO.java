package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 医生信息视图对象
 *
 * <p>用于医生管理界面，封装医生的完整信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>医生管理</b>：提供医生列表、医生详情的完整数据</li>
 *   <li><b>医生信息维护</b>：支持医生信息的增删改查</li>
 *   <li><b>医生状态管理</b>：包含医生启用/停用状态</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Doctor} 实体转换而来，包含医生的基本信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生管理</b>：管理员查看和管理医生信息</li>
 *   <li><b>医生列表</b>：展示所有医生的详细信息</li>
 *   <li><b>医生查询</b>：按条件查询医生信息</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>与DoctorBasicVO区别</b>：不包含挂号费和科室名称，用于管理场景</li>
 *   <li><b>gender字段</b>：使用枚举值，前端需要转换</li>
 *   <li><b>status字段</b>：用于控制医生是否可用</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "医生信息")
public class DoctorVO {

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
    private String code;

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
}
