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
import com.his.dto.PrescriptionDTO;
import com.his.entity.*;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.PrescriptionService;
import com.his.test.base.BaseControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 收费模块完整工作流集成测试
 *
 * <p>继承自BaseControllerTest，自动获得MockMvc、ObjectMapper和事务管理</p>
 */
@ActiveProfiles("integration-test")  // 覆盖父类的 "test" profile
@DisplayName("收费模块完整工作流集成测试")
class CashierWorkflowIntegrationTest extends BaseControllerTest {
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

    private Long testRegId;
    private Long testPrescriptionId;
    private Long testMedId;

    @BeforeEach
    protected void setUp() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // 1. 基础数据
        Department dept = new Department();
        dept.setDeptCode("D" + uid);
        dept.setName("内科");
        dept.setStatus((short) 1);
        dept.setIsDeleted((short) 0);
        dept = departmentRepository.saveAndFlush(dept);

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC" + uid);
        doctor.setName("王医生");
        doctor.setGender((short) 1);  // 1=男
        doctor.setDepartment(dept);
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        doctor = doctorRepository.saveAndFlush(doctor);

        Patient patient = new Patient();
        patient.setPatientNo("P" + uid);
        patient.setName("张三");
        patient.setGender((short) 1);
        patient.setIdCard("ID" + uid + "XXXX");
        patient.setIsDeleted((short) 0);
        patient = patientRepository.saveAndFlush(patient);

        // 2. 挂号 (已完成)
        Registration reg = new Registration();
        reg.setRegNo("R" + uid);
        reg.setPatient(patient);
        reg.setDepartment(dept);
        reg.setDoctor(doctor);
        reg.setVisitDate(LocalDate.now());
        reg.setRegistrationFee(new BigDecimal("10.00"));
        reg.setStatus(RegStatusEnum.COMPLETED.getCode());
        reg.setIsDeleted((short) 0);
        reg.setQueueNo("Q" + uid);
        reg = registrationRepository.saveAndFlush(reg);
        testRegId = reg.getMainId();

        // 3. 病历
        MedicalRecord record = new MedicalRecord();
        record.setRegistration(reg);
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setRecordNo("REC" + uid);
        record.setChiefComplaint("头痛");
        record.setDiagnosis("感冒");
        record.setStatus((short) 1);
        record.setIsDeleted((short) 0);
        record = medicalRecordRepository.saveAndFlush(record);

        // 4. 药品
        Medicine med = new Medicine();
        med.setMedicineCode("M" + uid);
        med.setName("板蓝根");
        med.setUnit("袋");
        med.setRetailPrice(new BigDecimal("15.00"));
        med.setStockQuantity(100);
        med.setStatus((short) 1);
        med.setIsDeleted((short) 0);
        med = medicineRepository.saveAndFlush(med);
        testMedId = med.getMainId();

        // 5. 处方 (模拟医生开方)
        PrescriptionDTO pDto = new PrescriptionDTO();
        pDto.setRegistrationId(testRegId);
        PrescriptionDTO.PrescriptionItemDTO itemDTO = new PrescriptionDTO.PrescriptionItemDTO();
        itemDTO.setMedicineId(testMedId);
        itemDTO.setQuantity(2);
        pDto.setItems(Arrays.asList(itemDTO));

        Prescription p = prescriptionService.createPrescription(pDto);
        testPrescriptionId = p.getMainId();

        // 6. 审核 (模拟药师审核)
        prescriptionService.review(testPrescriptionId, 1L, "OK");
    }

    @Test
    @DisplayName("完整流程：收费 -> 支付 -> 发药 -> 退费(退药)")
    @WithMockUser(roles = "CASHIER")
    void testCompleteWorkflow() throws Exception {
        // Step 1: 创建收费单
        CreateChargeDTO createDto = new CreateChargeDTO();
        createDto.setRegistrationId(testRegId);
        createDto.setPrescriptionIds(Arrays.asList(testPrescriptionId));

        String createResponse = mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(40.00))
                .andReturn().getResponse().getContentAsString();

        Long chargeId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // Step 2: 支付
        PaymentDTO payDto = new PaymentDTO();
        payDto.setPaymentMethod((short) 3);
        payDto.setTransactionNo("TXN_" + UUID.randomUUID().toString().substring(0, 8));
        payDto.setPaidAmount(new BigDecimal("40.00"));

        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        // 验证：支付后处方状态应更新为 PAID(5)
        Prescription prescriptionAfterPay = prescriptionRepository.findById(testPrescriptionId).get();
        assertThat(prescriptionAfterPay.getStatus()).isEqualTo(PrescriptionStatusEnum.PAID.getCode());
        assertThat(prescriptionAfterPay.getStatus()).as("支付后处方状态应为PAID(5)").isEqualTo((short) 5);

        // Step 3: 发药 (直接调用Service，不经过Controller)
        prescriptionService.dispense(testPrescriptionId, 1L);

        // 验证：发药后库存应减少 (100 -> 98)
        Medicine medAfterDispense = medicineRepository.findById(testMedId).get();
        assertThat(medAfterDispense.getStockQuantity()).isEqualTo(98);

        // 验证：发药后处方状态应为 DISPENSED(3)
        Prescription prescriptionAfterDispense = prescriptionRepository.findById(testPrescriptionId).get();
        assertThat(prescriptionAfterDispense.getStatus()).isEqualTo(PrescriptionStatusEnum.DISPENSED.getCode());

        // Step 4: 退费
        com.his.controller.ChargeController.RefundRequest refundReq = new com.his.controller.ChargeController.RefundRequest();
        refundReq.setRefundReason("患者不想要了");

        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2));

        // 验证库存恢复
        Medicine medRefunded = medicineRepository.findById(testMedId).get();
        assertThat(medRefunded.getStockQuantity()).isEqualTo(100);

        // 验证：退费后处方状态应为 REFUNDED(4)
        Prescription prescriptionAfterRefund = prescriptionRepository.findById(testPrescriptionId).get();
        assertThat(prescriptionAfterRefund.getStatus()).isEqualTo(PrescriptionStatusEnum.REFUNDED.getCode());
    }

    @Test
    @DisplayName("异常场景：支付金额不匹配")
    @WithMockUser(roles = "CASHIER")
    void testPaymentAmountMismatch() throws Exception {
        // Given: 创建收费单
        CreateChargeDTO createDto = new CreateChargeDTO();
        createDto.setRegistrationId(testRegId);
        createDto.setPrescriptionIds(Arrays.asList(testPrescriptionId));

        String createResponse = mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long chargeId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: 支付金额不匹配（40.00 vs 50.00）
        PaymentDTO payDto = new PaymentDTO();
        payDto.setPaymentMethod((short) 3);
        payDto.setTransactionNo("TXN_MISMATCH");
        payDto.setPaidAmount(new BigDecimal("50.00")); // 错误金额

        // Then: 应返回错误
        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("异常场景：重复支付（幂等性测试）")
    @WithMockUser(roles = "CASHIER")
    void testDuplicatePayment() throws Exception {
        // Given: 创建收费单并完成第一次支付
        CreateChargeDTO createDto = new CreateChargeDTO();
        createDto.setRegistrationId(testRegId);
        createDto.setPrescriptionIds(Arrays.asList(testPrescriptionId));

        String createResponse = mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long chargeId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        String txnNo = "TXN_IDEMPOTENT_" + UUID.randomUUID().toString().substring(0, 8);

        // 第一次支付
        PaymentDTO payDto = new PaymentDTO();
        payDto.setPaymentMethod((short) 3);
        payDto.setTransactionNo(txnNo);
        payDto.setPaidAmount(new BigDecimal("40.00"));

        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        // When: 使用相同的交易流水号再次支付
        PaymentDTO payDto2 = new PaymentDTO();
        payDto2.setPaymentMethod((short) 3);
        payDto2.setTransactionNo(txnNo); // 相同的交易流水号
        payDto2.setPaidAmount(new BigDecimal("40.00"));

        // Then: 应返回成功（幂等），但不应重复处理
        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payDto2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @DisplayName("边界条件：只包含挂号费，不包含处方")
    @WithMockUser(roles = "CASHIER")
    void testCreateChargeWithRegistrationOnly() throws Exception {
        // Given: 创建一个WAITING状态的挂号单（用于仅挂号收费场景）
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Registration regWaiting = new Registration();
        regWaiting.setRegNo("RW" + uid);
        regWaiting.setPatient(patientRepository.findById(
            registrationRepository.findById(testRegId).orElseThrow().getPatient().getMainId()
        ).orElseThrow());
        regWaiting.setDepartment(departmentRepository.findById(
            registrationRepository.findById(testRegId).orElseThrow().getDepartment().getMainId()
        ).orElseThrow());
        regWaiting.setDoctor(doctorRepository.findById(
            registrationRepository.findById(testRegId).orElseThrow().getDoctor().getMainId()
        ).orElseThrow());
        regWaiting.setVisitDate(LocalDate.now());
        regWaiting.setRegistrationFee(new BigDecimal("10.00"));
        regWaiting.setStatus(RegStatusEnum.WAITING.getCode()); // 待就诊状态
        regWaiting.setIsDeleted((short) 0);
        regWaiting.setQueueNo("QW" + uid);
        Long waitingRegId = registrationRepository.saveAndFlush(regWaiting).getMainId();

        // When: 创建收费单，只包含挂号费，不包含处方
        CreateChargeDTO createDto = new CreateChargeDTO();
        createDto.setRegistrationId(waitingRegId);
        createDto.setPrescriptionIds(null); // 无处方

        // Then: 应成功创建，金额仅为挂号费
        mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(10.00)); // 只有挂号费
    }

    @Test
    @DisplayName("异常场景：退费未支付的收费单")
    @WithMockUser(roles = "CASHIER")
    void testRefundUnpaidCharge() throws Exception {
        // Given: 创建收费单但不支付
        CreateChargeDTO createDto = new CreateChargeDTO();
        createDto.setRegistrationId(testRegId);
        createDto.setPrescriptionIds(Arrays.asList(testPrescriptionId));

        String createResponse = mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long chargeId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: 尝试退费未支付的收费单
        com.his.controller.ChargeController.RefundRequest refundReq = new com.his.controller.ChargeController.RefundRequest();
        refundReq.setRefundReason("测试退费");

        // Then: 应返回400错误（业务异常：未支付不能退费）
        mockMvc.perform(post("/api/cashier/charges/" + chargeId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isBadRequest()); // 业务异常返回400
    }
}
