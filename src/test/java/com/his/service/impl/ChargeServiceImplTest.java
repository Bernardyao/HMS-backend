package com.his.service.impl;

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
}
