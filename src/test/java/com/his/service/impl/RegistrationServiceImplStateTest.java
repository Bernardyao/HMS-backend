package com.his.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.his.dto.PaymentDTO;
import com.his.dto.RegistrationDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeRepository;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.ChargeService;
import com.his.test.base.BaseServiceTest;
import com.his.vo.ChargeVO;
import com.his.vo.RegistrationVO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * RegistrationServiceImpl 状态转换和边界条件测试
 * <p>
 * 专注于测试挂号服务的状态转换、异常场景和边界条件
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("RegistrationServiceImpl 状态转换测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceImplStateTest extends BaseServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private ChargeService chargeService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    // ==================== 退费场景测试 ====================

    @Test
    @DisplayName("取消已缴费挂号：收费单丢失应该抛出异常")
    void cancel_PaidRegistration_ChargeNotFound() {
        // Given - 已缴费的挂号，但找不到收费单
        Long registrationId = 123L;
        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode()); // 已缴费

        when(registrationRepository.findById(registrationId))
                .thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(Collections.emptyList()); // 空列表，找不到收费单

        // When & Then - 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(registrationId, "测试退费"));

        assertTrue(exception.getMessage().contains("未找到已支付的挂号收费单"));
        verify(chargeService, never()).processRefund(anyLong(), anyString());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("取消已缴费挂号：退费异常应该传播")
    void cancel_PaidRegistration_RefundThrowsException() {
        // Given - 已缴费的挂号，退费时抛出异常
        Long registrationId = 123L;
        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());

        Charge charge = new Charge();
        charge.setMainId(456L);
        charge.setChargeType((short) 1); // 挂号费
        charge.setStatus(ChargeStatusEnum.PAID.getCode());

        when(registrationRepository.findById(registrationId))
                .thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(List.of(charge));

        // 模拟退费服务抛出异常
        doThrow(new RuntimeException("支付网关超时"))
                .when(chargeService).processRefund(456L, "测试退费");

        // When & Then - 退费异常应该被包装成IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(registrationId, "测试退费"));

        assertTrue(exception.getMessage().contains("自动退费失败"));
        assertTrue(exception.getMessage().contains("支付网关超时"));
        verify(chargeService).processRefund(456L, "测试退费");
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    // ==================== 支付方式参数化测试 ====================

    @ParameterizedTest
    @DisplayName("挂号即收费：所有支持的支付方式都应该成功")
    @ValueSource(ints = {1, 2, 3, 4}) // 1=现金, 2=银行卡, 3=微信, 4=支付宝
    void register_WithPayment_AllMethods(int paymentMethod) {
        // Given - 完整的挂号DTO，包含支付信息
        RegistrationDTO dto = createValidRegistrationDTO();
        dto.setPaymentMethod((short) paymentMethod);
        dto.setTransactionNo("TXN_" + System.currentTimeMillis());

        Patient patient = createMockPatient();
        Department department = createMockDepartment();
        Doctor doctor = createMockDoctor();
        Registration savedRegistration = createMockRegistration();

        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(department));
        when(doctorRepository.findById(anyLong()))
                .thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort()))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong()))
                .thenReturn(0L);

        // Mock charge service
        ChargeVO chargeVO = new ChargeVO();
        chargeVO.setId(999L);
        when(chargeService.createRegistrationCharge(anyLong()))
                .thenReturn(chargeVO);
        when(chargeService.isRegistrationFeePaid(anyLong()))
                .thenReturn(false);
        when(chargeRepository.findByTransactionNo(anyString()))
                .thenReturn(Optional.empty());

        // When - 执行挂号即收费
        RegistrationVO result = registrationService.register(dto);

        // Then - 验证挂号成功
        assertNotNull(result);
        verify(chargeService).createRegistrationCharge(savedRegistration.getMainId());
        verify(chargeService).processPayment(eq(999L), any(PaymentDTO.class));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("挂号：排队号溢出应该正确处理")
    void register_QueueNoOverflow() {
        // Given - 模拟排队号溢出（已有999个挂号）
        RegistrationDTO dto = createValidRegistrationDTO();
        dto.setPaymentMethod(null); // 不支付

        Patient patient = createMockPatient();
        Department department = createMockDepartment();
        Doctor doctor = createMockDoctor();
        Registration savedRegistration = createMockRegistration();

        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(department));
        when(doctorRepository.findById(anyLong()))
                .thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort()))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);

        // 模拟已有999个挂号，下一个是1000
        when(registrationRepository.countByDateAndDepartment(any(), anyLong()))
                .thenReturn(999L);

        // When - 执行挂号
        RegistrationVO result = registrationService.register(dto);

        // Then - 排队号应该正确格式化为1000（虽然超出3位，但不应该抛异常）
        assertNotNull(result);
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("挂号：电话号码为null应该正常处理（可选字段）")
    void register_NullPhone() {
        // Given - 电话号码为null
        RegistrationDTO dto = createValidRegistrationDTO();
        dto.setPhone(null); // 可选字段
        dto.setPaymentMethod(null);

        Department department = createMockDepartment();
        Doctor doctor = createMockDoctor();
        Registration savedRegistration = createMockRegistration();

        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.empty()); // 新患者
        when(patientRepository.save(any(Patient.class)))
                .thenAnswer(invocation -> {
                    Patient p = invocation.getArgument(0);
                    p.setMainId(1L);
                    return p;
                });
        when(patientRepository.generatePatientNo())
                .thenReturn("P202601030001");
        when(departmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(department));
        when(doctorRepository.findById(anyLong()))
                .thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort()))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong()))
                .thenReturn(0L);
        when(registrationRepository.generateRegNo())
                .thenReturn("R202601030001");

        // When - 执行挂号
        RegistrationVO result = registrationService.register(dto);

        // Then - 应该成功，电话可以为null
        assertNotNull(result);
        verify(patientRepository).save(any(Patient.class));
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("挂号：最大年龄边界值应该正常处理")
    void register_MaxAge() {
        // Given - 最大年龄边界值（150岁）
        RegistrationDTO dto = createValidRegistrationDTO();
        dto.setAge((short) 150); // 人类已知最大年龄
        dto.setPaymentMethod(null);

        Patient patient = createMockPatient();
        Department department = createMockDepartment();
        Doctor doctor = createMockDoctor();
        Registration savedRegistration = createMockRegistration();

        when(patientRepository.findByIdCardAndIsDeleted(anyString(), anyShort()))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(department));
        when(doctorRepository.findById(anyLong()))
                .thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort()))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong()))
                .thenReturn(0L);

        // When - 执行挂号
        RegistrationVO result = registrationService.register(dto);

        // Then - 应该成功，系统不限制年龄上限
        assertNotNull(result);
        verify(registrationRepository).save(any(Registration.class));
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
        department.setName("内科");
        department.setIsDeleted((short) 0);
        return department;
    }

    private Doctor createMockDoctor() {
        Doctor doctor = new Doctor();
        doctor.setMainId(1L);
        doctor.setName("张医生");
        doctor.setIsDeleted((short) 0);
        return doctor;
    }

    private Registration createMockRegistration() {
        Registration registration = new Registration();
        registration.setMainId(123L);
        registration.setRegNo("R202601030001");
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        registration.setQueueNo("001");
        registration.setRegistrationFee(new BigDecimal("50.00"));
        registration.setIsDeleted((short) 0);
        return registration;
    }
}
