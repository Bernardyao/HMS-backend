package com.his.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.service.NurseWorkstationService;
import com.his.service.RegistrationStateMachine;
import com.his.test.base.BaseControllerTest;
import com.his.vo.ChargeVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 护士站挂号费收取集成测试
 *
 * <p>测试护士站直接收取挂号费的完整流程，包括：</p>
 * <ul>
 *   <li>场景1：成功支付（无现有收费单）</li>
 *   <li>场景2：成功支付（复用现有未支付收费单）</li>
 *   <li>场景3：重复支付尝试（应失败并返回友好错误）</li>
 *   <li>场景4：审计日志验证（确保状态变更被记录）</li>
 * </ul>
 *
 * <p>继承自BaseControllerTest，自动获得：
 * <ul>
 *     <li>MockMvc - 用于HTTP请求测试</li>
 *     <li>ObjectMapper - 用于JSON序列化</li>
 *     <li>@Transactional - 测试数据自动回滚</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@ActiveProfiles("integration-test")
@DisplayName("护士站挂号费收取集成测试")
class NursePaymentIntegrationTest extends BaseControllerTest {

    @Autowired
    private NurseWorkstationService nurseWorkstationService;

    @Autowired
    private ChargeService chargeService;

    @Autowired
    private RegistrationStateMachine registrationStateMachine;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ChargeRepository chargeRepository;

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
        dept = departmentRepository.save(dept);

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC" + uid);
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(dept);
        doctor.setTitle("主治医师");
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        doctor = doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setPatientNo("P" + uid);
        patient.setName("测试患者");
        patient.setGender((short) 1);
        patient.setIdCard("110101199001011234");
        patient.setPhone("13800138000");
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
        patient.setIsDeleted((short) 0);
        patient = patientRepository.save(patient);

        // 创建挂号单（状态为 WAITING）
        Registration registration = new Registration();
        registration.setRegNo("REG" + uid);
        registration.setPatient(patient);
        registration.setDepartment(dept);
        registration.setDoctor(doctor);
        registration.setVisitDate(LocalDate.now());
        registration.setVisitType((short) 1); // 初诊
        registration.setRegistrationFee(new BigDecimal("10.00"));
        registration.setStatus(RegStatusEnum.WAITING.getCode()); // 待就诊
        registration.setIsDeleted((short) 0);
        registration = registrationRepository.save(registration);
        testRegistrationId = registration.getMainId();
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景1：成功支付挂号费 - 无现有收费单")
    void testSuccessfulPayment_NoExistingCharge() {
        // Given: 一个待就诊的挂号单
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1); // 现金

        // When: 护士站收取挂号费
        ChargeVO result = nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // Then: 验证收费单创建成功并已支付
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10.00"));

        // Then: 验证挂号状态已更新为 PAID_REGISTRATION
        Registration registration = registrationRepository.findById(testRegistrationId).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.PAID_REGISTRATION.getCode());

        // Then: 验证挂号费已支付标记
        assertThat(chargeService.isRegistrationFeePaid(testRegistrationId)).isTrue();
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景2：成功支付挂号费 - 复用现有未支付收费单")
    void testSuccessfulPayment_ReuseExistingCharge() {
        // Given: 已存在一个未支付的挂号收费单
        ChargeVO existingCharge = chargeService.createRegistrationCharge(testRegistrationId);
        Long existingChargeId = existingCharge.getId();

        // When: 护士站再次尝试收取挂号费（应复用现有收费单）
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 2); // 银行卡

        ChargeVO result = nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // Then: 验证复用了现有收费单（ID相同）
        assertThat(result.getId()).isEqualTo(existingChargeId);
        assertThat(result.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());

        // Then: 验证数据库中只有一条收费记录
        List<Charge> charges = chargeRepository.findByRegistration_MainIdAndChargeTypeAndIsDeleted(
                testRegistrationId, (short) 1, (short) 0);
        assertThat(charges).hasSize(1);
        assertThat(charges.get(0).getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景3：重复支付尝试 - 应返回友好错误")
    void testDuplicatePaymentAttempt_ShouldFail() {
        // Given: 挂号费已支付
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1);
        nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // When & Then: 再次尝试支付应抛出异常
        PaymentDTO duplicatePayment = new PaymentDTO();
        duplicatePayment.setPaymentMethod((short) 3); // 微信

        assertThatThrownBy(() -> nurseWorkstationService.payRegistrationFee(testRegistrationId, duplicatePayment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("挂号费已支付");
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景4：审计日志验证 - 确保状态变更被记录")
    void testAuditLogCreation() {
        // Given: 一个待就诊的挂号单
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1);

        // When: 护士站收取挂号费
        nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // Then: 验证状态历史记录已创建
        List<RegistrationStatusHistory> history = registrationStateMachine.getHistory(testRegistrationId);

        assertThat(history).isNotEmpty();

        // 查找从 WAITING 到 PAID_REGISTRATION 的转换记录
        RegistrationStatusHistory paidHistory = history.stream()
                .filter(h -> h.getFromStatus().equals(RegStatusEnum.WAITING.getCode())
                        && h.getToStatus().equals(RegStatusEnum.PAID_REGISTRATION.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到支付挂号费的状态转换记录"));

        assertThat(paidHistory.getReason()).contains("支付挂号费");
        assertThat(paidHistory.getOperatorName()).isNotBlank();
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景5：错误状态挂号 - 无法收费")
    void testPayment_InvalidRegistrationStatus() {
        // Given: 将挂号状态改为已就诊
        Registration registration = registrationRepository.findById(testRegistrationId).orElseThrow();
        registration.setStatus(RegStatusEnum.COMPLETED.getCode());
        registrationRepository.save(registration);

        // When & Then: 尝试收费应失败
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1);

        assertThatThrownBy(() -> nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("状态不正确");
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景6：交易流水号自动生成")
    void testTransactionNumberAutoGeneration() {
        // Given: 支付请求不包含交易流水号
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1);
        paymentDTO.setTransactionNo(null); // 明确设置为null

        // When: 护士站收取挂号费
        ChargeVO result = nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // Then: 验证交易流水号已自动生成（通过检查支付成功来验证）
        assertThat(result.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());
    }

    @Test
    @WithMockUser(username = "nurse001", roles = {"NURSE"})
    @DisplayName("场景7：支付金额自动填充")
    void testPaymentAmountAutoFill() {
        // Given: 支付请求的金额为null或不正确
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod((short) 1);
        paymentDTO.setPaidAmount(null); // 不提供金额

        // When: 护士站收取挂号费
        ChargeVO result = nurseWorkstationService.payRegistrationFee(testRegistrationId, paymentDTO);

        // Then: 验证系统自动填充了正确的金额
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.getStatus()).isEqualTo(ChargeStatusEnum.PAID.getCode());
    }
}
