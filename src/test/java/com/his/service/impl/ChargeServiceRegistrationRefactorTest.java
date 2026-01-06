package com.his.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeDetailRepository;
import com.his.repository.ChargeRepository;
import com.his.repository.PrescriptionRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.PrescriptionService;
import com.his.service.RegistrationStateMachine;
import com.his.test.base.BaseServiceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("收费服务重构测试 - 挂号状态机集成")
class ChargeServiceRegistrationRefactorTest extends BaseServiceTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeDetailRepository chargeDetailRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private PrescriptionService prescriptionService;
    @Mock
    private com.his.monitoring.SequenceGenerationMetrics sequenceMetrics;
    @Mock
    private RegistrationStateMachine registrationStateMachine;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    @Test
    @DisplayName("测试支付挂号费：应使用状态机更新状态")
    void processPayment_ShouldUseStateMachineForStatusUpdate() throws Exception {
        // Given
        Long chargeId = 1000L;
        Long registrationId = 1L;
        String transactionNo = "WX202312271001";

        com.his.entity.Charge charge = new com.his.entity.Charge();
        charge.setMainId(chargeId);
        charge.setStatus(com.his.enums.ChargeStatusEnum.UNPAID.getCode());
        charge.setTotalAmount(new BigDecimal("10.00"));
        charge.setActualAmount(new BigDecimal("10.00"));
        charge.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        charge.setPatient(patient);

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        charge.setRegistration(registration);

        com.his.entity.ChargeDetail detail = new com.his.entity.ChargeDetail();
        detail.setItemType("REGISTRATION");
        detail.setItemId(registrationId);
        charge.setDetails(Arrays.asList(detail));

        com.his.dto.PaymentDTO paymentDTO = new com.his.dto.PaymentDTO();
        paymentDTO.setPaymentMethod(com.his.enums.PaymentMethodEnum.WECHAT.getCode());
        paymentDTO.setTransactionNo(transactionNo);
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        when(chargeRepository.findByIdWithDetails(chargeId)).thenReturn(Optional.of(charge));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        chargeService.processPayment(chargeId, paymentDTO);

        // Then
        // 验证调用了状态机的 transition 方法
        verify(registrationStateMachine).transition(
            eq(registrationId),
            eq(RegStatusEnum.WAITING),
            eq(RegStatusEnum.PAID_REGISTRATION),
            isNull(), // operatorId (ChargeServiceImpl 目前不传 user info，可能需要从 context 获取或者传 null)
            isNull(), // operatorName
            contains("收费单支付") // reason
        );

        // 验证不再直接调用 registrationRepository.save 来更新状态
        // 注意：ChargeServiceImpl 可能会在其他地方使用 registrationRepository，所以这里要精确验证
        // 这里假设原来的代码是 registrationRepository.save(registration)
        // 我们希望在 refactor 后，状态更新由 state machine 处理，而 charge service 不再直接保存 registration
        verify(registrationRepository, never()).save(any(Registration.class));
    }
}
