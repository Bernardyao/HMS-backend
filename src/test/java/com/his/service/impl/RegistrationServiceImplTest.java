package com.his.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.his.dto.RegistrationDTO;
import com.his.entity.Charge;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeRepository;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.ChargeService;
import com.his.test.base.BaseServiceTest;
import com.his.testutils.TestDataBuilders;
import com.his.vo.ChargeVO;
import com.his.vo.RegistrationVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RegistrationServiceImpl 单元测试
 * 测试挂号服务的核心功能，包括挂号、取消挂号、挂号即收费等功能
 */
@DisplayName("挂号服务测试")
class RegistrationServiceImplTest extends BaseServiceTest {

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

    // ========================================
    // 挂号接口测试用例
    // ========================================

    @Test
    @DisplayName("测试挂号：成功场景（老患者，无支付）")
    void register_Success_ExistingPatient_NoPayment() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setPaymentMethod(null); // 不提供支付信息

        Patient existingPatient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard(dto.getIdCard())
                .build();
        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();
        Doctor doctor = TestDataBuilders.DoctorBuilder.builder()
                .mainId(1L)
                .name("李医生")
                .build();

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.of(existingPatient));
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.of(department));
        when(doctorRepository.findById(dto.getDoctorId())).thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(false);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong())).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setMainId(1000L);
            return r;
        });

        // When
        RegistrationVO result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDeptName()).isEqualTo("内科");
        assertThat(result.getDoctorName()).isEqualTo("李医生");
        assertThat(result.getStatus()).isEqualTo(RegStatusEnum.WAITING.getCode());

        verify(registrationRepository).save(any());
        verify(chargeService, never()).createRegistrationCharge(anyLong());
        verify(chargeService, never()).processPayment(anyLong(), any());
    }

    @Test
    @DisplayName("测试挂号：成功场景（新患者建档）")
    void register_Success_NewPatient() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setPaymentMethod(null);

        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();
        Doctor doctor = TestDataBuilders.DoctorBuilder.builder()
                .mainId(1L)
                .name("李医生")
                .build();

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.empty()); // 新患者
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.of(department));
        when(doctorRepository.findById(dto.getDoctorId())).thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(false);
        when(patientRepository.save(any())).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setMainId(100L);
            return p;
        });
        when(registrationRepository.countByDateAndDepartment(any(), anyLong())).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setMainId(1000L);
            return r;
        });

        // When
        RegistrationVO result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientName()).isEqualTo(dto.getPatientName());
        assertThat(result.getPatientId()).isEqualTo(100L);

        verify(patientRepository).save(any());
        verify(registrationRepository).save(any());
    }

    @Test
    @DisplayName("测试挂号：成功场景（挂号即收费）")
    void register_Success_WithPayment() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setPaymentMethod((short) 3); // 微信支付
        dto.setTransactionNo("WX2025123110012001");

        Patient patient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard(dto.getIdCard())
                .build();
        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();
        Doctor doctor = TestDataBuilders.DoctorBuilder.builder()
                .mainId(1L)
                .name("李医生")
                .build();

        ChargeVO mockChargeVO = new ChargeVO();
        mockChargeVO.setId(500L);
        mockChargeVO.setTotalAmount(new BigDecimal("20.00"));

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.of(department));
        when(doctorRepository.findById(dto.getDoctorId())).thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(false);
        when(registrationRepository.countByDateAndDepartment(any(), anyLong())).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setMainId(1000L);
            return r;
        });

        // Mock charge service
        when(chargeService.createRegistrationCharge(1000L)).thenReturn(mockChargeVO);
        when(chargeService.processPayment(eq(500L), any())).thenReturn(mockChargeVO);

        // When
        RegistrationVO result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientName()).isEqualTo("张三");

        verify(chargeService).createRegistrationCharge(1000L);
        verify(chargeService).processPayment(eq(500L), any());
    }

    @Test
    @DisplayName("测试挂号：重复挂号失败")
    void register_DuplicateRegistration_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();

        Patient patient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard(dto.getIdCard())
                .build();
        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();
        Doctor doctor = TestDataBuilders.DoctorBuilder.builder()
                .mainId(1L)
                .name("李医生")
                .build();

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.of(department));
        when(doctorRepository.findById(dto.getDoctorId())).thenReturn(Optional.of(doctor));
        when(registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                anyLong(), anyLong(), any(), anyShort(), anyShort())).thenReturn(true); // 已存在挂号

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("今日已挂号");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试挂号：科室不存在失败")
    void register_DepartmentNotFound_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();

        Patient patient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard(dto.getIdCard())
                .build();

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("科室不存在");
    }

    @Test
    @DisplayName("测试挂号：医生不存在失败")
    void register_DoctorNotFound_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();

        Patient patient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard(dto.getIdCard())
                .build();
        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();

        when(patientRepository.findByIdCardAndIsDeleted(dto.getIdCard(), (short) 0))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(dto.getDeptId())).thenReturn(Optional.of(department));
        when(doctorRepository.findById(dto.getDoctorId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("医生不存在");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（患者姓名为空）")
    void register_Validation_EmptyPatientName_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setPatientName("");

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("患者姓名不能为空");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（身份证号为空）")
    void register_Validation_EmptyIdCard_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setIdCard("");

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("身份证号不能为空");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（性别为空）")
    void register_Validation_NullGender_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setGender(null);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("性别不能为空");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（科室ID为空）")
    void register_Validation_NullDeptId_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setDeptId(null);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("科室ID不能为空");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（医生ID为空）")
    void register_Validation_NullDoctorId_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setDoctorId(null);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("医生ID不能为空");
    }

    @Test
    @DisplayName("测试挂号：参数校验失败（挂号费为空）")
    void register_Validation_NullRegFee_Fails() {
        // Given
        RegistrationDTO dto = createBasicRegistrationDTO();
        dto.setRegFee(null);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("挂号费不能为空");
    }

    @Test
    @DisplayName("测试挂号：查询成功")
    void getById_Success() {
        // Given
        Long registrationId = 1000L;

        Patient patient = TestDataBuilders.PatientBuilder.builder()
                .mainId(100L)
                .name("张三")
                .idCard("110101199001011001")
                .build();
        Department department = TestDataBuilders.DepartmentBuilder.builder()
                .mainId(1L)
                .name("内科")
                .build();
        Doctor doctor = TestDataBuilders.DoctorBuilder.builder()
                .mainId(1L)
                .name("李医生")
                .build();

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setRegNo("R20251231001");
        registration.setPatient(patient);
        registration.setDepartment(department);
        registration.setDoctor(doctor);
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        registration.setVisitDate(LocalDate.now());
        registration.setRegistrationFee(new BigDecimal("20.00"));

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When
        RegistrationVO result = registrationService.getById(registrationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(registrationId);
        assertThat(result.getPatientName()).isEqualTo("张三");
        assertThat(result.getDeptName()).isEqualTo("内科");
        assertThat(result.getDoctorName()).isEqualTo("李医生");
    }

    @Test
    @DisplayName("测试挂号查询：记录不存在失败")
    void getById_NotFound_Fails() {
        // Given
        Long registrationId = 9999L;
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.getById(registrationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("挂号记录不存在");
    }

    // ========================================
    // 退号接口测试用例
    // ========================================

    @Test
    @DisplayName("测试取消挂号：成功场景（未支付挂号费）")
    void cancel_Success_Unpaid() {
        // Given
        Long registrationId = 1000L;
        String reason = "患者临时有事";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode()); // 待就诊状态

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        registrationService.cancel(registrationId, reason);

        // Then
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.CANCELLED.getCode());
        assertThat(registration.getCancelReason()).isEqualTo(reason);

        verify(registrationRepository).save(registration);
        verify(chargeService, never()).processRefund(anyLong(), anyString());
    }

    @Test
    @DisplayName("测试取消挂号：成功场景（已支付，自动退费）")
    void cancel_Success_PaidWithAutoRefund() {
        // Given
        Long registrationId = 1000L;
        Long chargeId = 500L;
        String reason = "患者临时有事";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode()); // 已缴挂号费状态

        // Mock the registration fee charge
        Charge registrationCharge = new Charge();
        registrationCharge.setMainId(chargeId);
        registrationCharge.setChargeType((short) 1); // 挂号费类型
        registrationCharge.setStatus(ChargeStatusEnum.PAID.getCode()); // 已支付状态

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(java.util.List.of(registrationCharge));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        registrationService.cancel(registrationId, reason);

        // Then
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.CANCELLED.getCode());
        assertThat(registration.getCancelReason()).isEqualTo(reason);

        // Verify that processRefund was called with the charge ID, not registration ID
        verify(chargeService).processRefund(chargeId, reason);
        verify(registrationRepository).save(registration);
    }

    @Test
    @DisplayName("测试取消挂号：状态错误失败（已就诊）")
    void cancel_InvalidStatus_Completed_Fails() {
        // Given
        Long registrationId = 1000L;
        String reason = "不再需要";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.COMPLETED.getCode()); // 已就诊

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When & Then
        assertThatThrownBy(() -> registrationService.cancel(registrationId, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有待就诊或已缴挂号费状态的挂号才能取消");

        verify(registrationRepository, never()).save(any());
        verify(chargeService, never()).processRefund(anyLong(), anyString());
    }

    @Test
    @DisplayName("测试取消挂号：状态错误失败（已取消）")
    void cancel_InvalidStatus_Cancelled_Fails() {
        // Given
        Long registrationId = 1000L;
        String reason = "取消原因";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.CANCELLED.getCode()); // 已取消

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When & Then
        assertThatThrownBy(() -> registrationService.cancel(registrationId, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有待就诊或已缴挂号费状态的挂号才能取消");
    }

    @Test
    @DisplayName("测试取消挂号：记录不存在失败")
    void cancel_NotFound_Fails() {
        // Given
        Long registrationId = 9999L;
        String reason = "取消原因";

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.cancel(registrationId, reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("挂号记录不存在");
    }

    @Test
    @DisplayName("测试取消挂号：自动退费失败（回滚事务）")
    void cancel_AutoRefundFailure_ThrowsException() {
        // Given
        Long registrationId = 1000L;
        Long chargeId = 500L;
        String reason = "患者临时有事";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());

        // Mock the registration fee charge
        Charge registrationCharge = new Charge();
        registrationCharge.setMainId(chargeId);
        registrationCharge.setChargeType((short) 1); // 挂号费类型
        registrationCharge.setStatus(ChargeStatusEnum.PAID.getCode()); // 已支付状态

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(java.util.List.of(registrationCharge));
        doThrow(new RuntimeException("退费系统异常"))
                .when(chargeService).processRefund(chargeId, reason);

        // When & Then
        assertThatThrownBy(() -> registrationService.cancel(registrationId, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("自动退费失败");

        verify(chargeService).processRefund(chargeId, reason);
        verify(registrationRepository, never()).save(any()); // 退费失败，不应保存状态
    }

    @Test
    @DisplayName("测试取消挂号：收费单不存在失败")
    void cancel_ChargeNotFound_Fails() {
        // Given
        Long registrationId = 1000L;
        String reason = "患者临时有事";

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0))
                .thenReturn(java.util.Collections.emptyList()); // No charge found

        // When & Then
        assertThatThrownBy(() -> registrationService.cancel(registrationId, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未找到已支付的挂号收费单");

        verify(chargeService, never()).processRefund(anyLong(), anyString());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试退费：成功场景")
    void refund_Success() {
        // Given
        Long registrationId = 1000L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.CANCELLED.getCode());

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        registrationService.refund(registrationId);

        // Then
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.REFUNDED.getCode());
        verify(registrationRepository).save(registration);
    }

    @Test
    @DisplayName("测试退费：状态错误失败（未取消）")
    void refund_InvalidStatus_NotCancelled_Fails() {
        // Given
        Long registrationId = 1000L;

        Registration registration = new Registration();
        registration.setMainId(registrationId);
        registration.setStatus(RegStatusEnum.WAITING.getCode());

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        // When & Then
        assertThatThrownBy(() -> registrationService.refund(registrationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有已取消状态的挂号才能退费");
    }

    @Test
    @DisplayName("测试退费：记录不存在失败")
    void refund_NotFound_Fails() {
        // Given
        Long registrationId = 9999L;
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.refund(registrationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("挂号记录不存在");
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 创建基础挂号DTO
     */
    private RegistrationDTO createBasicRegistrationDTO() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("张三");
        dto.setIdCard("110101199001011001");
        dto.setGender((short) 1);
        dto.setAge((short) 34);
        dto.setPhone("13912345678");
        dto.setDeptId(1L);
        dto.setDoctorId(1L);
        dto.setRegFee(new BigDecimal("20.00"));
        return dto;
    }
}
