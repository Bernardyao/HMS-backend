package com.his.service;

import com.his.entity.Prescription;
import com.his.enums.PrescriptionStatusEnum;

/**
 * 处方状态机接口
 *
 * <p>统一管理处方状态转换，确保状态流转的合法性和一致性</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>状态转换</b>：验证并执行合法的状态转换</li>
 *   <li><b>转换验证</b>：在转换前验证状态转换是否合法</li>
 *   <li><b>审计日志</b>：自动记录每次状态转换的详细信息</li>
 * </ul>
 *
 * <h3>状态流转规则</h3>
 * <ul>
 *   <li><b>DRAFT (0)</b>：草稿（初始状态）</li>
 *   <li><b>ISSUED (1)</b>：已开方</li>
 *   <li><b>REVIEWED (2)</b>：已审核</li>
 *   <li><b>PAID (5)</b>：已缴费</li>
 *   <li><b>DISPENSED (3)</b>：已发药（终态）</li>
 *   <li><b>REFUNDED (4)</b>：已退费（终态）</li>
 * </ul>
 *
 * <h3>合法状态转换</h3>
 * <ul>
 *   <li>DRAFT → ISSUED（医生开方）</li>
 *   <li>ISSUED → REVIEWED（药师审核）</li>
 *   <li>REVIEWED → PAID（患者缴费）</li>
 *   <li>PAID → DISPENSED（药师发药）</li>
 *   <li>DISPENSED → REFUNDED（退药退费）</li>
 *   <li>PAID → REVIEWED（退费）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生开方</b>：DRAFT → ISSUED</li>
 *   <li><b>药师审核</b>：ISSUED → REVIEWED</li>
 *   <li><b>患者缴费</b>：REVIEWED → PAID</li>
 *   <li><b>药师发药</b>：PAID → DISPENSED</li>
 *   <li><b>退药退费</b>：DISPENSED → REFUNDED</li>
 *   <li><b>退费</b>：PAID → REVIEWED</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>所有状态转换必须通过状态机执行，不得直接修改状态</li>
 *   <li>状态机自动记录审计日志，无需手动创建历史记录</li>
 *   <li>非法状态转换会抛出 IllegalStateException</li>
 *   <li>终态（DISPENSED、REFUNDED）不可逆向转换</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Prescription
 * @see PrescriptionStatusHistory
 * @see PrescriptionStatusEnum
 */
public interface PrescriptionStateMachine {

    /**
     * 执行状态转换
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>验证当前状态与目标状态的转换是否合法</li>
     *   <li>更新处方记录的状态</li>
     *   <li>自动记录状态转换审计日志</li>
     *   <li>返回更新后的Prescription对象</li>
     * </ul>
     *
     * <p><b>状态机优势：</b></p>
     * <ul>
     *   <li>统一管理：所有状态转换逻辑集中在一个地方</li>
     *   <li>合法性验证：防止非法状态转换</li>
     *   <li>审计日志：自动记录转换历史</li>
     *   <li>一致性保证：确保状态转换的原子性</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 医生开方
     * prescriptionStateMachine.transition(
     *     prescriptionId,
     *     PrescriptionStatusEnum.DRAFT,
     *     PrescriptionStatusEnum.ISSUED,
     *     currentUserId,
     *     currentUserName,
     *     "医生开方"
     * );
     *
     * // 药师审核
     * prescriptionStateMachine.transition(
     *     prescriptionId,
     *     PrescriptionStatusEnum.ISSUED,
     *     PrescriptionStatusEnum.REVIEWED,
     *     currentUserId,
     *     currentUserName,
     *     "药师审核"
     * );
     * </pre>
     *
     * @param prescriptionId 处方记录ID
     * @param fromStatus 源状态（当前状态）
     * @param toStatus 目标状态
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param reason 状态转换原因
     * @return 转换后的Prescription对象
     * @throws IllegalArgumentException 当处方记录不存在
     * @throws IllegalStateException 当状态转换不合法
     * @throws Exception 当发生其他系统异常
     */
    Prescription transition(Long prescriptionId, PrescriptionStatusEnum fromStatus, PrescriptionStatusEnum toStatus,
                          Long operatorId, String operatorName, String reason) throws Exception;

    /**
     * 验证状态转换是否合法
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>检查源状态和目标状态之间是否存在合法的转换路径</li>
     *   <li>不执行实际转换，仅验证</li>
     *   <li>用于在执行前预检查</li>
     * </ul>
     *
     * <p><b>验证规则：</b></p>
     * <ul>
     *   <li>相同状态之间不能转换</li>
     *   <li>终态（DISPENSED、REFUNDED）不能逆向转换</li>
     *   <li>必须存在预定义的状态转换路径</li>
     * </ul>
     *
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @return true=合法，false=不合法
     */
    boolean isValidTransition(PrescriptionStatusEnum fromStatus, PrescriptionStatusEnum toStatus);

    /**
     * 获取当前状态
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>查询处方记录的当前状态</li>
     *   <li>不进行任何验证或转换</li>
     *   <li>仅用于状态查询</li>
     * </ul>
     *
     * @param prescriptionId 处方记录ID
     * @return 当前状态枚举
     * @throws IllegalArgumentException 当处方记录不存在
     */
    PrescriptionStatusEnum getCurrentStatus(Long prescriptionId);

    /**
     * 获取允许的目标状态列表
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>根据当前状态，获取所有合法的目标状态</li>
     *   <li>用于前端展示可执行的操作</li>
     *   <li>不包含当前状态本身</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>前端UI：根据当前状态显示可执行的操作按钮</li>
     *   <li>权限控制：动态生成操作菜单</li>
     *   <li>状态引导：提示用户下一步可执行的操作</li>
     * </ul>
     *
     * @param currentStatus 当前状态
     * @return 允许的目标状态列表
     */
    java.util.List<PrescriptionStatusEnum> getAllowedNextStatuses(PrescriptionStatusEnum currentStatus);
}
