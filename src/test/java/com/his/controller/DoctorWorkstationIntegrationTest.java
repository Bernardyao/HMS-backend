package com.his.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.dto.MedicalRecordDTO;
import com.his.dto.PrescriptionDTO;
import com.his.entity.*;
import com.his.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医生工作站 - 病历与处方集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("医生工作站 - 病历与处方集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoctorWorkstationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    private static Long testDeptId;
    private static Long testDoctorId;
    private static Long testPatientId;
    private static Long testRegistrationId;
    private static Long testMedicine1Id;
    private static Long testMedicine2Id;

    @BeforeAll
    static void beforeAll() {
        System.out.println("=== 开始医生工作站集成测试 ===");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("=== 医生工作站集成测试完成 ===");
    }

    @BeforeEach
    void setUp() {
        // 准备测试数据：科室
        Department department = new Department();
        department.setDeptCode("D001");
        department.setName("内科");
        department.setStatus((short) 1);
        department.setIsDeleted((short) 0);
        department.setSortOrder(1);
        Department savedDept = departmentRepository.save(department);
        testDeptId = savedDept.getMainId();

        // 准备测试数据：医生
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC001");
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(savedDept);
        doctor.setTitle("主任医师");
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        Doctor savedDoctor = doctorRepository.save(doctor);
        testDoctorId = savedDoctor.getMainId();

        // 准备测试数据：患者
        Patient patient = new Patient();
        patient.setPatientNo("P001");
        patient.setName("李患者");
        patient.setIdCard("320106199501012345");
        patient.setGender((short) 1);
        patient.setAge((short) 28);
        patient.setPhone("13900139000");
        patient.setIsDeleted((short) 0);
        Patient savedPatient = patientRepository.save(patient);
        testPatientId = savedPatient.getMainId();

        // 准备测试数据：挂号单
        Registration registration = new Registration();
        registration.setRegNo("REG" + System.currentTimeMillis());
        registration.setPatient(savedPatient);
        registration.setDepartment(savedDept);
        registration.setDoctor(savedDoctor);
        registration.setVisitDate(LocalDate.now());
        registration.setRegistrationFee(new BigDecimal("15.00"));
        registration.setStatus((short) 0);
        registration.setIsDeleted((short) 0);
        Registration savedReg = registrationRepository.save(registration);
        testRegistrationId = savedReg.getMainId();

        // 准备测试数据：药品1
        Medicine medicine1 = new Medicine();
        medicine1.setMedicineCode("MED001");
        medicine1.setName("阿莫西林胶囊");
        medicine1.setRetailPrice(new BigDecimal("12.5000"));
        medicine1.setStockQuantity(1000);
        medicine1.setStatus((short) 1);
        medicine1.setIsDeleted((short) 0);
        medicine1.setSpecification("0.25g*24粒");
        medicine1.setUnit("盒");
        Medicine savedMed1 = medicineRepository.save(medicine1);
        testMedicine1Id = savedMed1.getMainId();

        // 准备测试数据：药品2
        Medicine medicine2 = new Medicine();
        medicine2.setMedicineCode("MED002");
        medicine2.setName("布洛芬片");
        medicine2.setRetailPrice(new BigDecimal("8.8000"));
        medicine2.setStockQuantity(500);
        medicine2.setStatus((short) 1);
        medicine2.setIsDeleted((short) 0);
        medicine2.setSpecification("0.1g*20片");
        medicine2.setUnit("盒");
        Medicine savedMed2 = medicineRepository.save(medicine2);
        testMedicine2Id = savedMed2.getMainId();
    }

    // ==================== 药品模块测试 ====================

    @Test
    @Order(1)
    @DisplayName("1. 测试搜索药品 - 根据名称模糊查询")
    @WithMockUser(roles = "DOCTOR")
    void testSearchMedicinesByName() throws Exception {
        mockMvc.perform(get("/api/medicine/search")
                        .param("keyword", "阿莫西林")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value(containsString("阿莫西林")));
    }

    @Test
    @Order(2)
    @DisplayName("2. 测试搜索药品 - 根据编码查询")
    @WithMockUser(roles = "DOCTOR")
    void testSearchMedicinesByCode() throws Exception {
        mockMvc.perform(get("/api/medicine/search")
                        .param("keyword", "MED001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].medicineCode").value("MED001"));
    }

    @Test
    @Order(3)
    @DisplayName("3. 测试查询药品详情")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicineById() throws Exception {
        mockMvc.perform(get("/api/medicine/{id}", testMedicine1Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.retailPrice").value(12.5))
                .andExpect(jsonPath("$.data.specification").value("0.25g*24粒"));
    }

    // ==================== 病历模块测试 ====================

    @Test
    @Order(4)
    @DisplayName("4. 测试创建病历")
    @WithMockUser(roles = "DOCTOR")
    void testCreateMedicalRecord() throws Exception {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setChiefComplaint("头痛、发热3天");
        dto.setPresentIllness("患者3天前无明显诱因出现头痛、发热，体温最高38.5°C");
        dto.setPastHistory("既往体健，无慢性病史");
        dto.setPhysicalExam("T: 38.5°C, P: 90次/分, R: 20次/分, BP: 120/80mmHg");
        dto.setDiagnosis("上呼吸道感染");
        dto.setDiagnosisCode("J06.9");
        dto.setTreatmentPlan("1. 抗感染治疗 2. 对症处理");
        dto.setDoctorAdvice("注意休息，多饮水");
        dto.setStatus((short) 1);

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("保存成功"))
                .andExpect(jsonPath("$.data.recordNo").isNotEmpty())
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛、发热3天"))
                .andExpect(jsonPath("$.data.diagnosis").value("上呼吸道感染"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("5. 测试更新病历 - 同一挂号单ID应更新而非新建")
    @WithMockUser(roles = "DOCTOR")
    void testUpdateMedicalRecord() throws Exception {
        // 第一次创建病历
        MedicalRecordDTO dto1 = new MedicalRecordDTO();
        dto1.setRegistrationId(testRegistrationId);
        dto1.setChiefComplaint("初次主诉");
        dto1.setDiagnosis("初步诊断");

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chiefComplaint").value("初次主诉"));

        // 第二次使用相同挂号单ID，应该更新病历
        MedicalRecordDTO dto2 = new MedicalRecordDTO();
        dto2.setRegistrationId(testRegistrationId);
        dto2.setChiefComplaint("更新后的主诉");
        dto2.setDiagnosis("更新后的诊断");

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chiefComplaint").value("更新后的主诉"))
                .andExpect(jsonPath("$.data.diagnosis").value("更新后的诊断"));

        // 验证数据库中只有一条病历记录
        List<MedicalRecord> records = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(testRegistrationId, (short) 0)
                .stream().toList();
        Assertions.assertEquals(1, records.size(), "同一挂号单应只有一条病历记录");
    }

    @Test
    @Order(6)
    @DisplayName("6. 测试根据挂号单ID查询病历")
    @WithMockUser(roles = "DOCTOR")
    void testGetMedicalRecordByRegistrationId() throws Exception {
        // 先创建病历
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setChiefComplaint("测试主诉");

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        // 再查询
        mockMvc.perform(get("/api/medical-record/by-registration/{registrationId}", testRegistrationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chiefComplaint").value("测试主诉"));
    }

    // ==================== 处方模块测试（核心） ====================

    @Test
    @Order(7)
    @DisplayName("7. 测试创建处方 - 核心业务：从数据库读取单价并计算总金额")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription() throws Exception {
        // 先创建病历
        MedicalRecordDTO recordDTO = new MedicalRecordDTO();
        recordDTO.setRegistrationId(testRegistrationId);
        recordDTO.setDiagnosis("上呼吸道感染");
        recordDTO.setStatus((short) 1);

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isOk());

        // 创建处方
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setPrescriptionType((short) 1);
        dto.setValidityDays(3);

        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();

        // 药品1：阿莫西林胶囊，数量2盒
        PrescriptionDTO.PrescriptionItemDTO item1 = new PrescriptionDTO.PrescriptionItemDTO();
        item1.setMedicineId(testMedicine1Id);
        item1.setQuantity(2);
        item1.setFrequency("一日三次");
        item1.setDosage("每次2粒");
        item1.setRoute("口服");
        item1.setDays(7);
        items.add(item1);

        // 药品2：布洛芬片，数量1盒
        PrescriptionDTO.PrescriptionItemDTO item2 = new PrescriptionDTO.PrescriptionItemDTO();
        item2.setMedicineId(testMedicine2Id);
        item2.setQuantity(1);
        item2.setFrequency("一日三次");
        item2.setDosage("每次1片");
        item2.setRoute("口服");
        item2.setInstructions("饭后服用");
        items.add(item2);

        dto.setItems(items);

        // 预期总金额 = 阿莫西林(12.5 * 2) + 布洛芬(8.8 * 1) = 25.0 + 8.8 = 33.8
        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("处方创建成功"))
                .andExpect(jsonPath("$.data.prescriptionNo").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(33.80))
                .andExpect(jsonPath("$.data.itemCount").value(3))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @Order(8)
    @DisplayName("8. 测试创建处方 - 验证价格防篡改（使用数据库价格）")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_PriceSecurity() throws Exception {
        // 先创建病历
        MedicalRecordDTO recordDTO = new MedicalRecordDTO();
        recordDTO.setRegistrationId(testRegistrationId);
        recordDTO.setDiagnosis("测试诊断");

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isOk());

        // 创建处方（注意：DTO中不传递单价，单价必须从数据库读取）
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(testRegistrationId);

        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(testMedicine1Id);
        item.setQuantity(5);
        items.add(item);
        dto.setItems(items);

        // 数据库中阿莫西林单价为 12.5，数量5，预期总金额 = 12.5 * 5 = 62.5
        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(62.50));

        // 验证处方明细中的单价是从数据库读取的
        String response = mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        
        Long prescriptionId = Long.parseLong(
            response.substring(response.indexOf("\"mainId\":") + 9, 
            response.indexOf(",", response.indexOf("\"mainId\":"))));

        List<PrescriptionDetail> details = prescriptionDetailRepository
                .findByPrescription_MainIdAndIsDeletedOrderBySortOrder(prescriptionId, (short) 0);
        
        Assertions.assertEquals(1, details.size());
        Assertions.assertEquals(new BigDecimal("12.5000"), details.get(0).getUnitPrice(),
                "处方明细中的单价必须与数据库中的药品单价一致");
    }

    @Test
    @Order(9)
    @DisplayName("9. 测试创建处方 - 未创建病历应失败")
    @WithMockUser(roles = "DOCTOR")
    void testCreatePrescription_WithoutMedicalRecord() throws Exception {
        // 创建新的挂号单（没有病历）
        Registration newReg = new Registration();
        newReg.setRegNo("REG_NEW" + System.currentTimeMillis());
        newReg.setPatient(patientRepository.findById(testPatientId).get());
        newReg.setDepartment(departmentRepository.findById(testDeptId).get());
        newReg.setDoctor(doctorRepository.findById(testDoctorId).get());
        newReg.setVisitDate(LocalDate.now());
        newReg.setRegistrationFee(new BigDecimal("15.00"));
        newReg.setStatus((short) 0);
        newReg.setIsDeleted((short) 0);
        Registration savedNewReg = registrationRepository.save(newReg);

        // 尝试创建处方（没有病历）
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(savedNewReg.getMainId());

        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(testMedicine1Id);
        item.setQuantity(1);
        items.add(item);
        dto.setItems(items);

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("请先创建病历")));
    }

    @Test
    @Order(10)
    @DisplayName("10. 测试查询处方详情")
    @WithMockUser(roles = "DOCTOR")
    void testGetPrescriptionById() throws Exception {
        // 先创建病历和处方
        MedicalRecordDTO recordDTO = new MedicalRecordDTO();
        recordDTO.setRegistrationId(testRegistrationId);
        recordDTO.setDiagnosis("测试诊断");
        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isOk());

        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(testRegistrationId);
        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(testMedicine1Id);
        item.setQuantity(1);
        items.add(item);
        dto.setItems(items);

        String response = mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        Long prescriptionId = Long.parseLong(
            response.substring(response.indexOf("\"mainId\":") + 9, 
            response.indexOf(",", response.indexOf("\"mainId\":"))));

        // 查询处方详情
        mockMvc.perform(get("/api/prescription/{id}", prescriptionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.prescriptionNo").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").exists());
    }

    @Test
    @Order(11)
    @DisplayName("11. 测试权限控制 - 未授权用户无法访问")
    void testUnauthorizedAccess() throws Exception {
        // 在测试环境中，未使用 @WithMockUser 时应该返回 403 Forbidden
        mockMvc.perform(get("/api/medicine/search")
                        .param("keyword", "阿莫西林"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("12. 测试完整工作流：搜索药品 -> 创建病历 -> 开处方")
    @WithMockUser(roles = "DOCTOR")
    void testCompleteWorkflow() throws Exception {
        // Step 1: 搜索药品
        mockMvc.perform(get("/api/medicine/search")
                        .param("keyword", "阿莫西林"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mainId").value(testMedicine1Id));

        // Step 2: 创建病历
        MedicalRecordDTO recordDTO = new MedicalRecordDTO();
        recordDTO.setRegistrationId(testRegistrationId);
        recordDTO.setChiefComplaint("感冒发热");
        recordDTO.setDiagnosis("上呼吸道感染");
        recordDTO.setTreatmentPlan("抗感染治疗");

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 3: 开处方
        PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
        prescriptionDTO.setRegistrationId(testRegistrationId);

        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item1 = new PrescriptionDTO.PrescriptionItemDTO();
        item1.setMedicineId(testMedicine1Id);
        item1.setQuantity(2);
        item1.setFrequency("一日三次");
        item1.setDosage("每次2粒");
        items.add(item1);

        PrescriptionDTO.PrescriptionItemDTO item2 = new PrescriptionDTO.PrescriptionItemDTO();
        item2.setMedicineId(testMedicine2Id);
        item2.setQuantity(1);
        item2.setFrequency("必要时");
        items.add(item2);

        prescriptionDTO.setItems(items);

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prescriptionDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalAmount").value(33.80))
                .andExpect(jsonPath("$.data.itemCount").value(3));

        // 验证数据已正确保存
        List<Prescription> prescriptions = prescriptionRepository
                .findByMedicalRecord_MainIdAndIsDeleted(
                        medicalRecordRepository.findByRegistration_MainIdAndIsDeleted(testRegistrationId, (short) 0)
                                .get().getMainId(), (short) 0);
        
        Assertions.assertTrue(prescriptions.size() >= 1, "应该至少有一个处方");
    }
}
