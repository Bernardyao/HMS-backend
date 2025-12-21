package com.his.repository;

import com.his.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository 层集成测试
 * 使用 @Transactional 注解确保测试后自动回滚数据
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Repository 层集成测试")
class RepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("测试 Patient 的保存和自定义查询")
    void testSaveAndFind() {
        // 1. 创建患者对象
        Patient patient = createTestPatient();

        // 2. 保存患者
        Patient savedPatient = patientRepository.save(patient);

        // 3. 验证保存后 ID 不为空
        assertNotNull(savedPatient.getMainId(), "保存后患者 ID 应不为空");
        assertThat(savedPatient.getMainId()).isGreaterThan(0L);

        // 4. 使用自定义方法 findByIdCardAndIsDeleted 查询患者
        Optional<Patient> foundPatient = patientRepository.findByIdCardAndIsDeleted(
                "320106199001011234", (short) 0);

        // 5. 验证查出的数据与保存的一致
        assertTrue(foundPatient.isPresent(), "应该能查询到患者");
        assertThat(foundPatient.get().getMainId()).isEqualTo(savedPatient.getMainId());
        assertThat(foundPatient.get().getPatientNo()).isEqualTo("P20250001");
        assertThat(foundPatient.get().getName()).isEqualTo("张三");
        assertThat(foundPatient.get().getIdCard()).isEqualTo("320106199001011234");
        assertThat(foundPatient.get().getPhone()).isEqualTo("13800138000");
        assertThat(foundPatient.get().getGender()).isEqualTo((short) 1);

        // 6. (可选) 创建 Registration 关联该患者
        Department department = createTestDepartment();
        Department savedDepartment = departmentRepository.save(department);

        Doctor doctor = createTestDoctor(savedDepartment);
        Doctor savedDoctor = doctorRepository.save(doctor);

        Registration registration = createTestRegistration(savedPatient, savedDoctor, savedDepartment);
        Registration savedRegistration = registrationRepository.save(registration);

        // 7. 验证挂号记录保存成功
        assertNotNull(savedRegistration.getMainId(), "保存后挂号记录 ID 应不为空");
        assertThat(savedRegistration.getPatient().getMainId()).isEqualTo(savedPatient.getMainId());
        assertThat(savedRegistration.getDoctor().getMainId()).isEqualTo(savedDoctor.getMainId());
        assertThat(savedRegistration.getDepartment().getMainId()).isEqualTo(savedDepartment.getMainId());

        // 8. 使用自定义方法查询挂号记录
        Optional<Registration> foundRegistration = registrationRepository.findByRegNoAndIsDeleted(
                "R20250001", (short) 0);

        assertTrue(foundRegistration.isPresent(), "应该能查询到挂号记录");
        assertThat(foundRegistration.get().getMainId()).isEqualTo(savedRegistration.getMainId());
        assertThat(foundRegistration.get().getRegNo()).isEqualTo("R20250001");
        assertThat(foundRegistration.get().getStatus()).isEqualTo((short) 0);
    }

    @Test
    @DisplayName("测试 Patient 模糊查询")
    void testFindByNameContaining() {
        // 准备测试数据
        Patient patient1 = createTestPatient();
        patient1.setPatientNo("P20250001");
        patient1.setName("张三");
        patient1.setIdCard("320106199001011234");
        patientRepository.save(patient1);

        Patient patient2 = createTestPatient();
        patient2.setPatientNo("P20250002");
        patient2.setName("张三丰");
        patient2.setIdCard("320106199002022345");
        patientRepository.save(patient2);

        Patient patient3 = createTestPatient();
        patient3.setPatientNo("P20250003");
        patient3.setName("李四");
        patient3.setIdCard("320106199003033456");
        patientRepository.save(patient3);

        // 测试模糊查询
        var results = patientRepository.findByNameContainingAndIsDeleted("张", (short) 0);

        // 验证结果
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Patient::getName)
                .containsExactlyInAnyOrder("张三", "张三丰");
    }

    @Test
    @DisplayName("测试 Department 层级查询")
    void testDepartmentHierarchy() {
        // 创建父科室
        Department parentDept = new Department();
        parentDept.setDeptCode("D001");
        parentDept.setName("内科");
        parentDept.setStatus((short) 1);
        parentDept.setIsDeleted((short) 0);
        parentDept.setSortOrder(1);
        Department savedParent = departmentRepository.save(parentDept);

        // 创建子科室
        Department childDept = new Department();
        childDept.setDeptCode("D001-01");
        childDept.setName("心血管内科");
        childDept.setStatus((short) 1);
        childDept.setIsDeleted((short) 0);
        childDept.setParent(savedParent);
        childDept.setSortOrder(1);
        departmentRepository.save(childDept);

        // 查询顶级科室
        var topDepts = departmentRepository.findByParentIsNullAndIsDeletedOrderBySortOrder((short) 0);
        assertThat(topDepts).hasSize(1);
        assertThat(topDepts.get(0).getName()).isEqualTo("内科");

        // 查询子科室
        var subDepts = departmentRepository.findByParent_MainIdAndIsDeletedOrderBySortOrder(
                savedParent.getMainId(), (short) 0);
        assertThat(subDepts).hasSize(1);
        assertThat(subDepts.get(0).getName()).isEqualTo("心血管内科");
    }

    @Test
    @DisplayName("测试 Doctor 按科室查询")
    void testFindDoctorsByDepartment() {
        // 创建科室
        Department department = createTestDepartment();
        Department savedDept = departmentRepository.save(department);

        // 创建医生
        Doctor doctor1 = createTestDoctor(savedDept);
        doctor1.setDoctorNo("D001");
        doctor1.setName("王医生");
        doctorRepository.save(doctor1);

        Doctor doctor2 = createTestDoctor(savedDept);
        doctor2.setDoctorNo("D002");
        doctor2.setName("李医生");
        doctorRepository.save(doctor2);

        // 查询该科室的医生
        var doctors = doctorRepository.findByDepartment_MainIdAndIsDeleted(
                savedDept.getMainId(), (short) 0);

        assertThat(doctors).hasSize(2);
        assertThat(doctors).extracting(Doctor::getName)
                .containsExactlyInAnyOrder("王医生", "李医生");
    }

    // ============================================
    // 辅助方法：创建测试数据
    // ============================================

    /**
     * 创建测试用患者对象
     */
    private Patient createTestPatient() {
        Patient patient = new Patient();
        patient.setPatientNo("P20250001");
        patient.setName("张三");
        patient.setGender((short) 1);
        patient.setIdCard("320106199001011234");
        patient.setPhone("13800138000");
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
        patient.setAge((short) 35);
        patient.setAddress("南京市鼓楼区");
        patient.setIsDeleted((short) 0);
        patient.setMedicalCardNo("MC20250001");
        patient.setEmergencyContact("李四");
        patient.setEmergencyPhone("13900139000");
        patient.setBloodType("A");
        return patient;
    }

    /**
     * 创建测试用科室对象
     */
    private Department createTestDepartment() {
        Department department = new Department();
        department.setDeptCode("D001");
        department.setName("内科");
        department.setStatus((short) 1);
        department.setIsDeleted((short) 0);
        department.setSortOrder(1);
        department.setDescription("内科诊疗");
        return department;
    }

    /**
     * 创建测试用医生对象
     */
    private Doctor createTestDoctor(Department department) {
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("D20250001");
        doctor.setName("王医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(department);
        doctor.setTitle("主任医师");
        doctor.setSpecialty("心血管疾病");
        doctor.setPhone("13700137000");
        doctor.setEmail("doctor.wang@hospital.com");
        doctor.setLicenseNo("L20250001");
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        return doctor;
    }

    /**
     * 创建测试用挂号记录对象
     */
    private Registration createTestRegistration(Patient patient, Doctor doctor, Department department) {
        Registration registration = new Registration();
        registration.setPatient(patient);
        registration.setDoctor(doctor);
        registration.setDepartment(department);
        registration.setRegNo("R20250001");
        registration.setVisitDate(LocalDate.now());
        registration.setVisitType((short) 1);
        registration.setRegistrationFee(new BigDecimal("20.00"));
        registration.setStatus((short) 0);
        registration.setIsDeleted((short) 0);
        registration.setQueueNo("001");
        registration.setAppointmentTime(LocalDateTime.now().plusHours(1));
        return registration;
    }
}
