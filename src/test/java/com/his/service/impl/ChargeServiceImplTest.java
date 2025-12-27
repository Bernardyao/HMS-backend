package com.his.service.impl;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
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
import com.his.vo.ChargeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargeServiceImplTest {

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
        when(chargeRepository.save(any())).thenAnswer(inv -> {
            com.his.entity.Charge c = inv.getArgument(0);
            c.setMainId(1000L);
            return c;
        });

        // When
        ChargeVO result = chargeService.createCharge(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDetails()).hasSize(2);
        
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

        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));
        
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

        when(chargeRepository.findByTransactionNo(transactionNo)).thenReturn(Optional.of(charge));

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

        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));
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

        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));
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
}