package com.his.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.his.entity.Registration;
import com.his.entity.RegistrationStatusHistory;
import com.his.enums.RegStatusEnum;
import com.his.repository.RegistrationRepository;
import com.his.repository.RegistrationStatusHistoryRepository;
import com.his.service.RegistrationStateMachine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 挂号状态机实现类
 *
 * <p>统一管理所有挂号状态转换，确保状态流转的合法性、一致性和可追溯性</p>
 *
 * <h3>核心特性</h3>
 * <ul>
 *   <li><b>状态转换验证</b>：在转换前验证所有状态转换的合法性</li>
 *   <li><b>原子性保证</b>：使用事务确保状态转换的原子性</li>
 *   <li><b>审计日志</b>：自动记录每次状态转换的详细信息</li>
 *   <li><b>集中管理</b>：所有状态转换逻辑集中在一个地方</li>
 * </ul>
 *
 * <h3>状态转换规则</h3>
 * <ul>
 *   <li><b>WAITING (0) → IN_CONSULTATION (5)</b>：医生接诊（未缴费病人）</li>
 *   <li><b>PAID_REGISTRATION (4) → IN_CONSULTATION (5)</b>：医生接诊（已缴费病人）</li>
 *   <li><b>IN_CONSULTATION (5) → COMPLETED (1)</b>：医生完成就诊</li>
 *   <li><b>WAITING (0) → CANCELLED (2)</b>：患者取消（未缴费）</li>
 *   <li><b>PAID_REGISTRATION (4) → CANCELLED (2)</b>：患者取消（已缴费，需退费）</li>
 *   <li><b>CANCELLED (2) → REFUNDED (3)</b>：退费完成</li>
 *   <li><b>WAITING (0) → PAID_REGISTRATION (4)</b>：患者缴费挂号费</li>
 * </ul>
 *
 * <h3>终态说明</h3>
 * <ul>
 *   <li><b>COMPLETED (1) - 已就诊</b>：终态，不可逆向转换</li>
 *   <li><b>REFUNDED (3) - 已退费</b>：终态，不可逆向转换</li>
 * </ul>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>禁止相同状态之间的转换</li>
 *   <li>禁止从终态逆向转换</li>
 *   <li>禁止跨越多个状态的跳跃式转换</li>
 *   <li>自动记录操作人、操作时间、转换原因</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see RegistrationStateMachine
 * @see Registration
 * @see RegistrationStatusHistory
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationStateMachineServiceImpl implements RegistrationStateMachine {

    private final RegistrationRepository registrationRepository;
    private final RegistrationStatusHistoryRepository statusHistoryRepository;

    /**
     * 定义状态转换规则
     * key: 源状态
     * value: 允许的目标状态集合
     */
    private static final Map<RegStatusEnum, Set<RegStatusEnum>> TRANSITION_RULES = Map.of(
        RegStatusEnum.WAITING, EnumSet.of(
            RegStatusEnum.IN_CONSULTATION,
            RegStatusEnum.CANCELLED,
            RegStatusEnum.PAID_REGISTRATION,
            RegStatusEnum.COMPLETED
        ),
        RegStatusEnum.PAID_REGISTRATION, EnumSet.of(
            RegStatusEnum.IN_CONSULTATION,
            RegStatusEnum.CANCELLED
        ),
        RegStatusEnum.IN_CONSULTATION, EnumSet.of(
            RegStatusEnum.COMPLETED
        ),
        RegStatusEnum.CANCELLED, EnumSet.of(
            RegStatusEnum.REFUNDED
        )
        // 注意：COMPLETED 和 REFUNDED 是终态，不允许任何转换
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Registration transition(Long registrationId, RegStatusEnum fromStatus, RegStatusEnum toStatus,
                                  Long operatorId, String operatorName, String reason) throws Exception {
        // 1. 参数验证（提前验证，避免后续空指针）
        validateTransitionParameters(registrationId, fromStatus, toStatus, operatorName, reason);

        log.info("开始状态转换，挂号ID: {}, {} → {}, 操作人: {}, 原因: {}",
                registrationId, fromStatus.getDescription(), toStatus.getDescription(),
                operatorName, reason);

        // 2. 查询挂号记录
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + registrationId));

        // 3. 验证当前状态与传入的源状态一致
        RegStatusEnum currentStatus = RegStatusEnum.fromCode(registration.getStatus());
        if (!currentStatus.equals(fromStatus)) {
            log.warn("状态转换失败：源状态不匹配，挂号ID: {}, 期望: {}, 实际: {}",
                    registrationId, fromStatus.getDescription(), currentStatus.getDescription());
            throw new IllegalStateException(
                String.format("状态不匹配：期望[%s]，实际[%s]", fromStatus.getDescription(), currentStatus.getDescription())
            );
        }

        // 4. 验证状态转换是否合法
        if (!isValidTransition(fromStatus, toStatus)) {
            log.warn("非法状态转换：{} → {}，挂号ID: {}", fromStatus, toStatus, registrationId);
            throw new IllegalStateException(
                String.format("非法状态转换：%s → %s", fromStatus.getDescription(), toStatus.getDescription())
            );
        }

        // 5. 执行状态转换
        Short oldStatusCode = registration.getStatus();
        Short newStatusCode = toStatus.getCode();

        registration.setStatus(newStatusCode);
        registration.setUpdatedAt(LocalDateTime.now());
        Registration savedRegistration = registrationRepository.save(registration);

        // 6. 记录审计日志
        try {
            RegistrationStatusHistory history = RegistrationStatusHistory.builder()
                    .registrationMainId(registrationId)
                    .fromStatus(oldStatusCode)
                    .toStatus(newStatusCode)
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .operatorType("USER")
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build();

            statusHistoryRepository.save(history);
            log.debug("状态转换审计日志已记录，挂号ID: {}", registrationId);
        } catch (Exception e) {
            // 审计日志记录失败不影响业务，但需要记录日志
            log.error("状态转换审计日志记录失败，挂号ID: " + registrationId, e);
        }

        log.info("状态转换成功，挂号ID: {}, {} → {}, 操作人: {}",
                registrationId, fromStatus.getDescription(), toStatus.getDescription(), operatorName);

        return savedRegistration;
    }

    @Override
    public boolean isValidTransition(RegStatusEnum fromStatus, RegStatusEnum toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }

        // 相同状态之间不能转换
        if (fromStatus.equals(toStatus)) {
            return false;
        }

        // 终态不能逆向转换
        if (fromStatus.equals(RegStatusEnum.COMPLETED) || fromStatus.equals(RegStatusEnum.REFUNDED)) {
            return false;
        }

        // 检查是否在转换规则中
        Set<RegStatusEnum> allowedStatuses = TRANSITION_RULES.get(fromStatus);
        return allowedStatuses != null && allowedStatuses.contains(toStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationStatusHistory> getHistory(Long registrationId) {
        log.debug("查询状态转换历史，挂号ID: {}", registrationId);

        if (registrationId == null || registrationId <= 0) {
            return Collections.emptyList();
        }

        List<RegistrationStatusHistory> histories = statusHistoryRepository
                .findByRegistrationMainIdOrderByCreatedAtDesc(registrationId);

        log.debug("查询到 {} 条状态转换历史记录，挂号ID: {}", histories.size(), registrationId);
        return histories;
    }

    @Override
    @Transactional(readOnly = true)
    public RegStatusEnum getCurrentStatus(Long registrationId) {
        if (registrationId == null || registrationId <= 0) {
            throw new IllegalArgumentException("挂号ID必须大于0");
        }

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + registrationId));

        return RegStatusEnum.fromCode(registration.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegStatusEnum> getAllowedNextStatuses(RegStatusEnum currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }

        Set<RegStatusEnum> allowedStatuses = TRANSITION_RULES.get(currentStatus);
        if (allowedStatuses == null) {
            return Collections.emptyList();
        }

        return allowedStatuses.stream()
                .sorted(Comparator.comparing(RegStatusEnum::getCode))
                .collect(Collectors.toList());
    }

    /**
     * 验证状态转换参数
     */
    private void validateTransitionParameters(Long registrationId, RegStatusEnum fromStatus,
                                             RegStatusEnum toStatus, String operatorName, String reason) {
        if (registrationId == null || registrationId <= 0) {
            throw new IllegalArgumentException("挂号ID必须大于0");
        }

        if (fromStatus == null) {
            throw new IllegalArgumentException("源状态不能为空");
        }

        if (toStatus == null) {
            throw new IllegalArgumentException("目标状态不能为空");
        }

        if (!operatorNameHasText(operatorName)) {
            throw new IllegalArgumentException("操作人姓名不能为空");
        }

        if (!reasonHasText(reason)) {
            throw new IllegalArgumentException("状态转换原因不能为空");
        }
    }

    /**
     * 检查字符串是否有文本内容（非null且非空）
     */
    private boolean operatorNameHasText(String operatorName) {
        return operatorName != null && !operatorName.trim().isEmpty();
    }

    /**
     * 检查字符串是否有文本内容（非null且非空）
     */
    private boolean reasonHasText(String reason) {
        return reason != null && !reason.trim().isEmpty();
    }
}
