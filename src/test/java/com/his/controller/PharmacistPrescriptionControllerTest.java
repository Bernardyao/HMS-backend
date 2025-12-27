package com.his.controller;

import com.his.config.JwtAuthenticationToken;
import com.his.entity.*;
import com.his.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("药师工作站集成测试")
class PharmacistPrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Long testPrescriptionId;
    private Long testPharmacistUserId = 100L;

    @BeforeEach
    void setUp() {
        prescriptionDetailRepository.deleteAll();
        prescriptionRepository.deleteAll();
        medicalRecordRepository.deleteAll();
        registrationRepository.deleteAll();
        doctorRepository.deleteAll();
        departmentRepository.deleteAll();
        patientRepository.deleteAll();
        medicineRepository.deleteAll();

        // Setup basic data
        Department dept = new Department();
        dept.setDeptCode("PHARM");
        dept.setName("药剂科");
        dept = departmentRepository.save(dept);

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC001");
        doctor.setName("张医生");
        doctor.setDepartment(dept);
        doctor.setGender((short) 1);
        doctor = doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setPatientNo("P001");
        patient.setName("李四");
        patient.setGender((short) 1);
        patient = patientRepository.save(patient);

        Registration reg = new Registration();
        reg.setRegNo("R001");
        reg.setPatient(patient);
        reg.setDoctor(doctor);
        reg.setDepartment(dept);
        reg.setVisitDate(java.time.LocalDate.now());
        reg = registrationRepository.save(reg);

        MedicalRecord record = new MedicalRecord();
        record.setRecordNo("REC001");
        record.setRegistration(reg);
        record.setPatient(patient);
        record.setDoctor(doctor);
        record = medicalRecordRepository.save(record);

        Medicine medicine = new Medicine();
        medicine.setMedicineCode("MED001");
        medicine.setName("感冒灵");
        medicine.setRetailPrice(new BigDecimal("10.00"));
        medicine.setStockQuantity(100);
        medicine = medicineRepository.save(medicine);

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo("PRE001");
        prescription.setMedicalRecord(record);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setStatus((short) 5); // PAID (已缴费) - 收费模块要求必须先缴费才能发药
        prescription.setTotalAmount(new BigDecimal("20.00"));
        prescription.setItemCount(2);
        prescription = prescriptionRepository.save(prescription);
        testPrescriptionId = prescription.getMainId();

        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setPrescription(prescription);
        detail.setMedicine(medicine);
        detail.setMedicineName(medicine.getName());
        detail.setUnitPrice(medicine.getRetailPrice());
        detail.setQuantity(2);
        detail.setSubtotal(new BigDecimal("20.00"));
        detail = prescriptionDetailRepository.save(detail);

        // Explicitly add to details list to avoid empty list issues in same transaction
        java.util.List<PrescriptionDetail> details = new java.util.ArrayList<>();
        details.add(detail);
        prescription.setDetails(details);
        prescriptionRepository.save(prescription);
    }

    private JwtAuthenticationToken setupPharmacistAuthentication() {
        return new JwtAuthenticationToken(
                testPharmacistUserId,
                "pharmacist",
                "PHARMACIST",
                null
        );
    }

    @Test
    @DisplayName("测试查询待发药列表")
    void testGetPendingDispenseList() throws Exception {
        mockMvc.perform(get("/api/pharmacist/prescriptions/pending")
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value(5)); // PAID (已缴费)
    }

    @Test
    @DisplayName("测试发药流程")
    void testDispense_Success() throws Exception {
        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/dispense", testPrescriptionId)
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("成功")));

        // Verify status in DB
        Prescription updated = prescriptionRepository.findById(testPrescriptionId).orElseThrow();
        assert updated.getStatus() == 3;
        assert updated.getDispenseBy().equals(testPharmacistUserId);
    }

    @Test
    @DisplayName("测试未缴费处方发药（失败）")
    void testDispense_NotApproved_Fail() throws Exception {
        // Change status to REVIEWED (已审核但未缴费)
        Prescription prescription = prescriptionRepository.findById(testPrescriptionId).orElseThrow();
        prescription.setStatus((short) 2); // REVIEWED
        prescriptionRepository.save(prescription);

        mockMvc.perform(post("/api/pharmacist/prescriptions/{id}/dispense", testPrescriptionId)
                        .with(authentication(setupPharmacistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("已缴费")));
    }
}
