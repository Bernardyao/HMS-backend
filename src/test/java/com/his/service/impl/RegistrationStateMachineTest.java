package com.his.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.his.entity.Registration;
import com.his.entity.RegistrationStatusHistory;
import com.his.enums.RegStatusEnum;
import com.his.repository.RegistrationRepository;
import com.his.repository.RegistrationStatusHistoryRepository;
import com.his.test.base.BaseServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RegistrationStateMachine 状态机测试
 *
 * <p>专注于测试状态机的状态转换逻辑、审计日志记录等功能</p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("RegistrationStateMachine 状态机测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationStateMachineTest extends BaseServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private RegistrationStateMachineServiceImpl stateMachine;

    // ==================== 合法状态转换测试 ====================

    @ParameterizedTest
    @DisplayName("状态转换：所有合法的状态转换都应该成功")
    @CsvSource({
        "WAITING, IN_CONSULTATION",           // 待就诊 → 就诊中（医生接诊未缴费病人）
        "PAID_REGISTRATION, IN_CONSULTATION", // 已缴挂号费 → 就诊中（医生接诊已缴费病人）
        "IN_CONSULTATION, COMPLETED",         // 就诊中 → 已就诊（医生完成就诊）
        "WAITING, CANCELLED",                 // 待就诊 → 已取消（患者取消）
        "PAID_REGISTRATION, CANCELLED",       // 已缴挂号费 → 已取消（患者取消）
        "CANCELLED, REFUNDED",                // 已取消 → 已退费（退费完成）
        "WAITING, PAID_REGISTRATION"          // 待就诊 → 已缴挂号费（患者缴费）
    })
    void transition_AllValidTransitions(String fromStatusStr, String toStatusStr) throws Exception {
        // Given - 当前状态和目标状态
        RegStatusEnum fromStatus = RegStatusEnum.valueOf(fromStatusStr);
        RegStatusEnum toStatus = RegStatusEnum.valueOf(toStatusStr);
        Long regId = 123L;
        Long operatorId = 1L;
        String operatorName = "测试医生";
        String reason = "测试原因";

        Registration registration = new Registration();
        registration.setMainId(regId);
        registration.setStatus(fromStatus.getCode());
        registration.setIsDeleted((short) 0);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());

        when(registrationRepository.findById(regId))
                .thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When - 执行状态转换
        Registration result = stateMachine.transition(regId, fromStatus, toStatus,
                operatorId, operatorName, reason);

        // Then - 状态应该成功更新
        verify(registrationRepository).save(registration);
        assertEquals(toStatus.getCode(), result.getStatus());

        // 验证审计日志被记录
        verify(statusHistoryRepository).save(any(RegistrationStatusHistory.class));
    }

    @Test
    @DisplayName("状态转换：WAITING → IN_CONSULTATION（医生接诊未缴费病人）")
    void transition_WaitingToInConsultation() throws Exception {
        // Given
        Long regId = 123L;
        Registration registration = createRegistration(regId, RegStatusEnum.WAITING);

        when(registrationRepository.findById(regId))
                .thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Registration result = stateMachine.transition(
                regId,
                RegStatusEnum.WAITING,
                RegStatusEnum.IN_CONSULTATION,
                1L,
                "医生张三",
                "医生接诊"
        );

        // Then
        assertEquals(RegStatusEnum.IN_CONSULTATION.getCode(), result.getStatus());
        verify(statusHistoryRepository).save(any(RegistrationStatusHistory.class));
    }

    @Test
    @DisplayName("状态转换：PAID_REGISTRATION → IN_CONSULTATION（医生接诊已缴费病人）")
    void transition_PaidRegistrationToInConsultation() throws Exception {
        // Given
        Long regId = 123L;
        Registration registration = createRegistration(regId, RegStatusEnum.PAID_REGISTRATION);

        when(registrationRepository.findById(regId))
                .thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Registration result = stateMachine.transition(
                regId,
                RegStatusEnum.PAID_REGISTRATION,
                RegStatusEnum.IN_CONSULTATION,
                1L,
                "医生李四",
                "医生接诊已缴费病人"
        );

        // Then
        assertEquals(RegStatusEnum.IN_CONSULTATION.getCode(), result.getStatus());
        verify(statusHistoryRepository).save(any(RegistrationStatusHistory.class));
    }

    // ==================== 非法状态转换测试 ====================

    @ParameterizedTest
    @DisplayName("状态转换：非法状态转换应该被拒绝")
    @CsvSource({
        "COMPLETED, WAITING",           // 已就诊 → 待就诊（逆向）
        "COMPLETED, IN_CONSULTATION",   // 已就诊 → 就诊中（逆向）
        "REFUNDED, WAITING",            // 已退费 → 待就诊（逆向）
        "IN_CONSULTATION, WAITING",     // 就诊中 → 待就诊（逆向）
        "IN_CONSULTATION, PAID_REGISTRATION" // 就诊中 → 已缴挂号费（逆向）
    })
    void transition_InvalidTransitions(String fromStatusStr, String toStatusStr) {
        // Given
        RegStatusEnum fromStatus = RegStatusEnum.valueOf(fromStatusStr);
        RegStatusEnum toStatus = RegStatusEnum.valueOf(toStatusStr);

        // When & Then - 应该返回false
        assertFalse(stateMachine.isValidTransition(fromStatus, toStatus));
    }

    @Test
    @DisplayName("状态转换：相同状态之间不能转换")
    void transition_SameStateNotAllowed() {
        // When & Then
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.WAITING, RegStatusEnum.WAITING));
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.COMPLETED, RegStatusEnum.COMPLETED));
    }

    // ==================== 参数验证测试 ====================

    @Test
    @DisplayName("状态转换：参数验证 - 挂号ID为null")
    void transition_NullRegistrationId() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(null, RegStatusEnum.WAITING, RegStatusEnum.IN_CONSULTATION,
                        1L, "医生", "原因"));
    }

    @Test
    @DisplayName("状态转换：参数验证 - 源状态为null")
    void transition_NullFromStatus() {
        // Given - Mock repository to avoid NPE
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(1L, null, RegStatusEnum.IN_CONSULTATION,
                        1L, "医生", "原因"));
    }

    @Test
    @DisplayName("状态转换：参数验证 - 目标状态为null")
    void transition_NullToStatus() {
        // Given - Mock repository to avoid NPE
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(1L, RegStatusEnum.WAITING, null,
                        1L, "医生", "原因"));
    }

    @Test
    @DisplayName("状态转换：参数验证 - 操作人为空")
    void transition_EmptyOperatorName() {
        // Given - Mock repository
        Registration registration = new Registration();
        registration.setMainId(1L);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(1L, RegStatusEnum.WAITING, RegStatusEnum.IN_CONSULTATION,
                        1L, "", "原因"));
    }

    @Test
    @DisplayName("状态转换：参数验证 - 转换原因为空")
    void transition_EmptyReason() {
        // Given - Mock repository
        Registration registration = new Registration();
        registration.setMainId(1L);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(1L, RegStatusEnum.WAITING, RegStatusEnum.IN_CONSULTATION,
                        1L, "医生", ""));
    }

    // ==================== 业务规则测试 ====================

    @Test
    @DisplayName("状态转换：终态不能逆向转换")
    void transition_TerminalStatesCannotTransition() {
        // COMPLETED 和 REFUNDED 是终态，不能逆向转换
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.COMPLETED, RegStatusEnum.WAITING));
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.COMPLETED, RegStatusEnum.IN_CONSULTATION));
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.REFUNDED, RegStatusEnum.WAITING));
        assertFalse(stateMachine.isValidTransition(RegStatusEnum.REFUNDED, RegStatusEnum.CANCELLED));
    }

    // ==================== 查询功能测试 ====================

    @Test
    @DisplayName("状态查询：获取当前状态")
    void getCurrentStatus_Success() {
        // Given
        Long regId = 123L;
        Registration registration = createRegistration(regId, RegStatusEnum.WAITING);

        when(registrationRepository.findById(regId))
                .thenReturn(Optional.of(registration));

        // When
        RegStatusEnum currentStatus = stateMachine.getCurrentStatus(regId);

        // Then
        assertEquals(RegStatusEnum.WAITING, currentStatus);
    }

    @Test
    @DisplayName("状态查询：获取允许的下一状态")
    void getAllowedNextStatuses_Success() {
        // Given
        RegStatusEnum currentStatus = RegStatusEnum.WAITING;

        // When
        List<RegStatusEnum> allowedStatuses = stateMachine.getAllowedNextStatuses(currentStatus);

        // Then
        assertTrue(allowedStatuses.contains(RegStatusEnum.IN_CONSULTATION));
        assertTrue(allowedStatuses.contains(RegStatusEnum.CANCELLED));
        assertTrue(allowedStatuses.contains(RegStatusEnum.PAID_REGISTRATION));
        assertFalse(allowedStatuses.contains(RegStatusEnum.WAITING)); // 不能是当前状态
    }

    @Test
    @DisplayName("历史查询：获取状态转换历史")
    void getHistory_Success() {
        // Given
        Long regId = 123L;

        // When
        List<RegistrationStatusHistory> histories = stateMachine.getHistory(regId);

        // Then
        // 由于没有mock数据，返回空列表
        assertNotNull(histories);
        assertTrue(histories.isEmpty());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的Registration对象
     */
    private Registration createRegistration(Long id, RegStatusEnum status) {
        Registration registration = new Registration();
        registration.setMainId(id);
        registration.setStatus(status.getCode());
        registration.setIsDeleted((short) 0);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());
        return registration;
    }
}
