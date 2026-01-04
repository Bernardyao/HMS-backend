package com.his.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费信息视图对象
 *
 * <p>用于收费管理，封装收费信息和收费明细返回给前端</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>收费管理</b>：支持患者收费和退费</li>
 *   <li><b>收费查询</b>：查询收费记录和状态</li>
 *   <li><b>明细管理</b>：显示收费明细列表</li>
 *   <li><b>状态跟踪</b>：跟踪收费状态（未缴费、已缴费、已退费）</li>
 * </ul>
 *
 * <h3>数据来源</h3>
 * <p>从 {@link com.his.entity.Charge} 实体转换而来，关联患者、收费明细信息</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>收费管理</b>：收费员为患者收费</li>
 *   <li><b>收费查询</b>：查询历史收费记录</li>
 *   <li><b>退费管理</b>：处理患者退费</li>
 * </ul>
 *
 * <h3>特殊说明</h3>
 * <ul>
 *   <li><b>状态流转</b>：未缴费(0) -> 已缴费(1) -> 已退费(2)</li>
 *   <li><b>totalAmount</b>：BigDecimal类型，精度为2位小数，单位为元</li>
 *   <li><b>details</b>：包含处方、检查等收费明细</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "收费信息")
public class ChargeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收费ID
     *
     * <p>收费记录在数据库中的唯一标识</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Long</li>
     *   <li>必填：是</li>
     *   <li>唯一：是</li>
     *   <li>示例：1</li>
     * </ul>
     */
    @Schema(description = "收费 ID", example = "1")
    private Long id;

    /**
     * 收费单号
     *
     * <p>收费的业务编号，系统自动生成</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>格式：CHG+yyyyMMdd+序号</li>
     *   <li>示例："CHG20231201001"</li>
     *   <li>唯一：是</li>
     * </ul>
     */
    @Schema(description = "收费单号", example = "CHG20231201001")
    private String chargeNo;

    /**
     * 患者ID
     *
     * <p>收费患者的唯一标识</p>
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
     * 患者姓名
     *
     * <p>收费患者的姓名</p>
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
     * 应收总金额
     *
     * <p>本次收费的总金额，所有明细金额之和</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：BigDecimal</li>
     *   <li>精度：2位小数</li>
     *   <li>示例：156.80（156.8元）</li>
     *   <li>单位：元</li>
     * </ul>
     */
    @Schema(description = "应收总金额", example = "156.80")
    private BigDecimal totalAmount;

    /**
     * 收费状态
     *
     * <p>收费记录的当前状态</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：Short</li>
     *   <li>枚举值：0=未缴费, 1=已缴费, 2=已退费</li>
     *   <li>示例：0（未缴费）</li>
     * </ul>
     */
    @Schema(description = "收费状态（0=未缴费, 1=已缴费, 2=已退费）",
            example = "0",
            allowableValues = {"0", "1", "2"})
    private Short status;

    /**
     * 状态描述
     *
     * <p>收费状态的文字描述，前端可直接显示</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：String</li>
     *   <li>枚举值："未缴费"、"已缴费"、"已退费"</li>
     *   <li>示例："未缴费"</li>
     * </ul>
     */
    @Schema(description = "状态描述", example = "未缴费")
    private String statusDesc;

    /**
     * 收费明细列表
     *
     * <p>收费包含的所有明细项目</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：List&lt;ChargeDetailVO&gt;</li>
     *   <li>可以为空列表</li>
     * </ul>
     */
    @Schema(description = "收费明细列表")
    private List<ChargeDetailVO> details;

    /**
     * 创建时间
     *
     * <p>收费记录创建的时间</p>
     *
     * <p><b>数据格式：</b></p>
     * <ul>
     *   <li>类型：LocalDateTime</li>
     *   <li>格式：yyyy-MM-ddTHH:mm:ss</li>
     *   <li>示例："2023-12-01T10:30:00"</li>
     * </ul>
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 收费明细视图对象
     *
     * <p>收费中的单个明细项目</p>
     *
     * @author HIS 开发团队
     * @version 1.0
     * @since 1.0
     */
    @Data
    @Schema(description = "收费明细信息")
    public static class ChargeDetailVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 项目类型
         *
         * <p>收费项目的类型</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>枚举值：PRESCRIPTION（处方）、REGISTRATION（挂号）、EXAMINATION（检查）</li>
         *   <li>示例："PRESCRIPTION"</li>
         * </ul>
         */
        @Schema(description = "项目类型", example = "PRESCRIPTION")
        private String itemType;

        /**
         * 项目名称
         *
         * <p>收费项目的名称</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：String</li>
         *   <li>长度：1-100字符</li>
         *   <li>示例："阿莫西林"</li>
         * </ul>
         */
        @Schema(description = "项目名称", example = "阿莫西林")
        private String itemName;

        /**
         * 项目金额
         *
         * <p>该收费项目的金额</p>
         *
         * <p><b>数据格式：</b></p>
         * <ul>
         *   <li>类型：BigDecimal</li>
         *   <li>精度：2位小数</li>
         *   <li>示例：56.80（56.8元）</li>
         *   <li>单位：元</li>
         * </ul>
         */
        @Schema(description = "项目金额", example = "56.80")
        private BigDecimal itemAmount;
    }
}
