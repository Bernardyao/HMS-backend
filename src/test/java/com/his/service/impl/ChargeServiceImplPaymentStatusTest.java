package com.his.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.ChargeTypeEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.PrescriptionStateMachine;
import com.his.service.RegistrationStateMachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 收费服务-支付后状态更新测试
 *
 * <p>验证支付成功后，挂号单和处方状态是否正确更新，避免重复收费问题</p>
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("收费服务-支付后状态更新测试")
class ChargeServiceImplPaymentStatusTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private ChargeDetailRepository chargeDetailRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private RegistrationStateMachine registrationStateMachine;

    @Mock
    private PrescriptionStateMachine prescriptionStateMachine;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    private Patient patient;
    private Doctor doctor;
    private Registration registration;
    private Prescription prescription;
    private Charge charge;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        patient = new Patient();
        patient.setMainId(1L);
        patient.setName("测试患者");

        doctor = new Doctor();
        doctor.setMainId(1L);
        doctor.setName("测试医生");

        registration = new Registration();
        registration.setMainId(1L);
        registration.setPatient(patient);
        registration.setDoctor(doctor);
        registration.setRegistrationFee(BigDecimal.valueOf(10.00));
        registration.setStatus(RegStatusEnum.WAITING.getCode()); // 初始状态：待就诊

        prescription = new Prescription();
        prescription.setMainId(1L);
        prescription.setPrescriptionNo("PRE20260108000001");
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setTotalAmount(BigDecimal.valueOf(50.00));
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode()); // 已审核
    }

    @Test
    @DisplayName("支付挂号费 - 状态从WAITING更新为PAID_REGISTRATION")
    void testPaymentRegistrationFee_UpdateStatusFromWaitingToPaid() throws Exception {
        // 【场景1】患者首次支付挂号费，状态从WAITING更新为PAID_REGISTRATION

        // 准备收费单数据
        charge = createCharge(ChargeTypeEnum.REGISTRATION_ONLY.getCode(), RegStatusEnum.WAITING.getCode());

        // 添加挂号费明细
        List<ChargeDetail> details = new ArrayList<>();
        ChargeDetail detail = new ChargeDetail();
        detail.setItemType("REGISTRATION");
        detail.setItemId(registration.getMainId());
        detail.setCharge(charge);
        details.add(detail);
        charge.setDetails(details);

        // Mock查询
        when(chargeRepository.findByIdWithDetails(anyLong()))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行支付
        PaymentDTO payment = new PaymentDTO();
        payment.setPaidAmount(BigDecimal.valueOf(10.00));
        payment.setPaymentMethod((short) 1);
        payment.setTransactionNo("TXN20260108000001");

        // 验证状态机调用
        // 【修复后】现在会检查当前状态并进行相应处理
        when(registrationStateMachine.transition(
                eq(1L),
                eq(RegStatusEnum.WAITING),
                eq(RegStatusEnum.PAID_REGISTRATION),
                any(),
                any(),
                eq("支付挂号费")
        )).thenReturn(registration);

        // 执行测试
        var result = chargeService.processPayment(1L, payment);

        // 验证结果
        assertNotNull(result);
        assertEquals(ChargeStatusEnum.PAID.getCode(), result.getStatus());

        // 验证状态机被正确调用
        verify(registrationStateMachine, times(1)).transition(
                eq(1L),
                eq(RegStatusEnum.WAITING),
                eq(RegStatusEnum.PAID_REGISTRATION),
                any(),
                any(),
                eq("支付挂号费")
        );

        System.out.println("✅ 测试通过：支付挂号费后，状态正确从WAITING更新为PAID_REGISTRATION");
    }

    @Test
    @DisplayName("重复支付挂号费 - 状态已是PAID_REGISTRATION跳过更新")
    void testDuplicatePaymentRegistrationFee_SkipUpdateWhenAlreadyPaid() throws Exception {
        // 【场景2】患者重复支付挂号费，状态已是PAID_REGISTRATION，直接跳过更新

        // 准备收费单数据
        charge = createCharge(ChargeTypeEnum.REGISTRATION_ONLY.getCode(), RegStatusEnum.PAID_REGISTRATION.getCode());

        // Mock查询 - 挂号单状态已经是PAID_REGISTRATION
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());

        when(chargeRepository.findByIdWithDetails(anyLong()))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行支付
        PaymentDTO payment = new PaymentDTO();
        payment.setPaidAmount(BigDecimal.valueOf(10.00));
        payment.setPaymentMethod((short) 1);
        payment.setTransactionNo("TXN20260108000002");

        // 【修复后】现在会检查当前状态，如果是PAID_REGISTRATION则跳过更新
        // 执行测试
        var result = chargeService.processPayment(1L, payment);

        // 验证结果
        assertNotNull(result);
        assertEquals(ChargeStatusEnum.PAID.getCode(), result.getStatus());

        // 验证状态机不被调用（因为状态已经是PAID_REGISTRATION）
        verify(registrationStateMachine, never()).transition(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        System.out.println("✅ 测试通过：重复支付时，状态已是PAID_REGISTRATION，跳过更新");
    }

    @Test
    @DisplayName("支付处方费 - 处方状态从REVIEWED更新为PAID")
    void testPaymentPrescriptionFee_UpdatePrescriptionStatus() throws Exception {
        // 【场景3】患者支付处方费，处方状态从REVIEWED更新为PAID

        // 准备收费单数据
        charge = createCharge(ChargeTypeEnum.PRESCRIPTION_ONLY.getCode(), RegStatusEnum.PAID_REGISTRATION.getCode());
        // 设置正确的处方费金额
        charge.setTotalAmount(BigDecimal.valueOf(50.00));
        charge.setActualAmount(BigDecimal.valueOf(50.00));

        // 添加处方收费明细
        List<ChargeDetail> details = new ArrayList<>();
        ChargeDetail detail = new ChargeDetail();
        detail.setItemType("PRESCRIPTION");
        detail.setItemId(prescription.getMainId());
        detail.setCharge(charge);
        details.add(detail);
        charge.setDetails(details);

        // Mock查询
        when(chargeRepository.findByIdWithDetails(anyLong()))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(prescriptionRepository.findById(anyLong()))
                .thenReturn(Optional.of(prescription));

        // 【精确Mock】只Mock会被调用的处方状态机，不Mock挂号状态机（避免UnnecessaryStubbingException）
        // 使用参数匹配确保Mock精确命中
        doReturn(prescription).when(prescriptionStateMachine).transition(
                eq(prescription.getMainId()),
                eq(PrescriptionStatusEnum.REVIEWED),
                eq(PrescriptionStatusEnum.PAID),
                any(), anyString(), anyString());

        // 执行支付
        PaymentDTO payment = new PaymentDTO();
        payment.setPaidAmount(BigDecimal.valueOf(50.00));
        payment.setPaymentMethod((short) 1);
        payment.setTransactionNo("TXN20260108000003");

        // 执行测试
        var result = chargeService.processPayment(1L, payment);

        // 验证结果
        assertNotNull(result);
        assertEquals(ChargeStatusEnum.PAID.getCode(), result.getStatus());

        // 验证处方状态机被调用
        verify(prescriptionStateMachine, times(1)).transition(
                eq(prescription.getMainId()),
                eq(PrescriptionStatusEnum.REVIEWED),
                eq(PrescriptionStatusEnum.PAID),
                any(), anyString(), anyString());

        System.out.println("✅ 测试通过：支付处方费后，处方状态正确从REVIEWED更新为PAID");
    }

    @Test
    @DisplayName("混合收费 - 挂号费和处方费一起支付")
    void testMixedPayment_UpdateBothRegistrationAndPrescriptionStatus() throws Exception {
        // 【场景4】患者混合收费（挂号费+处方费），同时更新挂号和处方状态

        // 准备收费单数据
        charge = createCharge(ChargeTypeEnum.MIXED.getCode(), RegStatusEnum.WAITING.getCode());
        // 设置正确的总金额（挂号费10.00 + 处方费50.00）
        charge.setTotalAmount(BigDecimal.valueOf(60.00));
        charge.setActualAmount(BigDecimal.valueOf(60.00));

        // 添加收费明细
        List<ChargeDetail> details = new ArrayList<>();

        // 挂号费明细
        ChargeDetail regDetail = new ChargeDetail();
        regDetail.setItemType("REGISTRATION");
        regDetail.setItemId(registration.getMainId());
        regDetail.setCharge(charge);
        details.add(regDetail);

        // 处方费明细
        ChargeDetail presDetail = new ChargeDetail();
        presDetail.setItemType("PRESCRIPTION");
        presDetail.setItemId(prescription.getMainId());
        presDetail.setCharge(charge);
        details.add(presDetail);

        charge.setDetails(details);

        // Mock查询
        when(chargeRepository.findByIdWithDetails(anyLong()))
                .thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(prescriptionRepository.findById(anyLong()))
                .thenReturn(Optional.of(prescription));

        // Mock状态机
        doReturn(registration).when(registrationStateMachine).transition(
                eq(1L),
                eq(RegStatusEnum.WAITING),
                eq(RegStatusEnum.PAID_REGISTRATION),
                any(),
                any(),
                eq("支付挂号费")
        );
        doReturn(prescription).when(prescriptionStateMachine).transition(
                eq(prescription.getMainId()),
                eq(PrescriptionStatusEnum.REVIEWED),
                eq(PrescriptionStatusEnum.PAID),
                any(), anyString(), anyString()
        );

        // 执行支付
        PaymentDTO payment = new PaymentDTO();
        payment.setPaidAmount(BigDecimal.valueOf(60.00)); // 10.00挂号费 + 50.00处方费
        payment.setPaymentMethod((short) 1);
        payment.setTransactionNo("TXN20260108000004");

        // 执行测试
        var result = chargeService.processPayment(1L, payment);

        // 验证结果
        assertNotNull(result);
        assertEquals(ChargeStatusEnum.PAID.getCode(), result.getStatus());

        // 验证状态机被调用（更新挂号状态）
        verify(registrationStateMachine, times(1)).transition(
                eq(1L),
                eq(RegStatusEnum.WAITING),
                eq(RegStatusEnum.PAID_REGISTRATION),
                any(),
                any(),
                eq("支付挂号费")
        );

        // 验证处方状态机被调用
        verify(prescriptionStateMachine, times(1)).transition(
                eq(prescription.getMainId()),
                eq(PrescriptionStatusEnum.REVIEWED),
                eq(PrescriptionStatusEnum.PAID),
                any(), anyString(), anyString());

        System.out.println("✅ 测试通过：混合收费时，挂号和处方状态都正确更新");
    }

    /**
     * 创建测试用收费单
     */
    private Charge createCharge(Short chargeType, Short registrationStatus) {
        Charge charge = new Charge();
        charge.setMainId(1L);
        charge.setChargeNo("CHG20260108000001");
        charge.setPatient(patient);
        charge.setRegistration(registration);
        charge.setChargeType(chargeType);
        charge.setTotalAmount(BigDecimal.valueOf(10.00));
        charge.setActualAmount(BigDecimal.valueOf(10.00));
        charge.setStatus(ChargeStatusEnum.UNPAID.getCode());
        charge.setCreatedAt(LocalDateTime.now());
        charge.setUpdatedAt(LocalDateTime.now());

        // 设置挂号单状态
        registration.setStatus(registrationStatus);

        return charge;
    }
}
