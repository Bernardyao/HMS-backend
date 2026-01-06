package com.his.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.his.dto.CreateChargeDTO;
import com.his.entity.Patient;
import com.his.entity.Prescription;
import com.his.entity.Registration;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeDetailRepository;
import com.his.repository.ChargeRepository;
import com.his.repository.PrescriptionRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.PrescriptionService;
import com.his.service.RegistrationStateMachine;
import com.his.test.base.BaseServiceTest;
import com.his.vo.ChargeVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("收费服务测试")
class ChargeServiceImplTest extends BaseServiceTest {

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
    private RegistrationStateMachine registrationStateMachine;
    @Mock
    private com.his.monitoring.SequenceGenerationMetrics sequenceMetrics;
    @Mock
    private com.his.monitoring.ChargeServiceMetrics chargeMetrics;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    @Test
    @DisplayName("测试创建收费单：成功场景")
    void createCharge_Success() {
        // Given
        Long registrationId = 1L;
        Long prescriptionId = 10L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.COMPLETED.getCode());
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        registration.setPatient(patient);

        Prescription prescription = new Prescription();
        prescription.setMainId(prescriptionId);
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
        prescription.setTotalAmount(new BigDecimal("90.00"));
        prescription.setIsDeleted((short) 0);

        CreateChargeDTO dto = new CreateChargeDTO();
        dto.setRegistrationId(registrationId);
        dto.setPrescriptionIds(Arrays.asList(prescriptionId));

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(prescriptionRepository.findAllById(dto.getPrescriptionIds())).thenReturn(Arrays.asList(prescription));
        // Mock挂号费已支付（分阶段收费场景）
        when(chargeRepository.isRegistrationFeePaidOptimized(registrationId)).thenReturn(true);
        when(chargeRepository.isPrescriptionFeePaidOptimized(prescriptionId)).thenReturn(false);
        when(chargeRepository.save(any())).thenAnswer(inv -> {
            com.his.entity.Charge c = inv.getArgument(0);
            c.setMainId(1000L);
            return c;
        });

        // When
        ChargeVO result = chargeService.createCharge(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("90.00");  // 分阶段收费：挂号费已支付，只收处方费
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDetails()).hasSize(1);  // 只有1个明细（处方）

        verify(chargeRepository).save(any());
    }

    @Test
    @DisplayName("测试支付：成功场景")
    void processPayment_Success() {
        // Given
        Long chargeId = 1000L;
        String transactionNo = "WX202312271001";

        com.his.entity.Charge charge = new com.his.entity.Charge();
        charge.setMainId(chargeId);
        charge.setStatus(com.his.enums.ChargeStatusEnum.UNPAID.getCode());
        charge.setTotalAmount(new BigDecimal("100.00"));
        charge.setActualAmount(new BigDecimal("100.00"));
        charge.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        charge.setPatient(patient);

        com.his.dto.PaymentDTO paymentDTO = new com.his.dto.PaymentDTO();
        paymentDTO.setPaymentMethod(com.his.enums.PaymentMethodEnum.WECHAT.getCode());
        paymentDTO.setTransactionNo(transactionNo);
        paymentDTO.setPaidAmount(new BigDecimal("100.00"));

        when(chargeRepository.findByIdWithDetails(chargeId)).thenReturn(Optional.of(charge));

        // Mock prescription update
        com.his.entity.ChargeDetail detail = new com.his.entity.ChargeDetail();
        detail.setItemType("PRESCRIPTION");
        detail.setItemId(10L);
        charge.setDetails(Arrays.asList(detail));

        Prescription prescription = new Prescription();
        prescription.setMainId(10L);
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
        when(prescriptionRepository.findById(10L)).thenReturn(Optional.of(prescription));

        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChargeVO result = chargeService.processPayment(chargeId, paymentDTO);

        // Then
        assertThat(result.getStatus()).isEqualTo(com.his.enums.ChargeStatusEnum.PAID.getCode());
        verify(prescriptionRepository).save(argThat(p -> p.getStatus().equals(PrescriptionStatusEnum.PAID.getCode())));
    }

    @Test
    @DisplayName("测试支付：幂等性")
    void processPayment_Idempotent() {
        Long chargeId = 1000L;
        String transactionNo = "WX202312271001";

        com.his.entity.Charge charge = new com.his.entity.Charge();
        charge.setMainId(chargeId);
        charge.setStatus(com.his.enums.ChargeStatusEnum.PAID.getCode()); // Already PAID
        charge.setTransactionNo(transactionNo);
        charge.setTotalAmount(new BigDecimal("100.00"));
        charge.setActualAmount(new BigDecimal("100.00"));
        charge.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        charge.setPatient(patient);

        com.his.dto.PaymentDTO paymentDTO = new com.his.dto.PaymentDTO();
        paymentDTO.setPaymentMethod(com.his.enums.PaymentMethodEnum.WECHAT.getCode());
        paymentDTO.setTransactionNo(transactionNo);
        paymentDTO.setPaidAmount(new BigDecimal("100.00"));

        when(chargeRepository.findByTransactionNoWithDetails(transactionNo)).thenReturn(Optional.of(charge));

        // When
        ChargeVO result = chargeService.processPayment(chargeId, paymentDTO);

        // Then
        assertThat(result.getStatus()).isEqualTo(com.his.enums.ChargeStatusEnum.PAID.getCode());
        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试退费：成功场景（处方仅缴费未发药）")
    void processRefund_Success_NotDispensed() {
        // Given
        Long chargeId = 1000L;
        String refundReason = "Patient allergy";

        com.his.entity.Charge charge = new com.his.entity.Charge();
        charge.setMainId(chargeId);
        charge.setStatus(com.his.enums.ChargeStatusEnum.PAID.getCode());
        charge.setTotalAmount(new BigDecimal("100.00"));
        charge.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        charge.setPatient(patient);

        com.his.entity.ChargeDetail detail = new com.his.entity.ChargeDetail();
        detail.setItemType("PRESCRIPTION");
        detail.setItemId(10L);
        charge.setDetails(Arrays.asList(detail));

        Prescription prescription = new Prescription();
        prescription.setMainId(10L);
        prescription.setStatus(PrescriptionStatusEnum.PAID.getCode()); // 已缴费

        when(chargeRepository.findByIdWithDetails(chargeId)).thenReturn(Optional.of(charge));
        when(prescriptionRepository.findById(10L)).thenReturn(Optional.of(prescription));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChargeVO result = chargeService.processRefund(chargeId, refundReason);

        // Then
        assertThat(result.getStatus()).isEqualTo(com.his.enums.ChargeStatusEnum.REFUNDED.getCode());
        verify(prescriptionRepository).save(argThat(p -> p.getStatus().equals(PrescriptionStatusEnum.REVIEWED.getCode())));
        verify(prescriptionService, never()).restoreInventoryOnly(anyLong());
    }

    @Test
    @DisplayName("测试退费：成功场景（处方已发药）")
    void processRefund_Success_Dispensed() {
        // Given
        Long chargeId = 1000L;
        String refundReason = "Patient request";

        com.his.entity.Charge charge = new com.his.entity.Charge();
        charge.setMainId(chargeId);
        charge.setStatus(com.his.enums.ChargeStatusEnum.PAID.getCode());
        charge.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        charge.setPatient(patient);

        com.his.entity.ChargeDetail detail = new com.his.entity.ChargeDetail();
        detail.setItemType("PRESCRIPTION");
        detail.setItemId(10L);
        charge.setDetails(Arrays.asList(detail));

        Prescription prescription = new Prescription();
        prescription.setMainId(10L);
        prescription.setStatus(PrescriptionStatusEnum.DISPENSED.getCode()); // 已发药

        when(chargeRepository.findByIdWithDetails(chargeId)).thenReturn(Optional.of(charge));
        when(prescriptionRepository.findById(10L)).thenReturn(Optional.of(prescription));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChargeVO result = chargeService.processRefund(chargeId, refundReason);

        // Then
        assertThat(result.getStatus()).isEqualTo(com.his.enums.ChargeStatusEnum.REFUNDED.getCode());
        verify(prescriptionRepository).save(argThat(p -> p.getStatus().equals(PrescriptionStatusEnum.REFUNDED.getCode())));
        verify(prescriptionService).restoreInventoryOnly(10L);
    }

    @Test
    @DisplayName("测试每日结算：成功场景")
    void getDailySettlement_Success() {
        // Given
        java.time.LocalDate date = java.time.LocalDate.now();

        com.his.entity.Charge c1 = new com.his.entity.Charge();
        c1.setStatus(com.his.enums.ChargeStatusEnum.PAID.getCode());
        c1.setPaymentMethod(com.his.enums.PaymentMethodEnum.WECHAT.getCode());
        c1.setActualAmount(new BigDecimal("100.00"));

        com.his.entity.Charge c2 = new com.his.entity.Charge();
        c2.setStatus(com.his.enums.ChargeStatusEnum.REFUNDED.getCode());
        c2.setPaymentMethod(com.his.enums.PaymentMethodEnum.ALIPAY.getCode());
        c2.setActualAmount(new BigDecimal("50.00"));
        c2.setRefundAmount(new BigDecimal("50.00"));

        when(chargeRepository.findByChargeTimeRange(any(), any())).thenReturn(java.util.Arrays.asList(c1, c2));

        // When
        com.his.vo.DailySettlementVO result = chargeService.getDailySettlement(date);

        // Then
        assertThat(result.getTotalCharges()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(result.getRefunds().getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getNetCollection()).isEqualByComparingTo("100.00");

        assertThat(result.getPaymentBreakdown().get("WECHAT").getAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getPaymentBreakdown().get("ALIPAY").getAmount()).isEqualByComparingTo("50.00");
    }

    // ========== 分阶段收费新增测试用例 ==========

    @Test
    @DisplayName("测试挂号收费：成功场景")
    void createRegistrationCharge_Success() {
        // Given
        Long registrationId = 1L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode()); // 待就诊状态
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        registration.setPatient(patient);

        // Mock：没有已支付的挂号费
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.save(any())).thenAnswer(inv -> {
            com.his.entity.Charge c = inv.getArgument(0);
            c.setMainId(1000L);
            return c;
        });

        // When
        ChargeVO result = chargeService.createRegistrationCharge(registrationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDetails()).hasSize(1);
        assertThat(result.getDetails().get(0).getItemType()).isEqualTo("REGISTRATION");

        verify(chargeRepository).save(argThat(c -> c.getChargeType() == 1)); // 挂号费类型
    }

    @Test
    @DisplayName("测试挂号收费：重复收费失败")
    void createRegistrationCharge_DuplicatePayment() {
        // Given
        Long registrationId = 1L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        registration.setPatient(patient);

        // Mock：已支付挂号费
        when(chargeRepository.isRegistrationFeePaidOptimized(registrationId)).thenReturn(true);
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When & Then
        assertThatThrownBy(() -> chargeService.createRegistrationCharge(registrationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("挂号费已支付");
    }

    @Test
    @DisplayName("测试处方收费：成功场景")
    void createPrescriptionCharge_Success() {
        // Given
        Long registrationId = 1L;
        Long prescriptionId = 10L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.COMPLETED.getCode()); // 已就诊
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        patient.setName("张三");
        registration.setPatient(patient);

        Prescription prescription = new Prescription();
        prescription.setMainId(prescriptionId);
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
        prescription.setTotalAmount(new BigDecimal("90.00"));
        prescription.setIsDeleted((short) 0);

        // Mock：挂号费已支付，处方费未支付
        when(chargeRepository.isRegistrationFeePaidOptimized(registrationId)).thenReturn(true);
        when(chargeRepository.isPrescriptionFeePaidOptimized(prescriptionId)).thenReturn(false);
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(prescriptionRepository.findAllById(Arrays.asList(prescriptionId))).thenReturn(Arrays.asList(prescription));
        when(chargeRepository.save(any())).thenAnswer(inv -> {
            com.his.entity.Charge c = inv.getArgument(0);
            c.setMainId(1000L);
            return c;
        });

        // When
        ChargeVO result = chargeService.createPrescriptionCharge(registrationId, Arrays.asList(prescriptionId));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("90.00"); // 挂号费已支付，仅收处方费
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDetails()).hasSize(1);
        assertThat(result.getDetails().get(0).getItemType()).isEqualTo("PRESCRIPTION");

        verify(chargeRepository).save(argThat(c -> c.getChargeType() == 2)); // 处方费类型
    }

    @Test
    @DisplayName("测试处方收费：未就诊失败")
    void createPrescriptionCharge_NotCompleted() {
        // Given
        Long registrationId = 1L;
        Long prescriptionId = 10L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode()); // 未就诊
        registration.setIsDeleted((short) 0);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When & Then
        assertThatThrownBy(() -> chargeService.createPrescriptionCharge(registrationId, Arrays.asList(prescriptionId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已就诊");
    }

    @Test
    @DisplayName("测试处方收费：重复收费失败")
    void createPrescriptionCharge_DuplicatePayment() {
        // Given
        Long registrationId = 1L;
        Long prescriptionId = 10L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.COMPLETED.getCode());
        registration.setIsDeleted((short) 0);

        Patient patient = new Patient();
        patient.setMainId(100L);
        registration.setPatient(patient);

        Prescription prescription = new Prescription();
        prescription.setMainId(prescriptionId);
        prescription.setPrescriptionNo("PRE001");
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());

        // Mock：处方费已支付
        when(chargeRepository.isPrescriptionFeePaidOptimized(prescriptionId)).thenReturn(true);
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(prescriptionRepository.findAllById(Arrays.asList(prescriptionId))).thenReturn(Arrays.asList(prescription));

        // When & Then
        assertThatThrownBy(() -> chargeService.createPrescriptionCharge(registrationId, Arrays.asList(prescriptionId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("费用已支付");
    }

    @Test
    @DisplayName("测试支付挂号费：更新挂号状态")
    void processPayment_UpdateRegistrationStatus() {
        // Given
        Long chargeId = 1000L;
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
        registration.setMainId(1L);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        charge.setRegistration(registration);

        com.his.entity.ChargeDetail detail = new com.his.entity.ChargeDetail();
        detail.setItemType("REGISTRATION");
        detail.setItemId(1L);
        charge.setDetails(Arrays.asList(detail));

        com.his.dto.PaymentDTO paymentDTO = new com.his.dto.PaymentDTO();
        paymentDTO.setPaymentMethod(com.his.enums.PaymentMethodEnum.WECHAT.getCode());
        paymentDTO.setTransactionNo(transactionNo);
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        when(chargeRepository.findByIdWithDetails(chargeId)).thenReturn(Optional.of(charge));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChargeVO result = chargeService.processPayment(chargeId, paymentDTO);

        // Then
        assertThat(result.getStatus()).isEqualTo(com.his.enums.ChargeStatusEnum.PAID.getCode());
        // 验证状态机被调用，而不是直接调用repository.save
        try {
            verify(registrationStateMachine).transition(
                    eq(1L),
                    eq(RegStatusEnum.WAITING),
                    eq(RegStatusEnum.PAID_REGISTRATION),
                    any(), // 可以是 null
                    anyString(),
                    anyString()
            );
        } catch (Exception e) {
            // Should not happen in test mock
        }
    }

    @Test
    @DisplayName("测试检查挂号费支付状态：已支付")
    void isRegistrationFeePaid_Paid() {
        // Given
        Long registrationId = 1L;

        when(chargeRepository.isRegistrationFeePaidOptimized(registrationId)).thenReturn(true);

        // When
        boolean result = chargeService.isRegistrationFeePaid(registrationId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("测试检查挂号费支付状态：未支付")
    void isRegistrationFeePaid_NotPaid() {
        // Given
        Long registrationId = 1L;

        when(chargeRepository.isRegistrationFeePaidOptimized(registrationId)).thenReturn(false);

        // When
        boolean result = chargeService.isRegistrationFeePaid(registrationId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("测试按类型获取收费记录")
    void getChargesByType_Success() {
        // Given
        Long registrationId = 1L;

        Patient patient1 = new Patient();
        patient1.setName("张三");
        com.his.entity.Charge regCharge = new com.his.entity.Charge();
        regCharge.setMainId(100L);
        regCharge.setChargeType((short) 1);
        regCharge.setPatient(patient1);

        Patient patient2 = new Patient();
        patient2.setName("张三");
        com.his.entity.Charge prescCharge = new com.his.entity.Charge();
        prescCharge.setMainId(101L);
        prescCharge.setChargeType((short) 2);
        prescCharge.setPatient(patient2);

        when(chargeRepository.findByRegistrationIdWithDetailsOrderByCreatedAtDesc(registrationId, (short) 0))
                .thenReturn(Arrays.asList(regCharge, prescCharge));

        // When
        java.util.Map<String, java.util.List<ChargeVO>> result = chargeService.getChargesByType(registrationId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get("registration")).hasSize(1);
        assertThat(result.get("prescription")).hasSize(1);
        assertThat(result.get("combined")).isEmpty();
    }
}
