package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 科室信息视图对象
 *
 * <p>用于科室管理界面，封装科室的完整信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>科室管理</b>：提供科室列表、科室详情的完整数据</li>
 *   <li><b>科室排序</b>：支持自定义排序，控制科室展示顺序</li>
 *   <li><b>科室描述</b>：包含科室的详细介绍信息</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Department} 实体转换而来，包含完整的科室信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>科室管理</b>：管理员查看和管理科室信息</li>
 *   <li><b>科室详情</b>：查看科室的完整信息</li>
 *   <li><b>科室列表</b>：展示所有科室的详细信息</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>与DepartmentBasicVO区别</b>：包含description和sortOrder等详细信息</li>
 *   <li><b>sortOrder字段</b>：用于控制科室在前端列表中的展示顺序，值越小越靠前</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "科室信息")
public class DepartmentVO {

    /**
     * 科室ID
     *
     * <p>科室在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "科室ID")
    private Long id;

    /**
     * 科室代码
     *
     * <p>科室的业务编码，用于业务逻辑处理和系统间对接</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-50字符</li>
     *   <li>示例： "DEPT001"、"INTERNAL"</li>
     * </ul>
     */
    @Schema(description = "科室代码")
    private String code;

    /**
     * 科室名称
     *
     * <p>科室的显示名称，用于前端界面展示</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：1-100字符</li>
     *   <li>示例："内科"、"外科"、"儿科"</li>
     * </ul>
     */
    @Schema(description = "科室名称")
    private String name;

    /**
     * 排序序号
     *
     * <p>控制科室在列表中的展示顺序，数值越小越靠前</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Integer</li>
     *   <li>必填：否</li>
     *   <li>示例：1（第一位）、10（第十位）</li>
     *   <li>默认值：0</li>
     * </ul>
     */
    @Schema(description = "排序序号")
    private Integer sortOrder;

    /**
     * 科室描述
     *
     * <p>科室的详细介绍，包括诊疗范围、专业特色等信息</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>长度：0-500字符</li>
     *   <li>示例："主要负责心血管疾病的诊断和治疗"</li>
     *   <li>可以为空字符串</li>
     * </ul>
     */
    @Schema(description = "科室描述")
    private String description;
}
