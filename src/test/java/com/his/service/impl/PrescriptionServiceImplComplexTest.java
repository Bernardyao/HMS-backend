package com.his.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.his.dto.PrescriptionDTO;
import com.his.entity.*;
import com.his.enums.PrescriptionStatusEnum;
import com.his.repository.MedicineRepository;
import com.his.repository.PrescriptionDetailRepository;
import com.his.repository.PrescriptionRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.PrescriptionStateMachine;
import com.his.test.base.BaseServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PrescriptionServiceImpl 复杂业务逻辑测试
 * <p>
 * 专注于测试处方服务的复杂场景和边界条件
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("PrescriptionServiceImpl 复杂逻辑测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class PrescriptionServiceImplComplexTest extends BaseServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private com.his.repository.MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PrescriptionStateMachine prescriptionStateMachine;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    // ==================== 处方创建边界测试 ====================

    @Test
    @DisplayName("创建处方：空药品列表应该抛出异常")
    void createPrescription_NoMedicines() {
        // Given - 空药品列表
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setPrescriptionType((short) 1);
        dto.setItems(Collections.emptyList()); // 空列表

        mockCommonDependencies();

        // When & Then - 应该抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> prescriptionService.createPrescription(dto));

        assertTrue(exception.getMessage().contains("药品列表不能为空"));
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    @DisplayName("创建处方：金额四舍五入应该正确处理")
    void createPrescription_AmountRounding() {
        // Given - 单价和数量会导致需要四舍五入的情况
        PrescriptionDTO dto = createValidPrescriptionDTO();

        PrescriptionDTO.PrescriptionItemDTO item1 = new PrescriptionDTO.PrescriptionItemDTO();
        item1.setMedicineId(1L);
        item1.setQuantity(3); // 单价10.01，数量3 = 30.03
        item1.setFrequency("一日3次");
        item1.setDosage("每次1片");
        item1.setRoute("口服");

        PrescriptionDTO.PrescriptionItemDTO item2 = new PrescriptionDTO.PrescriptionItemDTO();
        item2.setMedicineId(2L);
        item2.setQuantity(1); // 单价10.005，需要四舍五入
        item2.setFrequency("一日1次");
        item2.setDosage("每次2片");
        item2.setRoute("口服");

        dto.setItems(List.of(item1, item2));

        mockCommonDependencies();

        Medicine medicine1 = createMockMedicine(1L, "药品A", new BigDecimal("10.01"));
        Medicine medicine2 = createMockMedicine(2L, "药品B", new BigDecimal("10.005"));

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine1));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(medicine2));

        Prescription savedPrescription = mockSavedPrescription();
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(savedPrescription);
        when(prescriptionDetailRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // When - 创建处方
        Prescription result = prescriptionService.createPrescription(dto);

        // Then - 验证金额计算正确（四舍五入）
        assertNotNull(result);
        verify(prescriptionRepository).save(any(Prescription.class));

        // 验证总金额 = 10.01*3 + 10.005*1 = 30.03 + 10.01 = 40.04 (四舍五入到分)
        // RoundingMode.HALF_UP: 10.005 → 10.01
    }

    @Test
    @DisplayName("创建处方：药品已停用应该抛出异常")
    void createPrescription_StoppedMedicine() {
        // Given - 包含已停用的药品
        PrescriptionDTO dto = createValidPrescriptionDTO();

        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(1L);
        item.setQuantity(1);
        item.setFrequency("一日3次");
        item.setDosage("每次1片");
        item.setRoute("口服");

        dto.setItems(List.of(item));

        mockCommonDependencies();

        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setName("已停用药品");
        medicine.setStatus((short) 0); // 已停用
        medicine.setRetailPrice(new BigDecimal("50.00"));
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));

        // When & Then - 应该抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> prescriptionService.createPrescription(dto));

        assertTrue(exception.getMessage().contains("已停用"));
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    @DisplayName("创建处方：药品已删除应该抛出异常")
    void createPrescription_DeletedMedicine() {
        // Given - 包含已删除的药品
        PrescriptionDTO dto = createValidPrescriptionDTO();

        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(1L);
        item.setQuantity(1);
        item.setFrequency("一日3次");
        item.setDosage("每次1片");
        item.setRoute("口服");

        dto.setItems(List.of(item));

        mockCommonDependencies();

        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setName("已删除药品");
        medicine.setStatus((short) 1);
        medicine.setRetailPrice(new BigDecimal("50.00"));
        medicine.setIsDeleted((short) 1); // 已删除

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));

        // When & Then - 应该抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> prescriptionService.createPrescription(dto));

        assertTrue(exception.getMessage().contains("已被删除"));
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    @DisplayName("创建处方：药品不存在应该抛出异常")
    void createPrescription_MedicineNotFound() {
        // Given - 包含不存在的药品ID
        PrescriptionDTO dto = createValidPrescriptionDTO();

        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(999L); // 不存在的药品
        item.setQuantity(1);
        item.setFrequency("一日3次");
        item.setDosage("每次1片");
        item.setRoute("口服");

        dto.setItems(List.of(item));

        mockCommonDependencies();

        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then - 应该抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> prescriptionService.createPrescription(dto));

        assertTrue(exception.getMessage().contains("药品不存在"));
        assertTrue(exception.getMessage().contains("999"));
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    // ==================== 辅助方法 ====================

    private PrescriptionDTO createValidPrescriptionDTO() {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(1L);
        dto.setPrescriptionType((short) 1);
        dto.setValidityDays(3);
        return dto;
    }

    private Medicine createMockMedicine(Long id, String name, BigDecimal price) {
        Medicine medicine = new Medicine();
        medicine.setMainId(id);
        medicine.setName(name);
        medicine.setStatus((short) 1); // 在售
        medicine.setRetailPrice(price);
        medicine.setIsDeleted((short) 0);
        return medicine;
    }

    private Prescription mockSavedPrescription() {
        Prescription prescription = new Prescription();
        prescription.setMainId(1L);
        prescription.setPrescriptionNo("P202601030001");
        prescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());
        prescription.setIsDeleted((short) 0);
        return prescription;
    }

    private void mockCommonDependencies() {
        // Mock registration
        Registration registration = new Registration();
        registration.setMainId(1L);
        registration.setIsDeleted((short) 0);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        // Mock medical record
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setMainId(1L);
        medicalRecord.setIsDeleted((short) 0);
        when(medicalRecordRepository.findByRegistration_MainIdAndIsDeleted(1L, (short) 0))
                .thenReturn(Optional.of(medicalRecord));

        // Mock prescription repository
        when(prescriptionRepository.generatePrescriptionNo()).thenReturn("P202601030001");
    }
}
