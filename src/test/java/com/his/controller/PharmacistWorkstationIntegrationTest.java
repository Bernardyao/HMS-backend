package com.his.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.his.config.JwtAuthenticationToken;
import com.his.entity.*;
import com.his.repository.*;
import com.his.test.base.BaseControllerTest;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 药师工作站 - 集成测试
 * 覆盖：发药、退药、库存管理、统计查询
 */
@DisplayName("药师工作站 - 集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PharmacistWorkstationIntegrationTest extends BaseControllerTest {

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

    private static Long testMedicineId;
    private static Long testPrescriptionId;
    private static Long pharmacistUserId = 100L;

    @BeforeEach
    protected void setUp() {
        // 不使用deleteAll，使用timestamp确保数据唯一性
        // 1. 准备基础数据
        String timestamp = String.valueOf(System.currentTimeMillis());

        Department dept = new Department();
        dept.setDeptCode("PHARM_" + timestamp);
        dept.setName("药剂科");
        dept.setStatus((short) 1);
        dept.setIsDeleted((short) 0);
        dept = departmentRepository.save(dept);

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC_" + timestamp);
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(dept);
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        doctor = doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setPatientNo("P_" + timestamp);
        patient.setName("李四");
        patient.setGender((short) 1);
        patient.setIsDeleted((short) 0);
        patient = patientRepository.save(patient);

        Registration reg = new Registration();
        reg.setRegNo("R_" + timestamp);
        reg.setPatient(patient);
        reg.setDoctor(doctor);
        reg.setDepartment(dept);
        reg.setVisitDate(LocalDate.now());
        reg.setIsDeleted((short) 0);
        reg = registrationRepository.save(reg);

        MedicalRecord record = new MedicalRecord();
        record.setRecordNo("REC_" + timestamp);
        record.setRegistration(reg);
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setChiefComplaint("测试主诉");
        record.setStatus((short) 1);
        record.setIsDeleted((short) 0);
        record = medicalRecordRepository.save(record);

        // 2. 准备药品（初始库存 100）
        Medicine medicine = new Medicine();
        medicine.setMedicineCode("MED_" + timestamp);
        medicine.setName("感冒灵");
        medicine.setUnit("盒");
        medicine.setRetailPrice(new BigDecimal("10.00"));
        medicine.setStockQuantity(100);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);
        medicine = medicineRepository.save(medicine);
        testMedicineId = medicine.getMainId();

        // 3. 准备已缴费的处方（包含2盒感冒灵）
        // 注意：收费模块要求处方必须先缴费（状态=PAID=5）才能发药
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo("PRE_" + timestamp);
        prescription.setMedicalRecord(record);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setStatus((short) 5); // PAID (已缴费) - 收费模块要求必须先缴费才能发药
        prescription.setTotalAmount(new BigDecimal("20.00"));
        prescription.setItemCount(2);
        prescription.setPrescriptionType((short) 1);
        prescription.setIsDeleted((short) 0);
        prescription = prescriptionRepository.save(prescription);
        testPrescriptionId = prescription.getMainId();

        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setPrescription(prescription);
        detail.setMedicine(medicine);
        detail.setMedicineName(medicine.getName());
        detail.setUnitPrice(medicine.getRetailPrice());
        detail.setQuantity(2); // 2盒
        detail.setSubtotal(new BigDecimal("20.00"));
        detail.setIsDeleted((short) 0);
        detail = prescriptionDetailRepository.save(detail);

        List<PrescriptionDetail> details = new ArrayList<>();
        details.add(detail);
        prescription.setDetails(details);
        prescriptionRepository.save(prescription);
    }

    private JwtAuthenticationToken setupPharmacistAuthentication() {
        return new JwtAuthenticationToken(
                pharmacistUserId,
                "pharmacist",
                "PHARMACIST",
                null
        );
    }

    @Test
    @Order(1)
    @DisplayName("1. 待发药列表查询 - 应能看到已审核的处方")
    void testGetPendingDispenseList() throws Exception {
        mockMvc.perform(get("/api/pharmacist/prescriptions/pending")
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1))) // 至少有1条
                .andExpect(jsonPath("$.data[0].prescriptionNo").exists()); // 不再检查具体的处方号，因为它包含时间戳
    }

    @Test
    @Order(2)
    @DisplayName("2. 发药 - 库存应扣减，状态更新为已发药")
    void testDispense() throws Exception {
        // 执行发药
        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/dispense", testPrescriptionId)
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库状态
        Prescription p = prescriptionRepository.findById(testPrescriptionId).orElseThrow();
        Assertions.assertEquals((short) 3, p.getStatus()); // 3=已发药
        Assertions.assertEquals(pharmacistUserId, p.getDispenseBy());

        // 验证库存扣减 (100 - 2 = 98)
        Medicine m = medicineRepository.findById(testMedicineId).orElseThrow();
        Assertions.assertEquals(98, m.getStockQuantity());
    }

    @Test
    @Order(3)
    @DisplayName("3. 统计查询 - 发药后应统计到数据")
    void testStatistics() throws Exception {
        // 先执行发药
        testDispense();

        // 查询统计
        mockMvc.perform(get("/api/pharmacist/prescriptions/statistics/today")
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dispensedCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(20.0))
                .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("4. 退药 - 库存应恢复，状态更新为已退药")
    void testReturnMedicine() throws Exception {
        // 先执行发药
        testDispense();

        // 执行退药
        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/return", testPrescriptionId)
                        .param("reason", "患者要求退费")
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库状态
        Prescription p = prescriptionRepository.findById(testPrescriptionId).orElseThrow();
        Assertions.assertEquals((short) 4, p.getStatus()); // 4=已退药
        Assertions.assertEquals("患者要求退费", p.getReturnReason());

        // 验证库存恢复 (98 + 2 = 100)
        Medicine m = medicineRepository.findById(testMedicineId).orElseThrow();
        Assertions.assertEquals(100, m.getStockQuantity());
    }

    @Test
    @Order(5)
    @DisplayName("5. 手动更新库存")
    void testUpdateStock() throws Exception {
        // 手动增加库存 50
        mockMvc.perform(put("/api/pharmacist/medicines/{id}/stock", testMedicineId)
                        .param("quantity", "50")
                        .param("reason", "新货入库")
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证库存 (100 + 50 = 150)
        Medicine m = medicineRepository.findById(testMedicineId).orElseThrow();
        Assertions.assertEquals(150, m.getStockQuantity());
    }
}
