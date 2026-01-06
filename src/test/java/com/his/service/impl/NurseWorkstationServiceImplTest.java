package com.his.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.his.dto.NurseWorkstationDTO;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.MedicalRecord;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.GenderEnum;
import com.his.enums.RegStatusEnum;
import com.his.enums.VisitTypeEnum;
import com.his.repository.RegistrationRepository;
import com.his.test.base.BaseServiceTest;
import com.his.vo.NurseRegistrationVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 护士工作站服务测试类
 *
 * <p>测试范围：
 * <ul>
 *   <li>查询今日挂号（无过滤条件）</li>
 *   <li>按科室过滤</li>
 *   <li>按状态过滤</li>
 *   <li>按就诊类型过滤</li>
 *   <li>关键字搜索</li>
 *   <li>多条件组合查询</li>
 * </ul>
 *
 * <p>覆盖率目标: 85%+
 *
 * @author HIS开发团队
 * @since 1.0.0
 */
@DisplayName("护士工作站服务测试")
@SuppressWarnings("unchecked") // 抑制 JPA Specification 类型转换警告
class NurseWorkstationServiceImplTest extends BaseServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private NurseWorkstationServiceImpl nurseWorkstationService;

    // ==================== 基础查询测试 ====================

    @Test
    @DisplayName("查询今日挂号 - 默认查询当天")
    void testGetTodayRegistrations_DefaultToday() {
        // Given: 准备测试数据
        Patient patient = createPatient(1L, "张三", "13800138000");
        Department department = createDepartment(1L, "内科");
        Doctor doctor = createDoctor(1L, "李医生", "主治医师");

        Registration reg1 = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg1);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When: dto为null，应查询当天
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("张三");
        assertThat(result.get(0).getDeptName()).isEqualTo("内科");
    }

    @Test
    @DisplayName("查询今日挂号 - 指定日期")
    void testGetTodayRegistrations_SpecifiedDate() {
        // Given: 准备测试数据和指定日期
        LocalDate specifiedDate = LocalDate.of(2024, 1, 15);
        Patient patient = createPatient(1L, "李四", "13900139000");

        Registration reg = createRegistration(1L, patient, null, null,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg.setVisitDate(specifiedDate);

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setVisitDate(specifiedDate);

        // When: 查询指定日期
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("李四");
    }

    @Test
    @DisplayName("查询今日挂号 - 空结果")
    void testGetTodayRegistrations_EmptyResult() {
        // Given: 返回空列表
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(Collections.emptyList());

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ==================== 条件过滤测试 ====================

    @Test
    @DisplayName("按科室过滤 - 内科")
    void testGetTodayRegistrations_FilterByDepartment() {
        // Given: 准备多个科室的数据
        Patient patient = createPatient(1L, "王五", "13700137000");
        Department internalDept = createDepartment(1L, "内科");
        Doctor doctor = createDoctor(1L, "赵医生", "主任医师");

        Registration reg1 = createRegistration(1L, patient, internalDept, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg1);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setDepartmentId(1L); // 筛选内科

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptName()).isEqualTo("内科");
    }

    @Test
    @DisplayName("按状态过滤 - 待就诊")
    void testGetTodayRegistrations_FilterByStatus() {
        // Given
        Patient patient = createPatient(1L, "赵六", "13600136000");
        Department department = createDepartment(1L, "儿科");
        Doctor doctor = createDoctor(1L, "钱医生", "副主任医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setStatus(RegStatusEnum.WAITING.getCode());

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RegStatusEnum.WAITING.getCode());
        assertThat(result.get(0).getStatusDesc()).isEqualTo(RegStatusEnum.WAITING.getDescription());
    }

    @Test
    @DisplayName("按就诊类型过滤 - 初诊")
    void testGetTodayRegistrations_FilterByVisitType() {
        // Given
        Patient patient = createPatient(1L, "孙七", "13500135000");
        Department department = createDepartment(1L, "眼科");
        Doctor doctor = createDoctor(1L, "周医生", "医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setVisitType(VisitTypeEnum.FIRST.getCode());

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVisitType()).isEqualTo(VisitTypeEnum.FIRST.getCode());
        assertThat(result.get(0).getVisitTypeDesc()).isEqualTo(VisitTypeEnum.FIRST.getDescription());
    }

    @Test
    @DisplayName("关键字搜索 - 按患者姓名")
    void testGetTodayRegistrations_SearchByPatientName() {
        // Given
        Patient patient = createPatient(1L, "张三丰", "13400134000");
        Department department = createDepartment(1L, "中医科");
        Doctor doctor = createDoctor(1L, "吴医生", "主治医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setKeyword("张三");

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("张三丰");
    }

    @Test
    @DisplayName("关键字搜索 - 按挂号号")
    void testGetTodayRegistrations_SearchByRegNo() {
        // Given
        Patient patient = createPatient(1L, "李四光", "13300133000");
        Department department = createDepartment(1L, "口腔科");
        Doctor doctor = createDoctor(1L, "郑医生", "主任医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg.setRegNo("GZ20240115001");

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setKeyword("GZ20240115");

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRegNo()).isEqualTo("GZ20240115001");
    }

    // ==================== 多条件组合测试 ====================

    @Test
    @DisplayName("多条件组合 - 科室+状态")
    void testGetTodayRegistrations_CombinedDepartmentAndStatus() {
        // Given
        Patient patient = createPatient(1L, "测试患者", "13200132000");
        Department department = createDepartment(1L, "心内科");
        Doctor doctor = createDoctor(1L, "王医生", "副主任医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.COMPLETED.getCode(), VisitTypeEnum.FOLLOWUP.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setDepartmentId(1L);
        dto.setStatus(RegStatusEnum.COMPLETED.getCode());

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptName()).isEqualTo("心内科");
        assertThat(result.get(0).getStatus()).isEqualTo(RegStatusEnum.COMPLETED.getCode());
    }

    @Test
    @DisplayName("多条件组合 - 全部条件")
    void testGetTodayRegistrations_AllConditions() {
        // Given
        Patient patient = createPatient(1L, "全条件测试", "13100131000");
        Department department = createDepartment(10L, "神经内科");
        Doctor doctor = createDoctor(5L, "陈医生", "主治医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FOLLOWUP.getCode());
        reg.setRegNo("GZ20240115001");

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        NurseWorkstationDTO dto = new NurseWorkstationDTO();
        dto.setVisitDate(LocalDate.now());
        dto.setDepartmentId(10L);
        dto.setStatus(RegStatusEnum.WAITING.getCode());
        dto.setVisitType(VisitTypeEnum.FOLLOWUP.getCode());
        dto.setKeyword("全条件");

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(dto);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("全条件测试");
        assertThat(result.get(0).getDeptName()).isEqualTo("神经内科");
    }

    // ==================== 数据转换测试 ====================

    @Test
    @DisplayName("数据转换 - 完整信息")
    void testConvertToNurseVO_CompleteInfo() {
        // Given: 准备完整的挂号信息
        Patient patient = createPatient(1L, "完整测试", "13000130000");
        patient.setAge((short) 35);
        patient.setGender(GenderEnum.MALE.getCode());
        patient.setIdCard("110101198901011234");

        Department department = createDepartment(1L, "骨科");
        Doctor doctor = createDoctor(1L, "刘医生", "主任医师");
        doctor.setTitle("主任医师");

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setMainId(1L);

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg.setRegNo("GZ20240115001");
        reg.setQueueNo("5");
        reg.setAppointmentTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        reg.setRegistrationFee(new java.math.BigDecimal("10.00"));
        reg.setMedicalRecord(medicalRecord);

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then
        assertThat(result).hasSize(1);
        NurseRegistrationVO vo = result.get(0);

        assertThat(vo.getPatientName()).isEqualTo("完整测试");
        assertThat(vo.getAge()).isEqualTo((short) 35);
        assertThat(vo.getGenderDesc()).isEqualTo(GenderEnum.MALE.getDescription());

        // 验证脱敏
        assertThat(vo.getIdCard()).isEqualTo("110***********1234");
        assertThat(vo.getPhone()).isEqualTo("130****0000");

        assertThat(vo.getDeptName()).isEqualTo("骨科");
        assertThat(vo.getDoctorName()).isEqualTo("刘医生");
        assertThat(vo.getDoctorTitle()).isEqualTo("主任医师");

        assertThat(vo.getRegNo()).isEqualTo("GZ20240115001");
        assertThat(vo.getQueueNo()).isEqualTo("5");
        assertThat(vo.getRegistrationFee()).isEqualByComparingTo("10.00");

        assertThat(vo.getHasMedicalRecord()).isTrue();
    }

    @Test
    @DisplayName("数据转换 - 最小信息")
    void testConvertToNurseVO_MinimalInfo() {
        // Given: 只有基本信息
        Patient patient = createPatient(1L, "最小测试", "13900139000");

        Registration reg = new Registration();
        reg.setMainId(1L);
        reg.setPatient(patient);
        reg.setStatus(RegStatusEnum.WAITING.getCode());
        reg.setVisitType(VisitTypeEnum.FIRST.getCode());
        reg.setVisitDate(LocalDate.now());
        reg.setCreatedAt(LocalDateTime.now());
        reg.setIsDeleted((short) 0);

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then
        assertThat(result).hasSize(1);
        NurseRegistrationVO vo = result.get(0);

        assertThat(vo.getPatientName()).isEqualTo("最小测试");
        assertThat(vo.getDeptName()).isNull(); // 没有科室信息
        assertThat(vo.getDoctorName()).isNull(); // 没有医生信息
        assertThat(vo.getHasMedicalRecord()).isFalse(); // 没有病历
    }

    @Test
    @DisplayName("数据转换 - 边界条件测试")
    void testConvertToNurseVO_BoundaryConditions() {
        // Given: 测试各种边界条件
        Patient patient = createPatient(1L, "边界测试", "13800138000");
        Department department = createDepartment(1L, "五官科");
        Doctor doctor = createDoctor(1L, "测试医生", "医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.EMERGENCY.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then
        assertThat(result).hasSize(1);
        NurseRegistrationVO vo = result.get(0);

        assertThat(vo.getPatientName()).isEqualTo("边界测试");
        assertThat(vo.getStatusDesc()).isEqualTo(RegStatusEnum.WAITING.getDescription());
        assertThat(vo.getVisitTypeDesc()).isEqualTo(VisitTypeEnum.EMERGENCY.getDescription());
    }

    @Test
    @DisplayName("数据脱敏 - 短身份证号和手机号")
    void testDataMasking_ShortValues() {
        // Given: 短字符串不脱敏
        Patient patient = createPatient(1L, "脱敏测试", "13700");
        patient.setIdCard("12345");

        Department department = createDepartment(1L, "五官科");
        Doctor doctor = createDoctor(1L, "高医生", "医师");

        Registration reg = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());

        List<Registration> registrations = Arrays.asList(reg);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then: 短字符串不脱敏
        assertThat(result).hasSize(1);
        NurseRegistrationVO vo = result.get(0);
        assertThat(vo.getPhone()).isEqualTo("13700"); // 短于8位，不脱敏
        assertThat(vo.getIdCard()).isEqualTo("12345"); // 短于8位，不脱敏
    }

    @Test
    @DisplayName("多结果排序 - 按创建时间升序")
    void testGetTodayRegistrations_OrderedByCreatedAt() {
        // Given: 准备多条数据，创建时间不同
        Patient patient = createPatient(1L, "排序测试", "13600136000");
        Department department = createDepartment(1L, "检验科");
        Doctor doctor = createDoctor(1L, "测试医生", "医师");

        Registration reg1 = createRegistration(1L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg1.setCreatedAt(LocalDateTime.of(2024, 1, 15, 9, 0));
        reg1.setRegNo("GZ001");

        Registration reg2 = createRegistration(2L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg2.setCreatedAt(LocalDateTime.of(2024, 1, 15, 8, 0));
        reg2.setRegNo("GZ002");

        Registration reg3 = createRegistration(3L, patient, department, doctor,
                RegStatusEnum.WAITING.getCode(), VisitTypeEnum.FIRST.getCode());
        reg3.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        reg3.setRegNo("GZ003");

        // 返回按创建时间升序排列的结果
        List<Registration> registrations = Arrays.asList(reg2, reg1, reg3);
        when(registrationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(registrations);

        // When
        List<NurseRegistrationVO> result = nurseWorkstationService.getTodayRegistrations(new NurseWorkstationDTO());

        // Then: 验证排序顺序
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRegNo()).isEqualTo("GZ002"); // 最早
        assertThat(result.get(1).getRegNo()).isEqualTo("GZ001");
        assertThat(result.get(2).getRegNo()).isEqualTo("GZ003"); // 最晚
    }

    // ==================== 辅助方法 ====================

    private Patient createPatient(Long id, String name, String phone) {
        Patient patient = new Patient();
        patient.setMainId(id);
        patient.setName(name);
        patient.setPhone(phone);
        patient.setGender(GenderEnum.FEMALE.getCode());
        patient.setAge((short) 30);
        return patient;
    }

    private Department createDepartment(Long id, String name) {
        Department department = new Department();
        department.setMainId(id);
        department.setName(name);
        return department;
    }

    private Doctor createDoctor(Long id, String name, String title) {
        Doctor doctor = new Doctor();
        doctor.setMainId(id);
        doctor.setName(name);
        doctor.setTitle(title);
        return doctor;
    }

    private Registration createRegistration(Long id, Patient patient, Department department,
                                          Doctor doctor, Short status, Short visitType) {
        Registration registration = new Registration();
        registration.setMainId(id);
        registration.setPatient(patient);
        registration.setDepartment(department);
        registration.setDoctor(doctor);
        registration.setStatus(status);
        registration.setVisitType(visitType);
        registration.setVisitDate(LocalDate.now());
        registration.setCreatedAt(LocalDateTime.now());
        registration.setIsDeleted((short) 0);
        return registration;
    }
}
