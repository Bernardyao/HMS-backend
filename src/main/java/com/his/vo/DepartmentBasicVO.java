package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科室基础信息视图对象
 *
 * <p>用于挂号界面的科室选择下拉框，封装科室的最基础信息返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>科室选择</b>：提供前端挂号界面所需的科室基础数据</li>
 *   <li><b>层级展示</b>：包含科室层级关系，支持父子科室展示</li>
 *   <li><b>轻量数据</b>：仅包含必要字段，减少网络传输</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Department} 实体转换而来，仅提取基础字段</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>患者挂号</b>：挂号界面科室下拉选择</li>
 *   <li><b>医生查询</b>：按科室筛选医生时的科室选择</li>
 *   <li><b>排班查询</b>：查看科室排班信息时的科室选择</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>parentId为null</b>：表示顶级科室，无上级科室</li>
 *   <li><b>parentName为null</b>：当parentId为null时，parentName也为null</li>
 *   <li><b>与DepartmentVO区别</b>：此VO仅包含基础字段，不包含description等详细信息</li>
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
@Schema(description = "科室基础信息")
public class DepartmentBasicVO {

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
     *   <li>示例："DEPT001"、"INTERNAL"</li>
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
     * 上级科室ID
     *
     * <p>父科室的ID，用于构建科室层级关系</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：否（顶级科室为null）</li>
     *   <li>示例：null（顶级科室）、10（子科室）</li>
     * </ul>
     */
    @Schema(description = "上级科室ID")
    private Long parentId;

    /**
     * 上级科室名称
     *
     * <p>父科室的名称，用于前端展示科室层级关系</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>必填：否（顶级科室为null）</li>
     *   <li>示例：null（顶级科室）、"临床科室"（子科室）</li>
     * </ul>
     */
    @Schema(description = "上级科室名称")
    private String parentName;
}
