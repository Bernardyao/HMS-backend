package com.his.controller;

import com.his.config.JwtAuthenticationToken;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.RegStatusEnum;
import com.his.repository.ChargeRepository;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.MedicalRecordRepository;
import com.his.repository.PatientRepository;
import com.his.repository.PrescriptionDetailRepository;
import com.his.repository.PrescriptionRepository;
import com.his.repository.RegistrationRepository;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医生工作站控制器集成测试
 */
@Transactional
@DisplayName("医生工作站集成测试")
class DoctorControllerTest extends BaseControllerTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RegistrationRepository registrationRepository;
    
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    
    @Autowired
    private PrescriptionRepository prescriptionRepository;
    
    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;
    
    @Autowired
    private ChargeRepository chargeRepository;

    private Long testDeptId;
    private Long testDoctorId;
    private Long testPatientId;  // 新增：用于测试患者查询
    private Long testReg1Id;
    private Long testReg2Id;
    private Long testReg3Id;

    @BeforeEach
    protected void setUp() {
        // 防御性编程: 按外键约束顺序清理测试数据(从叶子节点到根节点)
        // 完整的依赖链:
        // Charge -> Patient, Registration
        // PrescriptionDetail -> Prescription
        // Prescription -> MedicalRecord, Patient, Doctor
        // MedicalRecord -> Registration, Patient, Doctor
        // Registration -> Patient, Doctor, Department
        // Doctor -> Department
        chargeRepository.deleteAll();
        prescriptionDetailRepository.deleteAll();
        prescriptionRepository.deleteAll();
        medicalRecordRepository.deleteAll();
        registrationRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        departmentRepository.deleteAll();

        // 防御性编程: 使用时间戳生成唯一标识,避免测试数据冲突
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        
        // 创建测试科室 - 内科
        Department department = new Department();
        department.setDeptCode("DEPT" + uniqueSuffix);
        department.setName("内科_测试" + uniqueSuffix);
        department.setStatus((short) 1);
        department.setIsDeleted((short) 0);
        department.setSortOrder(1);
        Department savedDept = departmentRepository.save(department);
        testDeptId = savedDept.getMainId();

        // 创建测试医生
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC" + uniqueSuffix);
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setTitle("主任医师");
        doctor.setDepartment(savedDept);
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        Doctor savedDoctor = doctorRepository.save(doctor);
        testDoctorId = savedDoctor.getMainId();

        // 创建测试患者1
        Patient patient1 = new Patient();
        patient1.setPatientNo("P" + uniqueSuffix + "001");
        patient1.setName("张三");
        patient1.setIdCard("32010619900101" + uniqueSuffix.substring(uniqueSuffix.length() - 4));
        patient1.setGender((short) 1);
        patient1.setAge((short) 34);
        patient1.setPhone("13900139001");
        patient1.setBloodType("A");
        patient1.setAllergyHistory("青霉素过敏");
        patient1.setMedicalHistory("高血压3年");
        patient1.setEmergencyContact("张夫人");
        patient1.setEmergencyPhone("13900139999");
        patient1.setIsDeleted((short) 0);
        Patient savedPatient1 = patientRepository.save(patient1);
        testPatientId = savedPatient1.getMainId();  // 保存患者ID用于测试

        // 创建测试患者2
        Patient patient2 = new Patient();
        patient2.setPatientNo("P" + uniqueSuffix + "002");
        patient2.setName("李四");
        patient2.setIdCard("32010619910202" + uniqueSuffix.substring(uniqueSuffix.length() - 4));
        patient2.setGender((short) 0);
        patient2.setAge((short) 33);
        patient2.setPhone("13900139002");
        patient2.setIsDeleted((short) 0);
        Patient savedPatient2 = patientRepository.save(patient2);

        // 创建测试患者3
        Patient patient3 = new Patient();
        patient3.setPatientNo("P" + uniqueSuffix + "003");
        patient3.setName("王五");
        patient3.setIdCard("32010619920303" + uniqueSuffix.substring(uniqueSuffix.length() - 4));
        patient3.setGender((short) 1);
        patient3.setAge((short) 32);
        patient3.setPhone("13900139003");
        patient3.setIsDeleted((short) 0);
        Patient savedPatient3 = patientRepository.save(patient3);

        // 创建测试挂号记录1 - 待就诊
        Registration registration1 = new Registration();
        registration1.setRegNo("R" + uniqueSuffix + "001");
        registration1.setPatient(savedPatient1);
        registration1.setDoctor(savedDoctor);
        registration1.setDepartment(savedDept);
        registration1.setVisitDate(LocalDate.now());
        registration1.setVisitType((short) 1);
        registration1.setRegistrationFee(new BigDecimal("15.00"));
        registration1.setStatus(RegStatusEnum.WAITING.getCode());
        registration1.setQueueNo("001");
        registration1.setIsDeleted((short) 0);
        Registration savedReg1 = registrationRepository.save(registration1);
        testReg1Id = savedReg1.getMainId();

        // 创建测试挂号记录2 - 待就诊
        Registration registration2 = new Registration();
        registration2.setRegNo("R" + uniqueSuffix + "002");
        registration2.setPatient(savedPatient2);
        registration2.setDoctor(savedDoctor);
        registration2.setDepartment(savedDept);
        registration2.setVisitDate(LocalDate.now());
        registration2.setVisitType((short) 1);
        registration2.setRegistrationFee(new BigDecimal("20.00"));
        registration2.setStatus(RegStatusEnum.WAITING.getCode());
        registration2.setQueueNo("002");
        registration2.setIsDeleted((short) 0);
        Registration savedReg2 = registrationRepository.save(registration2);
        testReg2Id = savedReg2.getMainId();

        // 创建测试挂号记录3 - 待就诊
        Registration registration3 = new Registration();
        registration3.setRegNo("R" + uniqueSuffix + "003");
        registration3.setPatient(savedPatient3);
        registration3.setDoctor(savedDoctor);
        registration3.setDepartment(savedDept);
        registration3.setVisitDate(LocalDate.now());
        registration3.setVisitType((short) 1);
        registration3.setRegistrationFee(new BigDecimal("15.00"));
        registration3.setStatus(RegStatusEnum.WAITING.getCode());
        registration3.setQueueNo("003");
        registration3.setIsDeleted((short) 0);
        Registration savedReg3 = registrationRepository.save(registration3);
        testReg3Id = savedReg3.getMainId();
    }
    
    /**
     * 辅助方法：创建 JWT 认证令牌（用于MockMvc请求）
     */
    private JwtAuthenticationToken setupDoctorAuthentication() {
        return new JwtAuthenticationToken(
                1L,                  // userId
                "testDoctor",        // username
                "DOCTOR",            // role
                testDoctorId         // relatedId (医生ID)
        );
    }

    @Test
    @DisplayName("测试查询候诊列表 - 个人视图返回3条记录")
    void testGetWaitingList_PersonalView_Success() throws Exception {
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .param("showAll", "false")  // 个人视图
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("个人视图")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                // 验证第一条记录（排队号001）
                .andExpect(jsonPath("$.data[0].queueNo").value("001"))
                .andExpect(jsonPath("$.data[0].patientName").value("张三"))
                .andExpect(jsonPath("$.data[0].gender").value(1))
                .andExpect(jsonPath("$.data[0].age").value(34))
                .andExpect(jsonPath("$.data[0].status").value(0))
                .andExpect(jsonPath("$.data[0].statusDesc").value("待就诊"))
                // 验证第二条记录（排队号002）
                .andExpect(jsonPath("$.data[1].queueNo").value("002"))
                .andExpect(jsonPath("$.data[1].patientName").value("李四"))
                .andExpect(jsonPath("$.data[1].gender").value(0))
                .andExpect(jsonPath("$.data[1].age").value(33))
                // 验证第三条记录（排队号003）
                .andExpect(jsonPath("$.data[2].queueNo").value("003"))
                .andExpect(jsonPath("$.data[2].patientName").value("王五"))
                .andExpect(jsonPath("$.data[2].gender").value(1))
                .andExpect(jsonPath("$.data[2].age").value(32));
    }

    @Test
    @DisplayName("测试查询候诊列表 - 科室视图返回3条记录")
    void testGetWaitingList_DepartmentView_Success() throws Exception {
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "true")  // 科室视图
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("科室视图")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("测试查询候诊列表 - 空列表（已就诊）")
    void testGetWaitingList_EmptyList() throws Exception {
        // 将所有记录标记为已就诊
        Registration reg1 = registrationRepository.findById(testReg1Id).orElseThrow();
        reg1.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(reg1);

        Registration reg2 = registrationRepository.findById(testReg2Id).orElseThrow();
        reg2.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(reg2);

        Registration reg3 = registrationRepository.findById(testReg3Id).orElseThrow();
        reg3.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(reg3);

        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("测试查询候诊列表 - 按排队号升序排列")
    void testGetWaitingList_OrderByQueueNo() throws Exception {
        // 创建一个排队号为000的记录（应该排在最前面）
        Patient patient4 = new Patient();
        patient4.setPatientNo("P20251219004");
        patient4.setName("赵六");
        patient4.setIdCard("320106199304044567");
        patient4.setGender((short) 0);
        patient4.setAge((short) 31);
        patient4.setPhone("13900139004");
        patient4.setIsDeleted((short) 0);
        Patient savedPatient4 = patientRepository.save(patient4);

        Department dept = departmentRepository.findById(testDeptId).orElseThrow();
        Doctor doc = doctorRepository.findById(testDoctorId).orElseThrow();

        Registration registration4 = new Registration();
        registration4.setRegNo("R20251219004");
        registration4.setPatient(savedPatient4);
        registration4.setDoctor(doc);
        registration4.setDepartment(dept);
        registration4.setVisitDate(LocalDate.now());
        registration4.setVisitType((short) 1);
        registration4.setRegistrationFee(new BigDecimal("15.00"));
        registration4.setStatus(RegStatusEnum.WAITING.getCode());
        registration4.setQueueNo("000");
        registration4.setIsDeleted((short) 0);
        registrationRepository.save(registration4);

        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "true")  // 使用科室视图
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].queueNo").value("000"))
                .andExpect(jsonPath("$.data[1].queueNo").value("001"))
                .andExpect(jsonPath("$.data[2].queueNo").value("002"))
                .andExpect(jsonPath("$.data[3].queueNo").value("003"));
    }

    @Test
    @DisplayName("测试更新就诊状态 - 从待就诊到已就诊")
    void testUpdateStatus_WaitingToCompleted_Success() throws Exception {
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg1Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1") // COMPLETED = 1
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("状态更新成功")));

        // 验证数据库中的状态已更新
        Registration updated = registrationRepository.findById(testReg1Id).orElseThrow();
        assert updated.getStatus().equals(RegStatusEnum.COMPLETED.getCode());
    }

    @Test
    @DisplayName("测试更新就诊状态 - 重复接诊（失败）")
    void testUpdateStatus_AlreadyCompleted_Fail() throws Exception {
        // 先更新为已就诊
        Registration reg = registrationRepository.findById(testReg1Id).orElseThrow();
        reg.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(reg);

        // 再次尝试更新为已就诊
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg1Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1") // COMPLETED = 1
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("只有[待就诊]状态的挂号才能接诊")));
    }

    @Test
    @DisplayName("测试更新就诊状态 - 挂号记录不存在")
    void testUpdateStatus_RegistrationNotFound() throws Exception {
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", 99999L)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1") // COMPLETED = 1
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("挂号记录不存在")));
    }

    @Test
    @DisplayName("测试更新就诊状态 - 无效状态码")
    void testUpdateStatus_InvalidStatus() throws Exception {
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg1Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "99") // 无效的状态码
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("无效的状态码")));
    }

    @Test
    @DisplayName("集成测试 - 完整的接诊流程")
    void testCompleteWorkflow() throws Exception {
        // 1. 查询候诊列表，应有3条记录
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        // 2. 接诊第一个患者
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg1Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3. 再次查询候诊列表，应只剩2条记录
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].queueNo").value("002"))
                .andExpect(jsonPath("$.data[1].queueNo").value("003"));

        // 4. 接诊第二个患者
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg2Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 5. 再次查询候诊列表，应只剩1条记录
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].queueNo").value("003"))
                .andExpect(jsonPath("$.data[0].patientName").value("王五"));

        // 6. 接诊最后一个患者
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg3Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 7. 最终查询候诊列表，应为空
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("测试查询候诊列表 - 只显示今日挂号")
    void testGetWaitingList_OnlyToday() throws Exception {
        // 创建一个昨天的挂号记录
        Patient patient5 = new Patient();
        patient5.setPatientNo("P20251218001");
        patient5.setName("陈七");
        patient5.setIdCard("320106199405055678");
        patient5.setGender((short) 1);
        patient5.setAge((short) 30);
        patient5.setPhone("13900139005");
        patient5.setIsDeleted((short) 0);
        Patient savedPatient5 = patientRepository.save(patient5);

        Department dept = departmentRepository.findById(testDeptId).orElseThrow();
        Doctor doc = doctorRepository.findById(testDoctorId).orElseThrow();

        Registration registrationYesterday = new Registration();
        registrationYesterday.setRegNo("R20251218001");
        registrationYesterday.setPatient(savedPatient5);
        registrationYesterday.setDoctor(doc);
        registrationYesterday.setDepartment(dept);
        registrationYesterday.setVisitDate(LocalDate.now().minusDays(1)); // 昨天
        registrationYesterday.setVisitType((short) 1);
        registrationYesterday.setRegistrationFee(new BigDecimal("15.00"));
        registrationYesterday.setStatus(RegStatusEnum.WAITING.getCode());
        registrationYesterday.setQueueNo("999");
        registrationYesterday.setIsDeleted((short) 0);
        registrationRepository.save(registrationYesterday);

        // 查询候诊列表，应该只包含今天的3条记录，不包含昨天的
        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[*].queueNo").value(not(hasItem("999"))));
    }

    @Test
    @DisplayName("测试查询候诊列表 - 空列表带有提示信息")
    void testGetWaitingList_EmptyWithMessage() throws Exception {
        // 将所有记录标记为已就诊
        registrationRepository.findAll().forEach(reg -> {
            reg.setStatus(RegStatusEnum.COMPLETED.getCode());
            registrationRepository.save(reg);
        });

        mockMvc.perform(get("/api/doctor/waiting-list")
                        .with(authentication(setupDoctorAuthentication()))
                        .param("showAll", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("暂无候诊患者")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("测试更新就诊状态 - 重复更新相同状态")
    void testUpdateStatus_SameStatus() throws Exception {
        // 先更新为已就诊
        Registration reg = registrationRepository.findById(testReg1Id).orElseThrow();
        reg.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(reg);

        // 再次尝试更新为已就诊（测试重复状态检查）
        mockMvc.perform(put("/api/doctor/registrations/{id}/status", testReg1Id)
                        .with(authentication(setupDoctorAuthentication()))
                        .param("status", "1") // COMPLETED = 1
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                // 可能触发"只有待就诊"或"已经是该状态"的提示
                .andExpect(jsonPath("$.message").exists());
    }

    // ==================== 患者查询测试用例 ====================

    @Test
    @DisplayName("测试查询患者详细信息 - 成功场景")
    void testGetPatientDetail_Success() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", testPatientId)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("查询患者信息成功")))
                .andExpect(jsonPath("$.data.patientId").value(testPatientId))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.gender").value(1))
                .andExpect(jsonPath("$.data.genderDesc").value("男"))
                .andExpect(jsonPath("$.data.age").value(34))
                // 验证脱敏字段（手机号格式）
                .andExpect(jsonPath("$.data.phone").value(matchesPattern("^\\d{3}\\*{4}\\d{4}$")))
                // 验证身份证号脱敏格式（不验证具体后4位，因为测试数据动态生成）
                .andExpect(jsonPath("$.data.idCard").value(matchesPattern("^\\d{3}\\*{11}\\d{4}$")))
                // 验证医疗信息
                .andExpect(jsonPath("$.data.bloodType").value("A"))
                .andExpect(jsonPath("$.data.allergyHistory").value("青霉素过敏"))
                .andExpect(jsonPath("$.data.medicalHistory").value("高血压3年"))
                .andExpect(jsonPath("$.data.emergencyContact").value("张夫人"))
                .andExpect(jsonPath("$.data.emergencyPhone").value("139****9999"));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 验证手机号脱敏格式")
    void testGetPatientDetail_PhoneMasked() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", testPatientId)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phone").value(matchesPattern("^\\d{3}\\*{4}\\d{4}$")));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 验证身份证号脱敏格式")
    void testGetPatientDetail_IdCardMasked() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", testPatientId)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.idCard").value(matchesPattern("^\\d{3}\\*{11}\\d{4}$")));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 患者不存在")
    void testGetPatientDetail_PatientNotFound() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", 99999L)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("患者信息不存在")));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 患者ID无效（负数）")
    void testGetPatientDetail_InvalidPatientId_Negative() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", -1L)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("患者ID必须大于0")));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 患者ID无效（0）")
    void testGetPatientDetail_InvalidPatientId_Zero() throws Exception {
        mockMvc.perform(get("/api/doctor/patients/{id}", 0L)
                        .with(authentication(setupDoctorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("患者ID必须大于0")));
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 未认证访问")
    void testGetPatientDetail_Unauthorized() throws Exception {
        // Spring Security在未认证时可能返回401或403，取决于配置
        mockMvc.perform(get("/api/doctor/patients/{id}", testPatientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(anyOf(is(401), is(403)))); // 未认证返回401，认证但无权限返回403
    }

    @Test
    @DisplayName("测试查询患者详细信息 - 管理员模式")
    void testGetPatientDetail_AdminMode() throws Exception {
        // 创建管理员认证token
        JwtAuthenticationToken adminAuth = new JwtAuthenticationToken(
                1L,              // userId
                "admin",         // username
                "ADMIN",         // role
                null             // relatedId（管理员可能没有）
        );

        mockMvc.perform(get("/api/doctor/patients/{id}", testPatientId)
                        .param("adminPatientId", String.valueOf(testPatientId))
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.patientId").value(testPatientId))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }
}
