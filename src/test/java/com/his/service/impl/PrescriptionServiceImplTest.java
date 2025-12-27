package com.his.service.impl;

import com.his.entity.Medicine;
import com.his.entity.Prescription;
import com.his.entity.PrescriptionDetail;
import com.his.enums.PrescriptionStatusEnum;
import com.his.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    @DisplayName("发药失败：状态不是已缴费")
    void dispense_Fail_WhenStatusNotPaid() {
        Long prescriptionId = 1L;
        Prescription prescription = new Prescription();
        prescription.setMainId(prescriptionId);
        // 设置为 REVIEWED (2)，而不是 PAID (5)
        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> prescriptionService.dispense(prescriptionId, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有已缴费状态的处方才能发药");
    }

    @Test
    @DisplayName("恢复库存：成功")
    void restoreInventoryOnly_Success() {
        Long prescriptionId = 1L;
        Prescription prescription = new Prescription();
        prescription.setMainId(prescriptionId);
        
        Medicine medicine = new Medicine();
        medicine.setMainId(10L);
        medicine.setStockQuantity(100);
        
        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setMedicine(medicine);
        detail.setQuantity(10);
        
        prescription.setDetails(Collections.singletonList(detail));

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(10L)).thenReturn(Optional.of(medicine));

        // 调用新方法
        prescriptionService.restoreInventoryOnly(prescriptionId);

        // 验证库存增加了 10 (100 -> 110)
        verify(medicineRepository).save(argThat(m -> m.getStockQuantity() == 110));
    }
}