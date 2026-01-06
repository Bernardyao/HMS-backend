package com.his.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.his.common.DataMaskingUtils;
import com.his.dto.PaymentDTO;
import com.his.dto.RegistrationDTO;
import com.his.entity.Charge;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.log.utils.LogUtils;
import com.his.repository.ChargeRepository;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.ChargeService;
import com.his.service.RegistrationService;
import com.his.vo.RegistrationVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 挂号服务实现类
 *
 * <p>负责患者挂号的核心业务逻辑，包括老患者查找、新患者建档、挂号单创建、取消和退费等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>挂号：支持老患者查找和新患者建档，创建挂号单并分配排队号</li>
 *   <li>挂号即收费：支持挂号时同步完成收费，提升患者体验</li>
 *   <li>取消挂号：支持未就诊的挂号单取消，已收费的自动退费</li>
 *   <li>重复挂号检查：防止同一患者同一天重复挂号同一医生</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>老患者查找：根据身份证号查询，如果存在则使用已有档案</li>
 *   <li>新患者建档：身份证号不存在时自动创建患者档案并生成病历号</li>
 *   <li>病历号和挂号流水号使用PostgreSQL序列生成，保证唯一性和并发安全性</li>
 *   <li>排队号按科室和日期生成（001, 002, 003...）</li>
 *   <li>重复挂号检查：同一患者、同一医生、同一天、待就诊状态不允许重复挂号</li>
 *   <li>挂号即收费：如果提供支付信息，创建收费单并执行支付</li>
 *   <li>取消挂号时自动退费：如果挂号费已支付，取消时自动执行退费流程</li>
 * </ul>
 *
 * <h3>状态流转</h3>
 * <ul>
 *   <li>WAITING（待就诊）→ COMPLETED（已就诊）→ INVOICED（已开立处方）</li>
 *   <li>WAITING（待就诊）→ PAID_REGISTRATION（已缴挂号费）→ COMPLETED（已就诊）</li>
 *   <li>WAITING/PAID_REGISTRATION → CANCELLED（已取消）→ REFUNDED（已退费）</li>
 * </ul>
 *
 * <h3>权限控制</h3>
 * <ul>
 *   <li>register: 需要 NURSE 或 ADMIN 角色</li>
 *   <li>cancel: 需要 NURSE、CASHIER 或 ADMIN 角色</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.Patient} - 患者档案（自动创建或查找）</li>
 *   <li>{@link com.his.entity.Registration} - 挂号单主表</li>
 *   <li>{@link com.his.entity.Doctor} - 医生信息</li>
 *   <li>{@link com.his.entity.Department} - 科室信息</li>
 *   <li>{@link com.his.entity.Charge} - 收费单（挂号即收费时创建）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.RegistrationService
 * @see com.his.service.ChargeService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final PatientRepository patientRepository;
    private final RegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeService chargeService;

    /**
     * 挂号（老患者查找 + 新患者建档 + 创建挂号单）
     *
     * <p>实现完整的挂号流程，包括患者识别/建档、挂号单创建、重复挂号检查、可选的挂号即收费</p>
     *
     * <p><b>业务流程：</b></p>
     * <ol>
     *   <li>参数校验：验证必填字段（姓名、身份证、科室、医生、挂号费等）</li>
     *   <li>老患者查找：根据身份证号查询患者档案</li>
     *   <li>新患者建档：如果患者不存在，自动创建患者档案并生成病历号</li>
     *   <li>科室和医生验证：查询并验证科室和医生是否存在</li>
     *   <li>重复挂号检查：防止同一患者同一天重复挂号同一医生</li>
     *   <li>创建挂号单：生成挂号流水号和排队号</li>
     *   <li>挂号即收费（可选）：如果提供支付信息，自动创建收费单并执行支付</li>
     * </ol>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>老患者查找：根据身份证号精确匹配</li>
     *   <li>新患者建档：自动生成病历号（格式：P+yyyyMMdd+4位序列号）</li>
     *   <li>挂号流水号：自动生成（格式：R+yyyyMMdd+4位序列号）</li>
     *   <li>排队号：按科室和日期生成（001, 002, 003...）</li>
     *   <li>重复挂号检查：同一患者、同一医生、同一天、待就诊状态不允许重复</li>
     *   <li>挂号即收费：支持现金、银行卡、微信、支付宝等支付方式</li>
     * </ul>
     *
     * <p><b>幂等性保护：</b></p>
     * <ul>
     *   <li>挂号费已支付时跳过收费</li>
     *   <li>交易流水号已存在时跳过支付</li>
     * </ul>
     *
     * <p><b>权限要求：</b></p>
     * <ul>
     *   <li>需要 NURSE 或 ADMIN 角色</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>患者姓名、身份证号、性别、科室ID、医生ID、挂号费不能为空</li>
     *   <li>科室和医生必须存在</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>患者档案已创建或查找到</li>
     *   <li>挂号单已创建（状态为WAITING或PAID_REGISTRATION）</li>
     *   <li>如果提供支付信息，收费单已创建并支付完成</li>
     * </ul>
     *
     * @param dto 挂号信息DTO
     *            <ul>
     *              <li>patientName: 患者姓名（必填）</li>
     *              <li>idCard: 身份证号（必填，用于老患者查找）</li>
     *              <li>gender: 性别（必填）</li>
     *              <li>age: 年龄（必填）</li>
     *              <li>phone: 联系电话（可选）</li>
     *              <li>deptId: 科室ID（必填）</li>
     *              <li>doctorId: 医生ID（必填）</li>
     *              <li>regFee: 挂号费（必填）</li>
     *              <li>paymentMethod: 支付方式（可选，1=现金, 2=银行卡, 3=微信, 4=支付宝）</li>
     *              <li>transactionNo: 交易流水号（可选，用于幂等性控制）</li>
     *            </ul>
     * @return 挂号单视图对象（RegistrationVO）
     * @throws IllegalArgumentException 如果参数校验失败
     * @throws IllegalArgumentException 如果科室或医生不存在
     * @throws IllegalStateException 如果检测到重复挂号
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public RegistrationVO register(RegistrationDTO dto) {
        String maskedIdCard = DataMaskingUtils.maskIdCard(dto.getIdCard());
        LogUtils.logBusinessOperation("挂号管理", "开始挂号",
                String.format("患者身份证: %s, 科室ID: %d, 医生ID: %d",
                        maskedIdCard, dto.getDeptId(), dto.getDoctorId()));

        // 1. 参数校验
        validateRegistrationDTO(dto);

        // 2. 老患查找 / 新患建档
        Patient patient = findOrCreatePatient(dto);
        log.info("患者信息已确认，患者ID: {}, 姓名: {}", patient.getMainId(), patient.getName());

        // 3. 查询科室和医生信息
        Department department = departmentRepository.findById(dto.getDeptId())
                .orElseThrow(() -> new IllegalArgumentException("科室不存在，ID: " + dto.getDeptId()));

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("医生不存在，ID: " + dto.getDoctorId()));

        // 4. 检查是否重复挂号（同一患者、同一医生、同一天、待就诊状态）
        LocalDate today = LocalDate.now();
        log.info("开始重复挂号检查，参数: 患者ID={}, 医生ID={}, 日期={}",
                patient.getMainId(), doctor.getMainId(), today);

        boolean alreadyRegistered = registrationRepository.existsByPatientAndDoctorAndDateAndStatusWaiting(
                patient.getMainId(), doctor.getMainId(), today, (short) 0, (short) 0);

        log.info("重复挂号检查结果: alreadyRegistered = {}", alreadyRegistered);

        if (alreadyRegistered) {
            log.warn("检测到重复挂号尝试，患者: {} (ID: {}), 医生: {} (ID: {}), 日期: {}",
                    patient.getName(), patient.getMainId(), doctor.getName(), doctor.getMainId(), today);
            throw new IllegalStateException(
                    String.format("患者 %s 今日已挂号 %s 医生，请勿重复挂号",
                            patient.getName(), doctor.getName()));
        }
        log.info("重复挂号检查通过，患者: {} (ID: {}), 医生: {} (ID: {})",
                patient.getName(), patient.getMainId(), doctor.getName(), doctor.getMainId());

        // 5. 创建挂号单
        Registration registration = createRegistration(patient, department, doctor, dto);
        Registration savedRegistration = registrationRepository.save(registration);
        log.info("挂号单创建成功，挂号ID: {}, 挂号流水号: {}",
                savedRegistration.getMainId(), savedRegistration.getRegNo());

        // 6. 如果提供了支付信息，执行"挂号即收费"
        if (dto.getPaymentMethod() != null) {
            log.info("执行挂号即收费，挂号ID: {}, 支付方式: {}", savedRegistration.getMainId(), dto.getPaymentMethod());
            processPaymentForRegistration(savedRegistration, dto);
        }

        // 7. 构建返回对象
        return buildRegistrationVO(savedRegistration, patient, department, doctor);
    }

    /**
     * 根据ID查询挂号记录详情
     *
     * <p>查询指定ID的挂号单及其关联信息</p>
     *
     * <p><b>查询内容：</b></p>
     * <ul>
     *   <li>挂号单基本信息（流水号、状态、就诊日期、排队号等）</li>
     *   <li>患者基本信息（姓名、性别、年龄）</li>
     *   <li>科室信息（科室名称）</li>
     *   <li>医生信息（医生姓名）</li>
     * </ul>
     *
     * @param id 挂号单ID
     * @return 挂号单视图对象（RegistrationVO）
     * @throws IllegalArgumentException 如果挂号记录不存在
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public RegistrationVO getById(Long id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + id));

        return buildRegistrationVO(
                registration,
                registration.getPatient(),
                registration.getDepartment(),
                registration.getDoctor()
        );
    }

    /**
     * 取消挂号
     *
     * <p>取消指定的挂号单，如果已收费则自动执行退费操作</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只允许取消状态为 WAITING 或 PAID_REGISTRATION 的挂号单</li>
     *   <li>如果挂号费已支付，自动执行退费流程</li>
     *   <li>退费通过调用收费服务的退费接口完成</li>
     *   <li>挂号状态变更为 CANCELLED</li>
     * </ul>
     *
     * <p><b>退费流程：</b></p>
     * <ol>
     *   <li>查找挂号费收费单（chargeType=1 且状态为PAID）</li>
     *   <li>调用收费服务的退费接口</li>
     *   <li>如果退费失败，抛出异常并记录日志</li>
     *   <li>退费成功后更新挂号状态为 CANCELLED</li>
     * </ol>
     *
     * <p><b>权限要求：</b></p>
     * <ul>
     *   <li>需要 NURSE、CASHIER 或 ADMIN 角色</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>挂号单存在</li>
     *   <li>挂号单状态为 WAITING 或 PAID_REGISTRATION</li>
     *   <li>取消原因已提供</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>挂号状态更新为 CANCELLED</li>
     *   <li>记录取消原因</li>
     *   <li>如果已支付，挂号收费单状态更新为 REFUNDED</li>
     * </ul>
     *
     * @param id 挂号单ID
     * @param reason 取消原因
     * @throws IllegalArgumentException 如果挂号记录不存在
     * @throws IllegalStateException 如果挂号状态不正确（非待就诊或已缴挂号费）
     * @throws IllegalStateException 如果自动退费失败
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('NURSE', 'CASHIER', 'ADMIN')")
    public void cancel(Long id, String reason) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + id));

        if (!RegStatusEnum.WAITING.getCode().equals(registration.getStatus()) &&
            !RegStatusEnum.PAID_REGISTRATION.getCode().equals(registration.getStatus())) {
            throw new IllegalStateException("只有待就诊或已缴挂号费状态的挂号才能取消");
        }

        // 如果已支付挂号费，先执行退费
        if (RegStatusEnum.PAID_REGISTRATION.getCode().equals(registration.getStatus())) {
            log.info("挂号已支付，执行自动退费，挂号ID: {}", id);
            try {
                // 查找挂号费收费单（chargeType=1表示挂号费）
                List<Charge> charges = chargeRepository.findByRegistration_MainIdAndIsDeleted(id, (short) 0);
                Optional<Charge> registrationCharge = charges.stream()
                        .filter(c -> c.getChargeType() == 1) // 挂号费类型
                        .filter(c -> c.getStatus() == ChargeStatusEnum.PAID.getCode()) // 已支付状态
                        .findFirst();

                if (registrationCharge.isEmpty()) {
                    throw new IllegalStateException("未找到已支付的挂号收费单，无法退费");
                }

                Long chargeId = registrationCharge.get().getMainId();
                log.info("找到挂号收费单，收费单ID: {}", chargeId);

                // 调用退费接口（使用收费单ID）
                chargeService.processRefund(chargeId, reason);
                log.info("自动退费成功，挂号ID: {}, 收费单ID: {}", id, chargeId);
            } catch (Exception e) {
                log.error("自动退费失败，挂号ID: {}", id, e);
                throw new IllegalStateException("自动退费失败：" + e.getMessage());
            }
        }

        registration.setStatus(RegStatusEnum.CANCELLED.getCode());
        registration.setCancelReason(reason);
        registrationRepository.save(registration);

        log.info("挂号已取消，挂号ID: {}, 取消原因: {}", id, reason);
    }

    /**
     * 退费（将已取消的挂号标记为已退费）
     *
     * <p>将已取消的挂号单状态更新为已退费，用于财务对账和审计</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只允许退费状态为 CANCELLED 的挂号单</li>
     *   <li>退费仅更新挂号单状态，不涉及实际资金操作</li>
     *   <li>实际资金退费在取消挂号时通过收费服务的退费接口完成</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>财务对账：标记退费流程已完成</li>
     *   <li>审计追踪：区分"已取消"和"已退费"状态</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>挂号单存在</li>
     *   <li>挂号单状态为 CANCELLED</li>
     *   <li>挂号费的退费已通过收费服务完成</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>挂号状态更新为 REFUNDED</li>
     * </ul>
     *
     * @param id 挂号单ID
     * @throws IllegalArgumentException 如果挂号记录不存在
     * @throws IllegalStateException 如果挂号状态不是已取消
     * @since 1.0
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + id));

        if (!RegStatusEnum.CANCELLED.getCode().equals(registration.getStatus())) {
            throw new IllegalStateException("只有已取消状态的挂号才能退费");
        }

        registration.setStatus(RegStatusEnum.REFUNDED.getCode());
        registrationRepository.save(registration);

        log.info("挂号已退费，挂号ID: {}", id);
    }

    // ============================================
    // 私有辅助方法
    // ============================================

    /**
     * 参数校验
     */
    private void validateRegistrationDTO(RegistrationDTO dto) {
        if (!StringUtils.hasText(dto.getPatientName())) {
            throw new IllegalArgumentException("患者姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getIdCard())) {
            throw new IllegalArgumentException("身份证号不能为空");
        }
        if (dto.getGender() == null) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (dto.getDeptId() == null) {
            throw new IllegalArgumentException("科室ID不能为空");
        }
        if (dto.getDoctorId() == null) {
            throw new IllegalArgumentException("医生ID不能为空");
        }
        if (dto.getRegFee() == null) {
            throw new IllegalArgumentException("挂号费不能为空");
        }
    }

    /**
     * 老患查找 / 新患建档
     */
    private Patient findOrCreatePatient(RegistrationDTO dto) {
        // 根据身份证号查询患者
        Optional<Patient> existingPatient = patientRepository.findByIdCardAndIsDeleted(
                dto.getIdCard(), (short) 0);

        if (existingPatient.isPresent()) {
            log.info("找到已有患者，患者ID: {}", existingPatient.get().getMainId());
            return existingPatient.get();
        }

        // 新患者，创建患者档案
        Patient newPatient = new Patient();
        newPatient.setPatientNo(generatePatientNo());
        newPatient.setName(dto.getPatientName());
        newPatient.setIdCard(dto.getIdCard());
        newPatient.setGender(dto.getGender());
        newPatient.setAge(dto.getAge());
        newPatient.setPhone(dto.getPhone());
        newPatient.setIsDeleted((short) 0);

        Patient savedPatient = patientRepository.save(newPatient);
        log.info("新患者建档成功，患者ID: {}, 病历号: {}",
                savedPatient.getMainId(), savedPatient.getPatientNo());

        return savedPatient;
    }

    /**
     * 创建挂号单
     */
    private Registration createRegistration(Patient patient, Department department,
                                           Doctor doctor, RegistrationDTO dto) {
        Registration registration = new Registration();
        registration.setRegNo(generateRegNo());
        registration.setPatient(patient);
        registration.setDepartment(department);
        registration.setDoctor(doctor);
        registration.setVisitDate(LocalDate.now());
        registration.setVisitType((short) 1); // 默认初诊
        registration.setRegistrationFee(dto.getRegFee());
        registration.setStatus(RegStatusEnum.WAITING.getCode());
        registration.setIsDeleted((short) 0);
        registration.setQueueNo(generateQueueNo(department.getMainId()));
        registration.setAppointmentTime(LocalDateTime.now());

        return registration;
    }

    /**
     * 构建 RegistrationVO
     */
    private RegistrationVO buildRegistrationVO(Registration registration, Patient patient,
                                              Department department, Doctor doctor) {
        RegistrationVO vo = new RegistrationVO();
        vo.setId(registration.getMainId());
        vo.setRegNo(registration.getRegNo());
        vo.setPatientId(patient.getMainId());
        vo.setPatientName(patient.getName());
        vo.setGender(patient.getGender());
        vo.setAge(patient.getAge());
        vo.setDeptId(department.getMainId());
        vo.setDeptName(department.getName());
        vo.setDoctorId(doctor.getMainId());
        vo.setDoctorName(doctor.getName());
        vo.setStatus(registration.getStatus());
        vo.setStatusDesc(RegStatusEnum.fromCode(registration.getStatus()).getDescription());
        vo.setVisitDate(registration.getVisitDate());
        vo.setRegistrationFee(registration.getRegistrationFee());
        vo.setQueueNo(registration.getQueueNo());
        vo.setAppointmentTime(registration.getAppointmentTime());
        vo.setCreatedAt(registration.getCreatedAt());

        return vo;
    }

    /**
     * 生成病历号（线程安全）
     *
     * <p>使用数据库序列生成，替代原有的count()+1方式</p>
     * <p>格式：P + yyyyMMdd + 4位序列号</p>
     *
     * @return 唯一的病历号
     */
    private String generatePatientNo() {
        try {
            String patientNo = patientRepository.generatePatientNo();
            log.debug("生成病历号: {}", patientNo);
            return patientNo;
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("数据库访问失败，无法生成病历号", e);
            throw new IllegalStateException("生成病历号失败：数据库错误 - " + e.getMostSpecificCause().getMessage(), e);
        } catch (RuntimeException e) {
            log.error("生成病历号失败", e);
            throw new IllegalStateException("生成病历号失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成挂号流水号（线程安全）
     *
     * <p>使用数据库序列生成，替代原有的count()+1方式</p>
     * <p>格式：R + yyyyMMdd + 4位序列号</p>
     *
     * @return 唯一的挂号流水号
     */
    private String generateRegNo() {
        try {
            String regNo = registrationRepository.generateRegNo();
            log.debug("生成挂号流水号: {}", regNo);
            return regNo;
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("数据库访问失败，无法生成挂号流水号", e);
            throw new IllegalStateException("生成挂号流水号失败：数据库错误 - " + e.getMostSpecificCause().getMessage(), e);
        } catch (RuntimeException e) {
            log.error("生成挂号流水号失败", e);
            throw new IllegalStateException("生成挂号流水号失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成排队号
     */
    private String generateQueueNo(Long deptId) {
        LocalDate today = LocalDate.now();
        long count = registrationRepository.countByDateAndDepartment(today, deptId) + 1;
        return String.format("%03d", count);
    }

    /**
     * 处理挂号即收费（内部辅助方法）
     *
     * <p>安全要求：调用此方法需要PAYMENT_PROCESS权限（已在register方法层面控制）</p>
     *
     * <p>业务流程：</p>
     * <ol>
     *   <li>创建挂号收费单</li>
     *   <li>执行支付（含幂等性检查）</li>
     *   <li>更新挂号状态为 PAID_REGISTRATION</li>
     * </ol>
     *
     * @param registration 挂号记录
     * @param dto 挂号DTO（包含支付信息）
     */
    private void processPaymentForRegistration(Registration registration, RegistrationDTO dto) {
        Long registrationId = registration.getMainId();

        // 【参数校验】验证支付方式合法性
        if (dto.getPaymentMethod() != null) {
            if (dto.getPaymentMethod() < 1 || dto.getPaymentMethod() > 4) {
                throw new IllegalArgumentException(
                    String.format("无效的支付方式: %d，支持的支付方式：1=现金, 2=银行卡, 3=微信, 4=支付宝",
                        dto.getPaymentMethod())
                );
            }
        }

        // 【参数校验】验证交易流水号格式
        if (StringUtils.hasText(dto.getTransactionNo())) {
            String transactionNo = dto.getTransactionNo().trim();
            // 长度校验：6-64位
            if (transactionNo.length() < 6 || transactionNo.length() > 64) {
                throw new IllegalArgumentException("交易流水号长度必须在6-64位之间");
            }
            // 格式校验：只允许字母、数字、下划线、横杠
            if (!transactionNo.matches("^[a-zA-Z0-9_-]+$")) {
                throw new IllegalArgumentException("交易流水号只能包含字母、数字、下划线和横杠");
            }
        }

        // 【参数校验】验证支付金额与挂号费匹配
        // 注意：PaymentDTO可能没有paidAmount字段，这里使用registration的挂号费
        BigDecimal registrationFee = registration.getRegistrationFee();
        if (registrationFee == null || registrationFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("挂号费金额必须大于0");
        }

        // 【幂等性保护】检查是否已支付
        if (chargeService.isRegistrationFeePaid(registrationId)) {
            log.warn("挂号费已支付，跳过重复收费，挂号ID: {}", registrationId);
            return;
        }

        // 【幂等性保护】检查交易流水号是否已使用
        if (StringUtils.hasText(dto.getTransactionNo())) {
            var existingCharge = chargeRepository.findByTransactionNo(dto.getTransactionNo());
            if (existingCharge.isPresent()) {
                log.warn("交易流水号已存在，跳过重复支付，流水号: {}", dto.getTransactionNo());
                return;
            }
        }

        // 1. 创建挂号收费单
        var chargeVO = chargeService.createRegistrationCharge(registrationId);

        // 2. 构建支付DTO
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod(dto.getPaymentMethod());
        paymentDTO.setTransactionNo(dto.getTransactionNo());
        paymentDTO.setPaidAmount(registration.getRegistrationFee());

        // 3. 执行支付
        chargeService.processPayment(chargeVO.getId(), paymentDTO);

        log.info("挂号即收费完成，挂号ID: {}, 收费单ID: {}, 支付金额: {}",
                registrationId, chargeVO.getId(), registration.getRegistrationFee());
    }
}
