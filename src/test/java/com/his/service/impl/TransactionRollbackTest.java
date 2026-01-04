package com.his.service.impl;

import com.his.dto.RegistrationDTO;
import com.his.entity.*;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.test.base.BaseServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 事务回滚测试
 * <p>
 * 验证在各种异常场景下，服务层正确处理异常并保证数据一致性
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("事务回滚测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionRollbackTest extends BaseServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private com.his.repository.ChargeRepository chargeRepository;

    @Mock
    private ChargeService chargeService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    // ==================== 挂号事务测试 ====================

    @Test
    @DisplayName("挂号：患者保存失败应该抛出异常")
    void register_PatientSaveFails_Rollback() {
        // Given - 患者保存会抛出异常
        RegistrationDTO dto = createValidRegistrationDTO();

        when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createMockDepartment()));
        when(doctorRepository.findById(anyLong())).thenReturn(Optional.of(createMockDoctor()));
        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.empty()); // 新患者
        when(patientRepository.save(any(Patient.class)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // When & Then - 应该抛出异常
        assertThrows(RuntimeException.class, () -> registrationService.register(dto));

        // Then - 验证挂号没有被保存（事务回滚）
        verify(registrationRepository, never()).save(any(Registration.class));
        verify(chargeService, never()).createRegistrationCharge(anyLong());
    }

    @Test
    @DisplayName("挂号：收费创建失败应该抛出异常")
    void register_ChargeServiceFails_Rollback() {
        // Given - 收费服务抛出异常
        RegistrationDTO dto = createValidRegistrationDTO();
        dto.setPaymentMethod((short) 1);
        dto.setTransactionNo("TXN001");

        Patient patient = createMockPatient();
        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createMockDepartment()));
        when(doctorRepository.findById(anyLong())).thenReturn(Optional.of(createMockDoctor()));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(false);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong())).thenReturn(0L);
        when(registrationRepository.generateRegNo()).thenReturn("R202601030001");

        Registration savedRegistration = createMockRegistration();
        when(registrationRepository.save(any(Registration.class))).thenReturn(savedRegistration);

        // Mock charge service to fail
        when(chargeService.createRegistrationCharge(anyLong()))
                .thenThrow(new RuntimeException("收费创建失败"));

        // When & Then - 应该抛出异常
        assertThrows(RuntimeException.class, () -> registrationService.register(dto));

        // 验证异常处理
        verify(chargeService).createRegistrationCharge(anyLong());
    }

    @Test
    @DisplayName("并发挂号：同一患者不应该重复创建")
    void concurrentRegistration_DataConsistency() {
        // Given - 患者已存在
        RegistrationDTO dto = createValidRegistrationDTO();

        Patient existingPatient = createMockPatient();
        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.of(existingPatient)); // 患者已存在
        when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createMockDepartment()));
        when(doctorRepository.findById(anyLong())).thenReturn(Optional.of(createMockDoctor()));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(false);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong())).thenReturn(0L);
        when(registrationRepository.generateRegNo()).thenReturn("R202601030001");

        Registration savedRegistration = createMockRegistration();
        when(registrationRepository.save(any(Registration.class))).thenReturn(savedRegistration);

        // When - 执行挂号
        registrationService.register(dto);

        // Then - 患者不应该重复创建
        verify(patientRepository, never()).save(any(Patient.class));
        verify(registrationRepository).save(any(Registration.class));
    }

    // ==================== 退费事务测试 ====================

    @Test
    @DisplayName("退费：收费单更新失败应该抛出异常")
    void refund_ChargeUpdateFails_Rollback() {
        // Given - 已缴费的挂号（状态必须是PAID_REGISTRATION = 4）
        Long registrationId = 123L;
        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus((short) 4); // PAID_REGISTRATION = 4

        Charge charge = new Charge();
        charge.setMainId(456L);
        charge.setChargeType((short) 1); // 挂号费
        charge.setStatus((short) 1); // PAID = 1 (ChargeStatusEnum.PAID.getCode())

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(List.of(charge));

        // Mock charge service to fail
        when(chargeService.processRefund(456L, "测试退费"))
                .thenThrow(new RuntimeException("退费处理失败"));

        // When & Then - 应该抛出IllegalStateException（包装的异常）
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(registrationId, "测试退费"));

        assertTrue(exception.getMessage().contains("自动退费失败"));

        // 验证退费被调用，但挂号状态不应该更新
        verify(chargeService).processRefund(456L, "测试退费");
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("退费：找不到收费单应该抛出异常")
    void refund_ChargeNotFound_ThrowsException() {
        // Given - 已缴费的挂号，但找不到符合条件的收费单
        Long registrationId = 123L;
        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus((short) 4); // PAID_REGISTRATION = 4

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(Collections.emptyList()); // 空列表，找不到收费单

        // When & Then - 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(registrationId, "测试退费"));

        assertTrue(exception.getMessage().contains("未找到已支付的挂号收费单"));

        // 验证退费没有被调用
        verify(chargeService, never()).processRefund(anyLong(), anyString());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    // ==================== 辅助方法 ====================

    private RegistrationDTO createValidRegistrationDTO() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("测试患者");
        dto.setIdCard("110101199001011234");
        dto.setGender((short) 1);
        dto.setAge((short) 30);
        dto.setPhone("13800138000");
        dto.setDeptId(1L);
        dto.setDoctorId(1L);
        dto.setRegFee(new BigDecimal("50.00"));
        return dto;
    }

    private Patient createMockPatient() {
        Patient patient = new Patient();
        patient.setMainId(1L);
        patient.setPatientNo("P202601030001");
        patient.setName("测试患者");
        patient.setIdCard("110101199001011234");
        patient.setGender((short) 1);
        patient.setAge((short) 30);
        patient.setPhone("13800138000");
        patient.setIsDeleted((short) 0);
        return patient;
    }

    private Department createMockDepartment() {
        Department department = new Department();
        department.setMainId(1L);
        department.setName("测试科室");
        department.setIsDeleted((short) 0);
        return department;
    }

    private Doctor createMockDoctor() {
        Doctor doctor = new Doctor();
        doctor.setMainId(1L);
        doctor.setName("测试医生");
        doctor.setIsDeleted((short) 0);
        return doctor;
    }

    private Registration createMockRegistration() {
        Registration registration = new Registration();
        registration.setMainId(123L);
        registration.setRegNo("R202601030001");
        registration.setStatus((short) 0); // 待就诊
        registration.setQueueNo("001");
        registration.setRegistrationFee(new BigDecimal("50.00"));
        registration.setIsDeleted((short) 0);
        return registration;
    }
}
