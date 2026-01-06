package com.his.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.service.PrescriptionService;
import com.his.test.base.BaseControllerTest;
import com.his.vo.ChargeVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分阶段收费完整流程集成测试
 *
 * 测试场景：
 * 1. 挂号收费：创建挂号收费单 → 支付 → 验证状态更新
 * 2. 处方收费：就诊完成 → 开处方 → 审核 → 创建处方收费单 → 支付
 * 3. 完整流程：挂号 → 支付挂号费 → 就诊 → 开处方 → 支付处方费 → 发药
 *
 * <p>继承自BaseControllerTest，自动获得：
 * <ul>
 *     <li>MockMvc - 用于HTTP请求测试</li>
 *     <li>ObjectMapper - 用于JSON序列化</li>
 *     <li>@Transactional - 测试数据自动回滚</li>
 * </ul>
 *
 * <p>使用integration-test profile以显示详细SQL日志
 */
@ActiveProfiles("integration-test")  // 覆盖父类的 "test" profile
@DisplayName("分阶段收费完整流程集成测试")
class PhasedChargingIntegrationTest extends BaseControllerTest {

    @Autowired
    private ChargeService chargeService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.his.repository.ChargeRepository chargeRepository;

    private Long testPatientId;
    private Long testDepartmentId;
    private Long testDoctorId;
    private Long testRegistrationId;

    @BeforeEach
    protected void setUp() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // 创建基础数据
        Department dept = new Department();
        dept.setDeptCode("D" + uid);
        dept.setName("内科");
        dept.setStatus((short) 1);
        dept.setIsDeleted((short) 0);
        testDepartmentId = departmentRepository.saveAndFlush(dept).getMainId();

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC" + uid);
        doctor.setName("王医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(dept);
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        testDoctorId = doctorRepository.saveAndFlush(doctor).getMainId();

        Patient patient = new Patient();
        patient.setPatientNo("P" + uid);
        patient.setName("张三");
        patient.setGender((short) 1);
        patient.setIdCard("ID" + uid + "XXXX");
        patient.setIsDeleted((short) 0);
        testPatientId = patientRepository.saveAndFlush(patient).getMainId();

        Medicine medicine = new Medicine();
        medicine.setMedicineCode("M" + uid);
        medicine.setName("阿莫西林胶囊");
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setRetailPrice(new BigDecimal("25.00"));
        medicine.setPurchasePrice(new BigDecimal("15.00"));
        medicine.setStockQuantity(1000);
        medicine.setMinStock(100);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);
        medicineRepository.saveAndFlush(medicine);
    }

    @Test
    @DisplayName("集成测试1：挂号收费完整流程")
    @WithMockUser(username = "cashier001", roles = {"CASHIER"})
    void testRegistrationCharge_CompleteFlow() throws Exception {
        // Step 1: 创建挂号
        testRegistrationId = createRegistration();

        // Step 2: 创建挂号收费单
        mockMvc.perform(post("/api/cashier/charges/registration/" + testRegistrationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(10.00))
                .andExpect(jsonPath("$.data.details[0].itemType").value("REGISTRATION"))
                .andExpect(jsonPath("$.data.details[0].itemAmount").value(10.00));

        // Step 3: 验证挂号费支付状态（未支付）
        mockMvc.perform(get("/api/cashier/charges/registration/" + testRegistrationId + "/payment-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        // Step 4: 支付挂号费
        Long chargeId = chargeRepository.findAll().stream()
                .filter(c -> c.getRegistration() != null && c.getRegistration().getMainId().equals(testRegistrationId))
                .filter(c -> c.getChargeType() == 1) // 挂号费类型
                .findFirst()
                .orElseThrow()
                .getMainId();
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3); // 微信
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1)); // 已缴费

        // Step 5: 验证挂号状态已更新
        Registration registration = registrationRepository.findById(testRegistrationId).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.PAID_REGISTRATION.getCode());

        // Step 6: 验证挂号费支付状态（已支付）
        mockMvc.perform(get("/api/cashier/charges/registration/" + testRegistrationId + "/payment-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Step 7: 尝试重复收费（应该失败）
        mockMvc.perform(post("/api/cashier/charges/registration/" + testRegistrationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 400 Bad Request (业务异常)
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("已支付")));
    }

    @Test
    @DisplayName("集成测试2：处方收费完整流程")
    @WithMockUser(username = "cashier001", roles = {"CASHIER"})
    void testPrescriptionCharge_CompleteFlow() throws Exception {
        // Step 1: 创建挂号并支付挂号费
        testRegistrationId = createAndPayForRegistration();

        // Step 2: 医生接诊
        Registration registration = registrationRepository.findById(testRegistrationId).orElseThrow();
        registration.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.saveAndFlush(registration);

        // Step 3: 创建病历
        MedicalRecord record = new MedicalRecord();
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());
        record.setRecordNo("MR" + System.currentTimeMillis());
        record.setChiefComplaint("头痛");
        record.setDiagnosis("上呼吸道感染");
        record.setStatus((short) 1); // 已提交
        record.setIsDeleted((short) 0);
        medicalRecordRepository.saveAndFlush(record);

        // Step 4: 开具处方
        Prescription prescription = new Prescription();
        prescription.setPatient(registration.getPatient());
        prescription.setMedicalRecord(record);
        prescription.setDoctor(registration.getDoctor());
        prescription.setPrescriptionNo("PR" + System.currentTimeMillis());
        prescription.setPrescriptionType((short) 1);
        prescription.setItemCount(1);
        prescription.setTotalAmount(new BigDecimal("25.00"));
        prescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());
        prescription.setIsDeleted((short) 0);
        prescriptionRepository.saveAndFlush(prescription);

        // Step 5: 审核处方
        prescriptionService.review(prescription.getMainId(), testDoctorId, "审核通过");

        // Verify prescription is reviewed (flush and reload)
        prescriptionRepository.flush();
        Prescription reviewedPrescription = prescriptionRepository.findById(prescription.getMainId()).orElseThrow();
        assertThat(reviewedPrescription.getStatus()).isEqualTo(PrescriptionStatusEnum.REVIEWED.getCode());

        // Step 6: 创建处方收费单
        Long prescriptionId = prescription.getMainId();
        CreateChargeDTO chargeDTO = new CreateChargeDTO();
        chargeDTO.setRegistrationId(testRegistrationId);
        chargeDTO.setPrescriptionIds(Arrays.asList(prescriptionId));

        mockMvc.perform(post("/api/cashier/charges/prescription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(25.00)) // 挂号费已支付，只有处方费
                .andExpect(jsonPath("$.data.details[0].itemType").value("PRESCRIPTION"));

        // Step 7: 支付处方费
        Long chargeId = chargeRepository.findAll().stream()
                .filter(c -> c.getChargeType() == 2) // 处方费类型
                .filter(c -> c.getRegistration() != null && c.getRegistration().getMainId().equals(testRegistrationId))
                .findFirst()
                .orElseThrow()
                .getMainId();

        // 获取实际应付金额
        Charge charge = chargeRepository.findById(chargeId).orElseThrow();
        BigDecimal amountToPay = charge.getTotalAmount();

        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3);
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(amountToPay); // 使用实际金额

        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1)); // 已缴费

        // Step 8: 验证处方状态已更新
        Prescription updatedPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertThat(updatedPrescription.getStatus()).isEqualTo(PrescriptionStatusEnum.PAID.getCode());

        // Step 9: 尝试重复收费（应该失败）
        mockMvc.perform(post("/api/cashier/charges/prescription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeDTO)))
                .andExpect(status().is4xxClientError()); // 400 Bad Request for duplicate charge
    }

    @Test
    @DisplayName("集成测试3：完整就诊流程（挂号收费+处方收费）")
    @WithMockUser(username = "cashier001", roles = {"CASHIER"})
    void testCompleteFlow_RegistrationAndPrescription() throws Exception {
        // ========== 阶段1：挂号收费 ==========

        // 1.1 创建挂号
        testRegistrationId = createRegistration();

        // 1.2 创建并支付挂号费
        createAndPayForRegistrationDirectly(testRegistrationId);

        // 1.3 验证挂号状态
        Registration registration = registrationRepository.findById(testRegistrationId).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.PAID_REGISTRATION.getCode());

        // ========== 阶段2：就诊流程 ==========

        // 2.1 医生接诊
        registration.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.saveAndFlush(registration);

        // 2.2 创建病历
        MedicalRecord record = new MedicalRecord();
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());
        record.setRecordNo("MR" + System.currentTimeMillis());
        record.setChiefComplaint("头痛");
        record.setDiagnosis("上呼吸道感染");
        record.setStatus((short) 1);
        record.setIsDeleted((short) 0);
        medicalRecordRepository.saveAndFlush(record);

        // 2.3 开具处方
        Prescription prescription = new Prescription();
        prescription.setPatient(registration.getPatient());
        prescription.setMedicalRecord(record);
        prescription.setDoctor(registration.getDoctor());
        prescription.setPrescriptionNo("PR" + System.currentTimeMillis());
        prescription.setPrescriptionType((short) 1);
        prescription.setItemCount(1);
        prescription.setTotalAmount(new BigDecimal("25.00"));
        prescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());
        prescription.setIsDeleted((short) 0);
        prescriptionRepository.saveAndFlush(prescription);

        // 2.4 审核处方
        prescriptionService.review(prescription.getMainId(), testDoctorId, "审核通过");

        // ========== 阶段3：处方收费 ==========

        // 3.1 创建处方收费单
        Long prescriptionId = prescription.getMainId();
        CreateChargeDTO chargeDTO = new CreateChargeDTO();
        chargeDTO.setRegistrationId(testRegistrationId);
        chargeDTO.setPrescriptionIds(Arrays.asList(prescriptionId));

        Long prescChargeId = chargeService.createPrescriptionCharge(testRegistrationId, Arrays.asList(prescriptionId)).getId();

        // 3.2 支付处方费
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3);
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(new BigDecimal("25.00"));

        chargeService.processPayment(prescChargeId, paymentDTO);

        // ========== 阶段4：验证结果 ==========

        // 4.1 查询按类型分组的收费记录
        java.util.Map<String, java.util.List<ChargeVO>> chargesByType = chargeService.getChargesByType(testRegistrationId);

        assertThat(chargesByType).hasSize(3);
        assertThat(chargesByType.get("registration")).hasSize(1);
        assertThat(chargesByType.get("prescription")).hasSize(1);
        assertThat(chargesByType.get("combined")).isEmpty();

        // 4.2 验证挂号收费单
        ChargeVO regCharge = chargesByType.get("registration").get(0);
        assertThat(regCharge.getTotalAmount()).isEqualByComparingTo("10.00");
        assertThat(regCharge.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());

        // 4.3 验证处方收费单
        ChargeVO prescCharge = chargesByType.get("prescription").get(0);
        assertThat(prescCharge.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(prescCharge.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());

        // 4.4 验证最终状态
        Registration finalRegistration = registrationRepository.findById(testRegistrationId).orElseThrow();
        assertThat(finalRegistration.getStatus()).isEqualTo(RegStatusEnum.COMPLETED.getCode());

        Prescription finalPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertThat(finalPrescription.getStatus()).isEqualTo(PrescriptionStatusEnum.PAID.getCode());
    }

    @Test
    @DisplayName("集成测试4：向后兼容 - 合并收费仍可用")
    @WithMockUser(username = "cashier001", roles = {"CASHIER"})
    void testBackwardCompatibility_CombinedCharge() throws Exception {
        // Step 1: 创建挂号并完成就诊
        testRegistrationId = createAndCompleteRegistration();

        // Step 2: 开具并审核处方
        Long prescriptionId = createAndReviewPrescription(testRegistrationId);

        // Step 3: 使用旧方式创建合并收费单（挂号费+处方费）
        CreateChargeDTO chargeDTO = new CreateChargeDTO();
        chargeDTO.setRegistrationId(testRegistrationId);
        chargeDTO.setPrescriptionIds(Arrays.asList(prescriptionId));

        mockMvc.perform(post("/api/cashier/charges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(25.00)); // Only prescription fee (registration already paid)

        // Step 4: 验证收费单创建成功
        java.util.List<com.his.entity.Charge> charges = chargeRepository.findByRegistration_MainIdAndIsDeleted(testRegistrationId, (short) 0);
        assertThat(charges).hasSize(2); // One for registration, one for prescription

        // Verify we have both charge types
        java.util.List<com.his.entity.Charge> regCharges = charges.stream()
                .filter(c -> c.getChargeType() == 1)
                .toList();
        java.util.List<com.his.entity.Charge> prescCharges = charges.stream()
                .filter(c -> c.getChargeType() == 2)
                .toList();

        assertThat(regCharges).hasSize(1);
        assertThat(prescCharges).hasSize(1);
        assertThat(regCharges.get(0).getTotalAmount()).isEqualByComparingTo("10.00");
        assertThat(prescCharges.get(0).getTotalAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("集成测试5：并发场景 - 防止重复收费")
    @WithMockUser(username = "cashier001", roles = {"CASHIER"})
    void testConcurrency_PreventDuplicateCharge() throws Exception {
        // Given: 创建已支付的挂号收费单
        testRegistrationId = createRegistration();

        Long regChargeId = chargeService.createRegistrationCharge(testRegistrationId).getId();

        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3);
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        chargeService.processPayment(regChargeId, paymentDTO);

        // When & Then: 尝试再次创建挂号收费单应该失败
        assertThatThrownBy(() -> chargeService.createRegistrationCharge(testRegistrationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("挂号费已支付");
    }

    // ========== 辅助方法 ==========

    private Long createRegistration() {
        Registration reg = new Registration();
        reg.setRegNo("R" + System.currentTimeMillis());
        reg.setPatient(patientRepository.findById(testPatientId).orElseThrow());
        reg.setDepartment(departmentRepository.findById(testDepartmentId).orElseThrow());
        reg.setDoctor(doctorRepository.findById(testDoctorId).orElseThrow());
        reg.setVisitDate(LocalDate.now());
        reg.setRegistrationFee(new BigDecimal("10.00"));
        reg.setStatus(RegStatusEnum.WAITING.getCode());
        reg.setIsDeleted((short) 0);
        reg.setQueueNo("Q" + System.currentTimeMillis());
        return registrationRepository.saveAndFlush(reg).getMainId();
    }

    private Long createAndPayForRegistration() throws Exception {
        Long regId = createRegistration();
        createAndPayForRegistrationDirectly(regId);
        return regId;  // Return registration ID, not charge ID
    }

    private Long createAndPayForRegistrationDirectly(Long registrationId) {
        // 创建挂号收费单
        Long chargeId = chargeService.createRegistrationCharge(registrationId).getId();

        // 支付
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3);
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        chargeService.processPayment(chargeId, paymentDTO);

        return chargeId;
    }

    private Long createAndCompleteRegistration() {
        // Create a new registration
        Long regId = createRegistration();

        // Pay for it
        Long chargeId = chargeService.createRegistrationCharge(regId).getId();

        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 3);
        paymentDTO.setTransactionNo("WX" + System.currentTimeMillis());
        paymentDTO.setPaidAmount(new BigDecimal("10.00"));

        chargeService.processPayment(chargeId, paymentDTO);

        // Mark as completed
        Registration reg = registrationRepository.findById(regId).orElseThrow();
        reg.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.saveAndFlush(reg);

        return regId;
    }

    private Long createAndReviewPrescription(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId).orElseThrow();

        MedicalRecord record = new MedicalRecord();
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());
        record.setRecordNo("MR" + System.currentTimeMillis());
        record.setChiefComplaint("测试");
        record.setDiagnosis("测试诊断");
        record.setStatus((short) 1);
        record.setIsDeleted((short) 0);
        medicalRecordRepository.saveAndFlush(record);

        Prescription prescription = new Prescription();
        prescription.setPatient(registration.getPatient());
        prescription.setMedicalRecord(record);
        prescription.setDoctor(registration.getDoctor());
        prescription.setPrescriptionNo("PR" + System.currentTimeMillis());
        prescription.setPrescriptionType((short) 1);
        prescription.setItemCount(1);
        prescription.setTotalAmount(new BigDecimal("25.00"));
        prescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());
        prescription.setIsDeleted((short) 0);
        prescriptionRepository.saveAndFlush(prescription);

        prescriptionService.review(prescription.getMainId(), testDoctorId, "审核通过");
        return prescription.getMainId();
    }
}
