package com.his.service;

import java.util.List;

import com.his.entity.Registration;
import com.his.entity.RegistrationStatusHistory;
import com.his.enums.RegStatusEnum;

/**
 * 挂号状态机接口
 *
 * <p>统一管理挂号状态转换，确保状态流转的合法性和一致性</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>状态转换</b>：验证并执行合法的状态转换</li>
 *   <li><b>转换验证</b>：在转换前验证状态转换是否合法</li>
 *   <li><b>审计日志</b>：自动记录每次状态转换的详细信息</li>
 *   <li><b>历史查询</b>：查询指定挂号的状态转换历史</li>
 * </ul>
 *
 * <h3>状态流转规则</h3>
 * <ul>
 *   <li><b>WAITING (0)</b>：待就诊（初始状态）</li>
 *   <li><b>PAID_REGISTRATION (4)</b>：已缴挂号费</li>
 *   <li><b>IN_CONSULTATION (5)</b>：就诊中</li>
 *   <li><b>COMPLETED (1)</b>：已就诊（终态）</li>
 *   <li><b>CANCELLED (2)</b>：已取消</li>
 *   <li><b>REFUNDED (3)</b>：已退费（终态）</li>
 * </ul>
 *
 * <h3>合法状态转换</h3>
 * <ul>
 *   <li>WAITING → IN_CONSULTATION（医生接诊未缴费病人）</li>
 *   <li>PAID_REGISTRATION → IN_CONSULTATION（医生接诊已缴费病人）</li>
 *   <li>IN_CONSULTATION → COMPLETED（医生完成就诊）</li>
 *   <li>WAITING → CANCELLED（患者取消）</li>
 *   <li>PAID_REGISTRATION → CANCELLED（患者取消）</li>
 *   <li>CANCELLED → REFUNDED（退费）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生接诊</b>：WAITING/PAID_REGISTRATION → IN_CONSULTATION</li>
 *   <li><b>完成就诊</b>：IN_CONSULTATION → COMPLETED</li>
 *   <li><b>取消挂号</b>：WAITING/PAID_REGISTRATION → CANCELLED</li>
 *   <li><b>退费处理</b>：CANCELLED → REFUNDED</li>
 *   <li><b>缴费处理</b>：WAITING → PAID_REGISTRATION</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>所有状态转换必须通过状态机执行，不得直接修改状态</li>
 *   <li>状态机自动记录审计日志，无需手动创建历史记录</li>
 *   <li>非法状态转换会抛出 IllegalStateException</li>
 *   <li>终态（COMPLETED、REFUNDED）不可逆向转换</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Registration
 * @see RegistrationStatusHistory
 * @see RegStatusEnum
 */
public interface RegistrationStateMachine {

    /**
     * 执行状态转换
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>验证当前状态与目标状态的转换是否合法</li>
     *   <li>更新挂号记录的状态</li>
     *   <li>自动记录状态转换审计日志</li>
     *   <li>返回更新后的Registration对象</li>
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
     * // 医生接诊
     * registrationStateMachine.transition(
     *     registrationId,
     *     RegStatusEnum.WAITING,
     *     RegStatusEnum.IN_CONSULTATION,
     *     currentUserId,
     *     currentUserName,
     *     "医生接诊"
     * );
     *
     * // 完成就诊
     * registrationStateMachine.transition(
     *     registrationId,
     *     RegStatusEnum.IN_CONSULTATION,
     *     RegStatusEnum.COMPLETED,
     *     currentUserId,
     *     currentUserName,
     *     "完成就诊"
     * );
     * </pre>
     *
     * @param registrationId 挂号记录ID
     * @param fromStatus 源状态（当前状态）
     * @param toStatus 目标状态
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param reason 状态转换原因
     * @return 转换后的Registration对象
     * @throws IllegalArgumentException 当挂号记录不存在
     * @throws IllegalStateException 当状态转换不合法
     * @throws Exception 当发生其他系统异常
     */
    Registration transition(Long registrationId, RegStatusEnum fromStatus, RegStatusEnum toStatus,
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
     *   <li>终态（COMPLETED、REFUNDED）不能逆向转换</li>
     *   <li>必须存在预定义的状态转换路径</li>
     * </ul>
     *
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @return true=合法，false=不合法
     */
    boolean isValidTransition(RegStatusEnum fromStatus, RegStatusEnum toStatus);

    /**
     * 获取状态转换历史
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>查询指定挂号的所有状态转换历史</li>
     *   <li>按转换时间倒序排列（最新的在前）</li>
     *   <li>包含详细的转换信息（源状态、目标状态、操作人、时间、原因）</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>问题排查：查看状态异常变更历史</li>
     *   <li>业务审计：追踪状态转换流程</li>
     *   <li>数据分析：分析医生接诊效率</li>
     *   <li>用户查询：展示给用户看状态变更历程</li>
     * </ul>
     *
     * @param registrationId 挂号记录ID
     * @return 状态转换历史列表（按时间倒序）
     */
    List<RegistrationStatusHistory> getHistory(Long registrationId);

    /**
     * 获取当前状态
     *
     * <p><b>功能说明：</b></p>
     * <ul>
     *   <li>查询挂号记录的当前状态</li>
     *   <li>不进行任何验证或转换</li>
     *   <li>仅用于状态查询</li>
     * </ul>
     *
     * @param registrationId 挂号记录ID
     * @return 当前状态枚举
     * @throws IllegalArgumentException 当挂号记录不存在
     */
    RegStatusEnum getCurrentStatus(Long registrationId);

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
    List<RegStatusEnum> getAllowedNextStatuses(RegStatusEnum currentStatus);
}
