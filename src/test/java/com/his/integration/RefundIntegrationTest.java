package com.his.integration;

import com.his.entity.*;
import com.his.repository.*;
import com.his.service.impl.RegistrationServiceImpl;
import com.his.test.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 退费集成测试
 * <p>
 * 使用真实数据库和Mock服务验证退费场景
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("退费集成测试")
class RefundIntegrationTest extends BaseIntegrationTest {

    // 使用AtomicLong作为唯一计数器，避免System.currentTimeMillis()冲突
    private static final AtomicLong TEST_COUNTER = new AtomicLong(0);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private com.his.repository.ChargeRepository chargeRepository;

    @MockBean
    private com.his.service.ChargeService chargeService;

    // ==================== 退费场景测试 ====================

    @Test
    @DisplayName("退费：收费单不存在应该抛出异常")
    void refund_ChargeNotFound_ThrowsException() {
        // Given - 创建已缴费的挂号，但没有收费单
        String uniqueSuffix = String.valueOf(TEST_COUNTER.incrementAndGet());

        Patient patient = createTestPatient(uniqueSuffix);
        Patient savedPatient = patientRepository.save(patient);

        Department department = createTestDepartment(uniqueSuffix);
        Department savedDepartment = departmentRepository.save(department);

        Doctor doctor = createTestDoctor(uniqueSuffix);
        doctor.setDepartment(savedDepartment);
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 创建已缴费的挂号
        Registration registration = new Registration();
        registration.setPatient(savedPatient);
        registration.setDepartment(savedDepartment);
        registration.setDoctor(savedDoctor);
        registration.setRegNo("R" + uniqueSuffix);
        registration.setStatus((short) 4); // PAID_REGISTRATION = 4
        registration.setVisitDate(LocalDate.now());
        registration.setQueueNo("001");
        registration.setRegistrationFee(new BigDecimal("50.00"));
        registration.setIsDeleted((short) 0);

        Registration savedRegistration = registrationRepository.save(registration);

        // 创建RegistrationServiceImpl实例
        RegistrationServiceImpl registrationService = new RegistrationServiceImpl(
                patientRepository, registrationRepository, departmentRepository,
                doctorRepository, chargeRepository, chargeService);

        // When & Then - 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(savedRegistration.getMainId(), "测试退费"));

        assertTrue(exception.getMessage().contains("未找到已支付的挂号收费单"));

        // 验证退费没有被调用
        verify(chargeService, never()).processRefund(anyLong(), anyString());

        // 验证挂号状态没有改变
        Registration unchangedRegistration = registrationRepository.findById(savedRegistration.getMainId()).orElseThrow();
        assertEquals((short) 4, unchangedRegistration.getStatus());
    }

    @Test
    @DisplayName("退费：待就诊状态可以直接取消（无需退费）")
    void refund_WaitingStatus_NoRefundNeeded() {
        // Given - 创建待就诊状态的挂号
        String uniqueSuffix = String.valueOf(TEST_COUNTER.incrementAndGet());

        Patient patient = createTestPatient(uniqueSuffix);
        Patient savedPatient = patientRepository.save(patient);

        Department department = createTestDepartment(uniqueSuffix);
        Department savedDepartment = departmentRepository.save(department);

        Doctor doctor = createTestDoctor(uniqueSuffix);
        doctor.setDepartment(savedDepartment);
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 创建待就诊的挂号
        Registration registration = new Registration();
        registration.setPatient(savedPatient);
        registration.setDepartment(savedDepartment);
        registration.setDoctor(savedDoctor);
        registration.setRegNo("R" + uniqueSuffix);
        registration.setStatus((short) 0); // WAITING = 0
        registration.setVisitDate(LocalDate.now());
        registration.setQueueNo("001");
        registration.setRegistrationFee(new BigDecimal("50.00"));
        registration.setIsDeleted((short) 0);

        Registration savedRegistration = registrationRepository.save(registration);

        // 创建RegistrationServiceImpl实例
        RegistrationServiceImpl registrationService = new RegistrationServiceImpl(
                patientRepository, registrationRepository, departmentRepository,
                doctorRepository, chargeRepository, chargeService);

        // When - 取消待就诊的挂号
        registrationService.cancel(savedRegistration.getMainId(), "患者取消");

        // Then - 验证挂号状态变为已取消
        Registration cancelledRegistration = registrationRepository.findById(savedRegistration.getMainId()).orElseThrow();
        assertEquals((short) 2, cancelledRegistration.getStatus()); // CANCELLED = 2
        assertEquals("患者取消", cancelledRegistration.getCancelReason());

        // 验证退费没有被调用
        verify(chargeService, never()).processRefund(anyLong(), anyString());
    }

    @Test
    @DisplayName("退费：已就诊状态不能取消")
    void refund_CompletedStatus_CannotCancel() {
        // Given - 创建已就诊状态的挂号
        String uniqueSuffix = String.valueOf(TEST_COUNTER.incrementAndGet());

        Patient patient = createTestPatient(uniqueSuffix);
        Patient savedPatient = patientRepository.save(patient);

        Department department = createTestDepartment(uniqueSuffix);
        Department savedDepartment = departmentRepository.save(department);

        Doctor doctor = createTestDoctor(uniqueSuffix);
        doctor.setDepartment(savedDepartment);
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 创建已就诊的挂号
        Registration registration = new Registration();
        registration.setPatient(savedPatient);
        registration.setDepartment(savedDepartment);
        registration.setDoctor(savedDoctor);
        registration.setRegNo("R" + uniqueSuffix);
        registration.setStatus((short) 1); // COMPLETED = 1
        registration.setVisitDate(LocalDate.now());
        registration.setQueueNo("001");
        registration.setRegistrationFee(new BigDecimal("50.00"));
        registration.setIsDeleted((short) 0);

        Registration savedRegistration = registrationRepository.save(registration);

        // 创建RegistrationServiceImpl实例
        RegistrationServiceImpl registrationService = new RegistrationServiceImpl(
                patientRepository, registrationRepository, departmentRepository,
                doctorRepository, chargeRepository, chargeService);

        // When & Then - 应该抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registrationService.cancel(savedRegistration.getMainId(), "测试取消"));

        assertTrue(exception.getMessage().contains("只有待就诊或已缴挂号费状态的挂号才能取消"));

        // 验证退费没有被调用
        verify(chargeService, never()).processRefund(anyLong(), anyString());
    }

    // ==================== 辅助方法 ====================

    private Patient createTestPatient(String suffix) {
        Patient patient = new Patient();
        patient.setPatientNo("P" + suffix);
        patient.setName("测试患者");
        patient.setIdCard("110101199001011234");
        patient.setGender((short) 1);
        patient.setAge((short) 30);
        patient.setPhone("13800138000");
        patient.setIsDeleted((short) 0);
        return patient;
    }

    private Department createTestDepartment(String suffix) {
        Department department = new Department();
        department.setDeptCode("DEPT" + suffix);
        department.setName("测试科室");
        department.setIsDeleted((short) 0);
        return department;
    }

    private Doctor createTestDoctor(String suffix) {
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("D" + suffix);
        doctor.setName("测试医生");
        doctor.setGender((short) 1);  // 性别必填
        doctor.setIsDeleted((short) 0);
        return doctor;
    }
}
