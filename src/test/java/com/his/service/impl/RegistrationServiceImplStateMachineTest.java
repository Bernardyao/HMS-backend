package com.his.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.his.common.CommonConstants;
import com.his.entity.Charge;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.ChargeTypeEnum;
import com.his.enums.GenderEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeRepository;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.ChargeService;
import com.his.service.RegistrationStateMachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegistrationServiceImpl状态机修复测试
 *
 * <p>验证取消和退费方法正确使用状态机，确保状态转换的验证和审计日志</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("挂号服务-状态机修复测试")
class RegistrationServiceImplStateMachineTest {

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

    @Mock
    private RegistrationStateMachine registrationStateMachine;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private Patient patient;
    private Department department;
    private Doctor doctor;
    private Registration registration;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        patient = new Patient();
        patient.setMainId(1L);
        patient.setPatientNo("P2026010800001");
        patient.setName("张三");
        patient.setGender(GenderEnum.MALE.getCode());
        patient.setAge((short) 30);
        patient.setIdCard("110101199001010001");
        patient.setPhone("13800138000");
        patient.setIsDeleted(CommonConstants.NORMAL);

        department = new Department();
        department.setMainId(1L);
        department.setName("内科");
        department.setIsDeleted(CommonConstants.NORMAL);

        doctor = new Doctor();
        doctor.setMainId(1L);
        doctor.setName("李医生");
        doctor.setDepartment(department);
        doctor.setIsDeleted(CommonConstants.NORMAL);

        registration = new Registration();
        registration.setMainId(1L);
        registration.setRegNo("R2026010800001");
        registration.setPatient(patient);
        registration.setDepartment(department);
        registration.setDoctor(doctor);
        registration.setVisitDate(LocalDate.now());
        registration.setRegistrationFee(BigDecimal.valueOf(10.00));
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        registration.setIsDeleted(CommonConstants.NORMAL);
        registration.setQueueNo("001");
        registration.setAppointmentTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("取消挂号 - 使用状态机WAITING → CANCELLED")
    void testCancelRegistration_FromWaiting() throws Exception {
        // 【场景1】取消WAITING状态的挂号，不涉及退费

        // 准备数据：WAITING状态
        registration.setStatus(RegStatusEnum.WAITING.getCode());

        // Mock查询
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // Mock状态机
        Registration updatedRegistration = new Registration();
        updatedRegistration.setStatus(RegStatusEnum.CANCELLED.getCode());
        when(registrationStateMachine.transition(
                eq(1L),
                eq(RegStatusEnum.WAITING),
                eq(RegStatusEnum.CANCELLED),
                any(),
                any(),
                eq("取消挂号: 患者要求取消")
        )).thenReturn(updatedRegistration);

        // 执行取消
        registrationService.cancel(1L, "患者要求取消");

        // 验证状态机被调用
        verify(registrationStateMachine, times(1)).transition(
                1L,
                RegStatusEnum.WAITING,
                RegStatusEnum.CANCELLED,
                null,
                "SYSTEM",
                "取消挂号: 患者要求取消"
        );

        // 验证取消原因被设置
        assertEquals("患者要求取消", registration.getCancelReason());

        System.out.println("✅ 测试通过：取消WAITING状态挂号，使用状态机更新");
    }

    @Test
    @DisplayName("取消挂号 - 使用状态机PAID_REGISTRATION → CANCELLED（含退费）")
    void testCancelRegistration_FromPaidRegistration() throws Exception {
        // 【场景2】取消PAID_REGISTRATION状态的挂号，涉及自动退费

        // 准备数据：PAID_REGISTRATION状态
        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());

        // 准备退费收费单
        Charge charge = new Charge();
        charge.setMainId(1L);
        charge.setChargeNo("CHG2026010800001");
        charge.setChargeType(ChargeTypeEnum.REGISTRATION_ONLY.getCode());
        charge.setStatus(ChargeStatusEnum.PAID.getCode());
        List<Charge> charges = new ArrayList<>();
        charges.add(charge);

        // Mock查询
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));
        when(chargeRepository.findByRegistration_MainIdAndIsDeleted(eq(1L), eq(CommonConstants.NORMAL)))
                .thenReturn(charges);

        // Mock退费
        when(chargeService.processRefund(1L, "患者要求取消")).thenReturn(null);

        // Mock状态机
        Registration updatedRegistration = new Registration();
        updatedRegistration.setStatus(RegStatusEnum.CANCELLED.getCode());
        when(registrationStateMachine.transition(
                eq(1L),
                eq(RegStatusEnum.PAID_REGISTRATION),
                eq(RegStatusEnum.CANCELLED),
                any(),
                any(),
                eq("取消挂号: 患者要求取消")
        )).thenReturn(updatedRegistration);

        // 执行取消
        registrationService.cancel(1L, "患者要求取消");

        // 验证退费被调用
        verify(chargeService, times(1)).processRefund(1L, "患者要求取消");

        // 验证状态机被调用
        verify(registrationStateMachine, times(1)).transition(
                1L,
                RegStatusEnum.PAID_REGISTRATION,
                RegStatusEnum.CANCELLED,
                null,
                "SYSTEM",
                "取消挂号: 患者要求取消"
        );

        System.out.println("✅ 测试通过：取消PAID_REGISTRATION状态挂号，先退费再更新状态");
    }

    @Test
    @DisplayName("退费 - 使用状态机CANCELLED → REFUNDED")
    void testRefundRegistration() throws Exception {
        // 【场景3】对已取消的挂号执行退费

        // 准备数据：CANCELLED状态
        registration.setStatus(RegStatusEnum.CANCELLED.getCode());

        // Mock查询
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // Mock状态机
        Registration updatedRegistration = new Registration();
        updatedRegistration.setStatus(RegStatusEnum.REFUNDED.getCode());
        when(registrationStateMachine.transition(
                eq(1L),
                eq(RegStatusEnum.CANCELLED),
                eq(RegStatusEnum.REFUNDED),
                any(),
                any(),
                eq("退费")
        )).thenReturn(updatedRegistration);

        // 执行退费
        registrationService.refund(1L);

        // 验证状态机被调用
        verify(registrationStateMachine, times(1)).transition(
                1L,
                RegStatusEnum.CANCELLED,
                RegStatusEnum.REFUNDED,
                null,
                "SYSTEM",
                "退费"
        );

        System.out.println("✅ 测试通过：退费操作使用状态机更新状态");
    }

    @Test
    @DisplayName("取消挂号 - 状态转换失败应抛出异常")
    void testCancelRegistration_StateTransitionFails() throws Exception {
        // 【场景4】状态转换失败时应该抛出异常

        // 准备数据：WAITING状态
        registration.setStatus(RegStatusEnum.WAITING.getCode());

        // Mock查询
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // Mock状态机抛出异常
        when(registrationStateMachine.transition(
                anyLong(),
                any(RegStatusEnum.class),
                any(RegStatusEnum.class),
                any(),
                any(),
                anyString()
        )).thenThrow(new IllegalStateException("无效的状态转换"));

        // 执行取消并验证异常
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registrationService.cancel(1L, "测试")
        );

        assertTrue(exception.getMessage().contains("取消挂号失败"));
        assertTrue(exception.getMessage().contains("无效的状态转换"));

        System.out.println("✅ 测试通过：状态转换失败时正确抛出异常");
    }

    @Test
    @DisplayName("退费 - 状态不正确时抛出异常")
    void testRefundRegistration_InvalidState() throws Exception {
        // 【场景5】非CANCELLED状态不能退费

        // 准备数据：WAITING状态（非CANCELLED）
        registration.setStatus(RegStatusEnum.WAITING.getCode());

        // Mock查询
        when(registrationRepository.findById(1L))
                .thenReturn(Optional.of(registration));

        // 执行退费并验证异常
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registrationService.refund(1L)
        );

        assertTrue(exception.getMessage().contains("只有已取消状态的挂号才能退费"));

        System.out.println("✅ 测试通过：非CANCELLED状态不能退费");
    }
}
