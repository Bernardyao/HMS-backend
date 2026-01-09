package com.his.service.impl;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.his.entity.Prescription;
import com.his.enums.PrescriptionStatusEnum;
import com.his.repository.PrescriptionRepository;
import com.his.service.PrescriptionStateMachine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 处方状态机服务实现类
 *
 * <p>统一管理处方状态转换，确保状态流转的合法性和一致性</p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li><b>状态转换</b>：验证并执行合法的状态转换</li>
 *   <li><b>转换验证</b>：在转换前验证状态转换是否合法</li>
 *   <li><b>审计日志</b>：自动记录每次状态转换的详细信息</li>
 *   <li><b>原子性</b>：确保状态转换的原子性</li>
 * </ul>
 *
 * <h3>状态转换规则</h3>
 * <ul>
 *   <li>DRAFT (0) → ISSUED (1)</li>
 *   <li>ISSUED (1) → REVIEWED (2)</li>
 *   <li>REVIEWED (2) → PAID (5)</li>
 *   <li>PAID (5) → DISPENSED (3)</li>
 *   <li>DISPENSED (3) → REFUNDED (4)</li>
 *   <li>PAID (5) → REVIEWED (2)</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionStateMachineServiceImpl implements PrescriptionStateMachine {

    private final PrescriptionRepository prescriptionRepository;

    /**
     * 状态转换规则映射
     * Key: 源状态
     * Value: 允许的目标状态集合
     */
    private static final Map<PrescriptionStatusEnum, Set<PrescriptionStatusEnum>> TRANSITION_RULES =
            new EnumMap<>(PrescriptionStatusEnum.class);

    static {
        // 初始化状态转换规则
        // DRAFT 可以转换为 ISSUED
        TRANSITION_RULES.put(PrescriptionStatusEnum.DRAFT,
                EnumSet.of(PrescriptionStatusEnum.ISSUED));

        // ISSUED 可以转换为 REVIEWED
        TRANSITION_RULES.put(PrescriptionStatusEnum.ISSUED,
                EnumSet.of(PrescriptionStatusEnum.REVIEWED));

        // REVIEWED 可以转换为 PAID
        TRANSITION_RULES.put(PrescriptionStatusEnum.REVIEWED,
                EnumSet.of(PrescriptionStatusEnum.PAID));

        // PAID 可以转换为 DISPENSED 或 REVIEWED（退费）
        TRANSITION_RULES.put(PrescriptionStatusEnum.PAID,
                EnumSet.of(PrescriptionStatusEnum.DISPENSED, PrescriptionStatusEnum.REVIEWED));

        // DISPENSED 可以转换为 REFUNDED
        TRANSITION_RULES.put(PrescriptionStatusEnum.DISPENSED,
                EnumSet.of(PrescriptionStatusEnum.REFUNDED));

        // REFUNDED 是终态，没有后续转换
        TRANSITION_RULES.put(PrescriptionStatusEnum.REFUNDED, EnumSet.noneOf(PrescriptionStatusEnum.class));
    }

    @Override
    public Prescription transition(Long prescriptionId, PrescriptionStatusEnum fromStatus,
                                   PrescriptionStatusEnum toStatus, Long operatorId,
                                   String operatorName, String reason) throws Exception {
        log.info("处方状态机转换，处方ID: {}, 源状态: {}, 目标状态: {}, 操作人: {}, 原因: {}",
                prescriptionId, fromStatus.getDescription(), toStatus.getDescription(),
                operatorName, reason);

        // 1. 验证状态转换是否合法
        if (!isValidTransition(fromStatus, toStatus)) {
            String errorMsg = String.format("无效的处方状态转换: %s -> %s",
                    fromStatus.getDescription(), toStatus.getDescription());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 2. 查询处方记录
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> {
                    String errorMsg = "处方记录不存在，ID: " + prescriptionId;
                    log.error(errorMsg);
                    return new IllegalArgumentException(errorMsg);
                });

        // 3. 验证当前状态是否匹配
        PrescriptionStatusEnum currentStatus = PrescriptionStatusEnum.fromCode(prescription.getStatus());
        if (!currentStatus.equals(fromStatus)) {
            String errorMsg = String.format("处方状态不匹配: 期望 %s，实际 %s",
                    fromStatus.getDescription(), currentStatus.getDescription());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 4. 执行状态转换
        log.info("处方状态机转换通过验证，执行转换，处方ID: {}", prescriptionId);
        prescription.setStatus(toStatus.getCode());
        prescription.setUpdatedAt(LocalDateTime.now());

        // 5. 保存更新后的处方
        Prescription savedPrescription = prescriptionRepository.save(prescription);

        // 6. 记录审计日志
        log.info("处方状态机转换成功，处方ID: {}, 原状态: {}, 新状态: {}",
                prescriptionId, fromStatus.getDescription(), toStatus.getDescription());

        return savedPrescription;
    }

    @Override
    public boolean isValidTransition(PrescriptionStatusEnum fromStatus, PrescriptionStatusEnum toStatus) {
        // 相同状态之间的转换无效
        if (fromStatus.equals(toStatus)) {
            return false;
        }

        // 检查是否存在转换规则
        Set<PrescriptionStatusEnum> allowedStatuses = TRANSITION_RULES.get(fromStatus);
        if (allowedStatuses == null) {
            return false;
        }

        return allowedStatuses.contains(toStatus);
    }

    @Override
    public PrescriptionStatusEnum getCurrentStatus(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("处方记录不存在，ID: " + prescriptionId));
        return PrescriptionStatusEnum.fromCode(prescription.getStatus());
    }

    @Override
    public java.util.List<PrescriptionStatusEnum> getAllowedNextStatuses(PrescriptionStatusEnum currentStatus) {
        Set<PrescriptionStatusEnum> allowedStatuses = TRANSITION_RULES.get(currentStatus);
        if (allowedStatuses == null) {
            return java.util.Collections.emptyList();
        }
        return new java.util.ArrayList<>(allowedStatuses);
    }
}
