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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 新增功能完整集成测试
 * 测试药品、病历、处方三大模块的API响应格式
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(value = "test", inheritProfiles = false)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // 所有测试方法共享同一个测试类实例
public class WorkstationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Long testMedicineId;
    private Long testRegistrationId;  // 实例变量，在PER_CLASS模式下可以跨测试共享
    private Long testRecordId;
    private Long testPrescriptionId;

    @BeforeAll
    void setUpOnce() {
        // 创建一次性的测试数据（科室、医生、患者、挂号单）
        // 这些数据在所有测试中共享

        // 1. 创建科室
        Department department = new Department();
        department.setDeptCode("TEST_DEPT");
        department.setName("测试内科");
        department.setStatus((short) 1);
        department.setIsDeleted((short) 0);
        department = departmentRepository.save(department);

        // 2. 创建医生
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC001");
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        doctor.setDepartment(department);
        doctor = doctorRepository.save(doctor);

        // 3. 创建患者
        Patient patient = new Patient();
        patient.setPatientNo("PAT001");
        patient.setName("测试患者");
        patient.setGender((short) 1);
        patient.setAge((short) 30);
        patient.setIsDeleted((short) 0);
        patient = patientRepository.save(patient);

        // 4. 创建挂号单
        Registration registration = new Registration();
        registration.setRegNo("REG" + System.currentTimeMillis());
        registration.setVisitDate(LocalDate.now());
        registration.setVisitType((short) 1);
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setStatus((short) 1);
        registration.setIsDeleted((short) 0);
        registration.setPatient(patient);
        registration.setDoctor(doctor);
        registration.setDepartment(department);
        registration = registrationRepository.save(registration);
        testRegistrationId = registration.getMainId();
    }

    @BeforeEach
    void setUp() {
        // 每个测试前创建新的药品
        Medicine medicine = new Medicine();
        medicine.setMedicineCode("TEST001");
        medicine.setName("测试药品-阿莫西林");
        medicine.setGenericName("阿莫西林");
        medicine.setRetailPrice(new BigDecimal("12.50"));
        medicine.setStockQuantity(1000);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setManufacturer("测试制药厂");
        medicine.setCategory("抗生素");
        medicine.setIsPrescription((short) 1);
        medicine = medicineRepository.save(medicine);
        testMedicineId = medicine.getMainId();
    }

    // ==================== 药品模块测试 ====================

    @Test
    @Order(1)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("1. 测试搜索药品 - 应返回MedicineVO格式")
    void testSearchMedicines() throws Exception {
        mockMvc.perform(get("/api/common/medicines/search")
                .param("keyword", "阿莫")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mainId").exists())
                .andExpect(jsonPath("$.data[0].medicineCode").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].retailPrice").exists())
                .andExpect(jsonPath("$.data[0].stockQuantity").exists())
                .andExpect(jsonPath("$.data[0].status").exists())
                .andExpect(jsonPath("$.data[0].specification").exists())
                .andExpect(jsonPath("$.data[0].unit").exists())
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data[0].prescriptionDetails").doesNotExist());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("2. 测试查询药品详情 - 应返回MedicineVO格式")
    void testGetMedicineById() throws Exception {
        mockMvc.perform(get("/api/common/medicines/{id}", testMedicineId)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.mainId").value(testMedicineId))
                .andExpect(jsonPath("$.data.medicineCode").exists())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.retailPrice").isNumber())
                .andExpect(jsonPath("$.data.stockQuantity").isNumber())
                .andExpect(jsonPath("$.data.genericName").exists())
                .andExpect(jsonPath("$.data.manufacturer").exists())
                .andExpect(jsonPath("$.data.category").exists())
                .andExpect(jsonPath("$.data.isPrescription").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.prescriptionDetails").doesNotExist());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("3. 测试查询不存在的药品 - 应返回400错误")
    void testGetMedicineById_NotFound() throws Exception {
        mockMvc.perform(get("/api/common/medicines/{id}", 999999L)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("不存在")));
    }

    // ==================== 病历模块测试 ====================

    @Test
    @Order(4)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("4. 测试创建病历 - 应返回MedicalRecordVO格式")
    void testCreateMedicalRecord() throws Exception {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setChiefComplaint("头痛、发热3天");
        dto.setPresentIllness("患者3天前无明显诱因出现头痛、发热，体温最高达39°C");
        dto.setPastHistory("既往体健，无慢性病史");
        dto.setPhysicalExam("T: 38.5°C, P: 90次/分, R: 20次/分, BP: 120/80mmHg");
        dto.setDiagnosis("上呼吸道感染");
        dto.setDiagnosisCode("J06.9");
        dto.setTreatmentPlan("1. 抗感染治疗 2. 对症处理");
        dto.setDoctorAdvice("注意休息，多饮水");
        dto.setStatus((short) 1);

        MvcResult result = mockMvc.perform(post("/api/doctor/medical-records/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("保存成功"))
                .andExpect(jsonPath("$.data.mainId").exists())
                .andExpect(jsonPath("$.data.recordNo").exists())
                .andExpect(jsonPath("$.data.recordNo", startsWith("MR")))
                .andExpect(jsonPath("$.data.registrationId").value(testRegistrationId))
                .andExpect(jsonPath("$.data.patientId").exists())
                .andExpect(jsonPath("$.data.patientName").exists())
                .andExpect(jsonPath("$.data.doctorId").exists())
                .andExpect(jsonPath("$.data.doctorName").exists())
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛、发热3天"))
                .andExpect(jsonPath("$.data.diagnosis").value("上呼吸道感染"))
                .andExpect(jsonPath("$.data.diagnosisCode").value("J06.9"))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.visitTime").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.prescriptions").doesNotExist())
                .andExpect(jsonPath("$.data.registration").doesNotExist())
                .andExpect(jsonPath("$.data.patient").doesNotExist())
                .andReturn();

        // 保存recordId供后续测试使用
        String responseBody = result.getResponse().getContentAsString();
        testRecordId = objectMapper.readTree(responseBody).get("data").get("mainId").asLong();
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("5. 测试查询病历详情 - 应返回MedicalRecordVO格式")
    void testGetMedicalRecordById() throws Exception {
        Assumptions.assumeTrue(testRecordId != null, "需要先创建病历");

        mockMvc.perform(get("/api/doctor/medical-records/{id}", testRecordId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(testRecordId))
                .andExpect(jsonPath("$.data.recordNo", startsWith("MR")))
                .andExpect(jsonPath("$.data.patientName").exists())
                .andExpect(jsonPath("$.data.doctorName").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.prescriptions").doesNotExist());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("6. 测试根据挂号单查询病历 - 应返回MedicalRecordVO格式")
    void testGetMedicalRecordByRegistrationId() throws Exception {
        mockMvc.perform(get("/api/doctor/medical-records/by-registration/{registrationId}", testRegistrationId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.registrationId").value(testRegistrationId))
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.prescriptions").doesNotExist());
    }

    // ==================== 处方模块测试 ====================

    @Test
    @Order(7)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("7. 测试创建处方 - 应返回PrescriptionVO格式并正确计算金额")
    void testCreatePrescription() throws Exception {
        Assumptions.assumeTrue(testRecordId != null, "需要先创建病历");

        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setPrescriptionType((short) 1);
        dto.setValidityDays(3);

        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item1 = new PrescriptionDTO.PrescriptionItemDTO();
        item1.setMedicineId(testMedicineId);
        item1.setQuantity(2);
        item1.setFrequency("一日三次");
        item1.setDosage("每次2粒");
        item1.setRoute("口服");
        item1.setDays(7);
        items.add(item1);
        dto.setItems(items);

        MvcResult result = mockMvc.perform(post("/api/doctor/prescriptions/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("处方创建成功"))
                .andExpect(jsonPath("$.data.mainId").exists())
                .andExpect(jsonPath("$.data.prescriptionNo").exists())
                .andExpect(jsonPath("$.data.prescriptionNo", startsWith("PRE")))
                .andExpect(jsonPath("$.data.recordId").value(testRecordId))
                .andExpect(jsonPath("$.data.patientId").exists())
                .andExpect(jsonPath("$.data.patientName").exists())
                .andExpect(jsonPath("$.data.doctorId").exists())
                .andExpect(jsonPath("$.data.doctorName").exists())
                .andExpect(jsonPath("$.data.prescriptionType").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(25.00))  // 12.50 * 2 = 25.00
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.validityDays").value(3))
                .andExpect(jsonPath("$.data.createdAt").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.medicalRecord").doesNotExist())
                .andExpect(jsonPath("$.data.patient").doesNotExist())
                .andExpect(jsonPath("$.data.doctor").doesNotExist())
                .andReturn();

        // 保存prescriptionId供后续测试使用
        String responseBody = result.getResponse().getContentAsString();
        testPrescriptionId = objectMapper.readTree(responseBody).get("data").get("mainId").asLong();
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("8. 测试查询处方详情 - 应返回PrescriptionVO格式")
    void testGetPrescriptionById() throws Exception {
        Assumptions.assumeTrue(testPrescriptionId != null, "需要先创建处方");

        mockMvc.perform(get("/api/doctor/prescriptions/{id}", testPrescriptionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainId").value(testPrescriptionId))
                .andExpect(jsonPath("$.data.prescriptionNo", startsWith("PRE")))
                .andExpect(jsonPath("$.data.patientName").exists())
                .andExpect(jsonPath("$.data.doctorName").exists())
                .andExpect(jsonPath("$.data.totalAmount").isNumber())
                .andExpect(jsonPath("$.data.itemCount").isNumber())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data.medicalRecord").doesNotExist())
                .andExpect(jsonPath("$.data.details").doesNotExist());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("9. 测试根据病历查询处方列表 - 应返回PrescriptionVO列表")
    void testGetPrescriptionsByRecordId() throws Exception {
        Assumptions.assumeTrue(testRecordId != null, "需要先创建病历");

        mockMvc.perform(get("/api/doctor/prescriptions/by-record/{recordId}", testRecordId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mainId").exists())
                .andExpect(jsonPath("$.data[0].prescriptionNo").exists())
                .andExpect(jsonPath("$.data[0].totalAmount").exists())
                // 确保不返回关联对象
                .andExpect(jsonPath("$.data[0].medicalRecord").doesNotExist());
    }

    @Test
    @Order(10)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("10. 测试创建处方未先创建病历 - 应返回400错误")
    void testCreatePrescription_NoMedicalRecord() throws Exception {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(999999L);  // 不存在的挂号单
        dto.setPrescriptionType((short) 1);
        
        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(testMedicineId);
        item.setQuantity(1);
        items.add(item);
        dto.setItems(items);

        mockMvc.perform(post("/api/doctor/prescriptions/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", anyOf(
                    containsString("不存在"),
                    containsString("请先创建病历")
                )));
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("11. 测试价格防篡改 - 金额应从数据库读取")
    void testPriceAntiTampering() throws Exception {
        Assumptions.assumeTrue(testRecordId != null, "需要先创建病历");

        // 查询药品当前价格
        Medicine medicine = medicineRepository.findById(testMedicineId).orElseThrow();
        BigDecimal expectedPrice = medicine.getRetailPrice();
        int quantity = 3;
        BigDecimal expectedTotal = expectedPrice.multiply(BigDecimal.valueOf(quantity));

        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setRegistrationId(testRegistrationId);
        dto.setPrescriptionType((short) 1);
        
        List<PrescriptionDTO.PrescriptionItemDTO> items = new ArrayList<>();
        PrescriptionDTO.PrescriptionItemDTO item = new PrescriptionDTO.PrescriptionItemDTO();
        item.setMedicineId(testMedicineId);
        item.setQuantity(quantity);
        items.add(item);
        dto.setItems(items);

        mockMvc.perform(post("/api/doctor/prescriptions/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 验证总金额 = 数据库单价 × 数量
                .andExpect(jsonPath("$.data.totalAmount").value(expectedTotal.doubleValue()))
                .andExpect(jsonPath("$.data.itemCount").value(quantity));
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "PHARMACIST")
    @DisplayName("12. 测试审核处方 - 药师角色")
    void testReviewPrescription() throws Exception {
        Assumptions.assumeTrue(testPrescriptionId != null, "需要先创建处方");

        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/review", testPrescriptionId)
                .param("reviewDoctorId", "2")
                .param("remark", "处方合理，准予发药"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("审核成功"));
    }

    // ==================== 权限测试 ====================

    @Test
    @Order(13)
    @WithMockUser(roles = "NURSE")
    @DisplayName("13. 测试公共API访问 - 护士角色可以搜索药品")
    void testAccessDenied_Nurse() throws Exception {
        mockMvc.perform(get("/api/common/medicines/search")
                .param("keyword", "阿莫"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(14)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("14. 测试医生不能审核处方 - 应返回403")
    void testAccessDenied_DoctorReview() throws Exception {
        Assumptions.assumeTrue(testPrescriptionId != null, "需要先创建处方");

        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/review", testPrescriptionId)
                .param("reviewDoctorId", "2")
                .param("remark", "测试"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
