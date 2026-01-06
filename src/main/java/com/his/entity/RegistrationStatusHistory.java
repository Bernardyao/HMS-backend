package com.his.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挂号状态转换历史实体类
 *
 * <p>映射数据库表 his_registration_status_history，记录所有挂号状态转换的审计日志</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>状态转换追踪</b>：记录每次状态变更的详细信息</li>
 *   <li><b>审计日志</b>：支持操作人、操作时间、转换原因追踪</li>
 *   <li><b>问题排查</b>：便于定位状态异常和排查问题</li>
 *   <li><b>业务分析</b>：支持就诊流程分析和优化</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li><b>只读记录</b>：审计记录不可修改，仅用于追踪</li>
 *   <li><b>级联删除</b>：挂号记录删除时，历史记录自动删除</li>
 *   <li><b>完整记录</b>：每次状态转换都会创建新记录</li>
 *   <li><b>操作人信息</b>：记录操作人ID、姓名和操作类型</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>状态机日志</b>：记录所有状态转换行为</li>
 *   <li><b>问题排查</b>：追踪挂号状态异常变更</li>
 *   <li><b>业务审计</b>：满足医疗系统审计要求</li>
 *   <li><b>数据分析</b>：分析医生接诊效率和患者流转情况</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Registration
 * @see com.his.enums.RegStatusEnum
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "his_registration_status_history")
public class RegistrationStatusHistory {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "main_id")
    private Long mainId;

    /**
     * 挂号记录ID
     */
    @Column(name = "registration_main_id", nullable = false)
    private Long registrationMainId;

    /**
     * 源状态码
     */
    @Column(name = "from_status", nullable = false)
    private Short fromStatus;

    /**
     * 目标状态码
     */
    @Column(name = "to_status", nullable = false)
    private Short toStatus;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @Column(name = "operator_name", length = 50)
    private String operatorName;

    /**
     * 操作类型
     * <p>SYSTEM = 系统自动执行，USER = 用户手动操作</p>
     */
    @Column(name = "operator_type", length = 20)
    private String operatorType;

    /**
     * 状态转换原因
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * 创建时间（状态转换时间）
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 获取源状态描述
     */
    public String getFromStatusDescription() {
        try {
            return com.his.enums.RegStatusEnum.fromCode(fromStatus).getDescription();
        } catch (Exception e) {
            return "未知状态(" + fromStatus + ")";
        }
    }

    /**
     * 获取目标状态描述
     */
    public String getToStatusDescription() {
        try {
            return com.his.enums.RegStatusEnum.fromCode(toStatus).getDescription();
        } catch (Exception e) {
            return "未知状态(" + toStatus + ")";
        }
    }
}
