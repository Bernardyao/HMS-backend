package com.his.service.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.his.common.CommonConstants;
import com.his.converter.VoConverter;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.GenderEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.DoctorService;
import com.his.service.RegistrationStateMachine;
import com.his.vo.PatientDetailVO;
import com.his.vo.RegistrationVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 医生工作站服务实现类
 *
 * <p>为医生工作站提供核心业务功能，包括候诊管理、接诊流程、患者信息查询等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>候诊列表查询：支持个人视图（仅自己的患者）和科室视图（整个科室的患者）</li>
 *   <li>接诊管理：接诊、完成就诊等状态管理</li>
 *   <li>患者信息查询：查询患者详细信息和历史记录</li>
 *   <li>数据脱敏：自动对患者敏感信息（身份证、手机号）进行脱敏处理</li>
 *   <li>权限验证：防止水平越权（IDOR）攻击，确保医生只能操作自己的挂号</li>
 * </ul>
 *
 * <h3>安全特性</h3>
 * <ul>
 *   <li>IDOR防御：验证医生身份，防止医生操作其他医生的挂号记录</li>
 *   <li>数据脱敏：身份证号保留前3位和后4位，手机号保留前3位和后4位</li>
 *   <li>防御性编程：完整的参数验证和业务状态检查</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>候诊列表显示今日所有活跃状态的挂号：WAITING（待就诊）、PAID_REGISTRATION（已缴挂号费）、IN_CONSULTATION（就诊中）</li>
 *   <li>按排队号升序排列（queueNo ASC）</li>
 *   <li>接诊时只允许从 WAITING 或 PAID_REGISTRATION 状态更新为 COMPLETED 状态</li>
 *   <li>只能操作今日的挂号记录</li>
 *   <li>医生只能查询和操作自己的挂号记录（权限验证）</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.Doctor} - 医生信息</li>
 *   <li>{@link com.his.entity.Registration} - 挂号单</li>
 *   <li>{@link com.his.entity.Patient} - 患者信息</li>
 *   <li>{@link com.his.entity.Department} - 科室信息</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 * @see com.his.service.DoctorService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final RegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RegistrationStateMachine registrationStateMachine;

    /**
     * 获取今日候诊列表（支持个人/科室混合视图）
     *
     * <p>根据视图模式（个人视图或科室视图）查询候诊列表</p>
     *
     * <p><b>视图模式：</b></p>
     * <ul>
     *   <li>个人视图（showAllDept=false）：仅返回分配给当前医生的候诊患者</li>
     *   <li>科室视图（showAllDept=true）：返回整个科室的所有候诊患者</li>
     * </ul>
     *
     * <p><b>查询规则：</b></p>
     * <ul>
     *   <li>仅查询今日挂号记录</li>
     *   <li>查询活跃患者状态：WAITING（待就诊）、PAID_REGISTRATION（已缴挂号费）、IN_CONSULTATION（就诊中）</li>
     *   <li>按状态码升序、排队号升序排列</li>
     *   <li>自动过滤已删除的挂号记录</li>
     * </ul>
     *
     * <p><b>防御性编程：</b></p>
     * <ul>
     *   <li>个人视图：验证医生ID存在且大于0</li>
     *   <li>科室视图：验证科室ID存在且大于0，科室未被删除和停用</li>
     * </ul>
     *
     * @param doctorId 医生ID（个人视图时使用，必填）
     * @param deptId 科室ID（科室视图时使用，必填；个人视图时可选）
     * @param showAllDept 是否显示科室所有患者（true=科室视图，false=个人视图）
     * @return 候诊列表（RegistrationVO），按排队号升序排列
     * @throws IllegalArgumentException 当个人视图模式下医生ID为空或无效
     * @throws IllegalArgumentException 当科室视图模式下科室ID为空或无效
     * @throws IllegalArgumentException 当科室不存在、已被删除或已停用
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<RegistrationVO> getWaitingList(Long doctorId, Long deptId, boolean showAllDept) {
        log.info("查询候诊列表，医生ID: {}, 科室ID: {}, 科室视图: {}, 日期: {}",
                doctorId, deptId, showAllDept, LocalDate.now());

        // 防御性编程1: 根据视图模式验证必需参数
        if (showAllDept) {
            // 科室视图：必须提供科室ID
            if (deptId == null) {
                log.error("查询候诊列表失败（科室视图）: 科室ID为null");
                throw new IllegalArgumentException("科室视图模式下，科室ID不能为空");
            }
            if (deptId <= 0) {
                log.error("查询候诊列表失败（科室视图）: 科室ID无效 - {}", deptId);
                throw new IllegalArgumentException("科室ID必须大于0，当前值: " + deptId);
            }
        } else {
            // 个人视图：必须提供医生ID
            if (doctorId == null) {
                log.error("查询候诊列表失败（个人视图）: 医生ID为null");
                throw new IllegalArgumentException("个人视图模式下，医生ID不能为空");
            }
            if (doctorId <= 0) {
                log.error("查询候诊列表失败（个人视图）: 医生ID无效 - {}", doctorId);
                throw new IllegalArgumentException("医生ID必须大于0，当前值: " + doctorId);
            }
        }

        List<Registration> registrations;

        // 定义活跃患者状态列表：WAITING（待就诊）、PAID_REGISTRATION（已缴挂号费）、IN_CONSULTATION（就诊中）
        List<Short> activeStatuses = Arrays.asList(
                RegStatusEnum.WAITING.getCode(),
                RegStatusEnum.PAID_REGISTRATION.getCode(),
                RegStatusEnum.IN_CONSULTATION.getCode()
        );

        if (showAllDept) {
            // 科室视图：查询整个科室的候诊列表
            // 防御性编程2: 验证科室是否存在
            Department department = departmentRepository.findById(deptId).orElse(null);
            if (department == null) {
                log.warn("查询候诊列表失败: 科室不存在，ID: {}", deptId);
                throw new IllegalArgumentException("科室不存在，ID: " + deptId);
            }

            // 防御性编程3: 验证科室是否被删除
            if (department.getIsDeleted() != null && department.getIsDeleted().equals(CommonConstants.DELETED)) {
                log.warn("查询候诊列表失败: 科室已被删除，ID: {}, 名称: {}", deptId, department.getName());
                throw new IllegalArgumentException("科室已停用: " + department.getName());
            }

            // 防御性编程4: 验证科室是否启用
            if (department.getStatus() != null && department.getStatus().equals(CommonConstants.STATUS_DISABLED)) {
                log.warn("查询候诊列表失败: 科室已停用，ID: {}, 名称: {}", deptId, department.getName());
                throw new IllegalArgumentException("科室已停用: " + department.getName());
            }

            log.info("使用科室视图查询，科室ID: {}, 科室名称: {}", deptId, department.getName());

            // 查询今日、指定科室、活跃患者状态的挂号记录，按状态码升序、排队号升序
            registrations = registrationRepository
                    .findByDepartmentAndStatuses(
                            LocalDate.now(),
                            deptId,
                            activeStatuses
                    );

            log.info("科室[{}]查询到 {} 条候诊记录", department.getName(), registrations.size());
        } else {
            // 个人视图：仅查询分配给当前医生的候诊列表
            log.info("使用个人视图查询，医生ID: {}", doctorId);

            // 查询今日、指定医生、活跃患者状态的挂号记录，按状态码升序、排队号升序
            registrations = registrationRepository
                    .findByDoctorAndStatuses(
                            LocalDate.now(),
                            doctorId,
                            activeStatuses
                    );

            log.info("医生[ID:{}]查询到 {} 条候诊记录", doctorId, registrations.size());
        }

        // 转换为 VO
        return registrations.stream()
                .map(VoConverter::toRegistrationVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新挂号状态（接诊或完成就诊）
     * 防御性编程: 完整的参数验证和状态转换检查
     *
     * @param regId 挂号记录ID
     * @param newStatus 新状态
     * @throws IllegalArgumentException 当参数无效或记录不存在时
     * @throws IllegalStateException 当状态转换不合法时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long regId, RegStatusEnum newStatus) {
        // 防御性编程1: 参数非空验证
        if (regId == null) {
            log.error("更新挂号状态失败: 挂号ID为null");
            throw new IllegalArgumentException("挂号ID不能为空");
        }

        if (regId <= 0) {
            log.error("更新挂号状态失败: 挂号ID无效 - {}", regId);
            throw new IllegalArgumentException("挂号ID必须大于0，当前值: " + regId);
        }

        if (newStatus == null) {
            log.error("更新挂号状态失败: 新状态为null，挂号ID: {}", regId);
            throw new IllegalArgumentException("新状态不能为空");
        }

        log.info("更新挂号状态，挂号ID: {}, 新状态: {}", regId, newStatus);

        // 防御性编程2: 查询挂号记录
        Registration registration = registrationRepository.findById(regId)
                .orElseThrow(() -> {
                    log.warn("更新挂号状态失败: 挂号记录不存在，ID: {}", regId);
                    return new IllegalArgumentException("挂号记录不存在，ID: " + regId);
                });

        // 防御性编程3: 检查记录是否被删除
        if (registration.getIsDeleted() != null && registration.getIsDeleted().equals(CommonConstants.DELETED)) {
            log.warn("更新挂号状态失败: 挂号记录已被删除，ID: {}", regId);
            throw new IllegalArgumentException("该挂号记录已被删除，无法操作");
        }

        // 防御性编程4: 检查就诊日期
        if (registration.getVisitDate() != null && !registration.getVisitDate().equals(LocalDate.now())) {
            log.warn("更新挂号状态失败: 只能操作今日的挂号记录，挂号ID: {}, 就诊日期: {}",
                    regId, registration.getVisitDate());
            throw new IllegalStateException(
                    String.format("只能操作今日的挂号记录，该记录就诊日期为: %s", registration.getVisitDate())
            );
        }

        // 获取当前状态描述
        String currentStatusDesc = "未知";
        try {
            currentStatusDesc = RegStatusEnum.fromCode(registration.getStatus()).getDescription();
        } catch (Exception e) {
            log.warn("无法解析当前状态码: {}", registration.getStatus());
        }

        // 【关键修复】使用状态机更新状态，确保审计日志和状态转换验证
        try {
            // 获取当前用户信息
            Long operatorId = null;
            String operatorName = "SYSTEM";
            try {
                operatorId = com.his.common.SecurityUtils.getCurrentUserId();
                operatorName = com.his.common.SecurityUtils.getCurrentUsername();
            } catch (Exception e) {
                log.warn("无法从安全上下文获取用户信息，使用系统默认值: {}", e.getMessage());
            }

            // 调用状态机进行状态转换（自动验证合法性并记录审计日志）
            registrationStateMachine.transition(
                regId,
                RegStatusEnum.fromCode(registration.getStatus()),
                newStatus,
                operatorId,
                operatorName,
                "医生工作站更新状态"
            );

            log.info("挂号状态通过状态机更新成功，挂号ID: {}, 原状态: {}, 新状态: {}",
                    regId, currentStatusDesc, newStatus.getDescription());
        } catch (Exception e) {
            log.error("状态机转换失败，挂号ID: {}, 原状态: {}, 新状态: {}",
                    regId, currentStatusDesc, newStatus.getDescription(), e);
            throw new IllegalStateException("状态转换失败: " + e.getMessage());
        }
    }

    /**
     * 【新增】验证并更新挂号状态（带医生身份验证，防止水平越权IDOR）
     *
     * <p><b>核心安全逻辑：</b></p>
     * <ol>
     *   <li>验证挂号记录存在且未被删除</li>
     *   <li>验证该挂号的医生ID是否与当前医生ID相同</li>
     *   <li>如果医生ID不匹配，抛出异常：只有拥有该挂号的医生才能更新</li>
     *   <li>然后执行updateStatus()中的所有验证（状态合法性、日期验证等）</li>
     * </ol>
     *
     * <p><b>IDOR防御：</b>通过将doctorId从JWT Token中获取，而不是来自URL参数，
     * 确保医生无法伪造其他医生的身份来更新他人的挂号。</p>
     *
     * @param regId 挂号记录ID
     * @param currentDoctorId 当前医生ID（从JWT Token中提取，绝对可信）
     * @param newStatus 新状态
     * @throws IllegalArgumentException 如果挂号不存在或医生无权限
     */
    @Override
    public void validateAndUpdateStatus(Long regId, Long currentDoctorId, RegStatusEnum newStatus) {
        // 防御性编程1: 参数非空验证
        if (regId == null || regId <= 0) {
            log.error("【IDOR防御】验证更新挂号状态失败: 挂号ID无效 - {}", regId);
            throw new IllegalArgumentException("挂号ID必须大于0，当前值: " + regId);
        }

        if (currentDoctorId == null || currentDoctorId <= 0) {
            log.error("【IDOR防御】验证更新挂号状态失败: 当前医生ID无效 - {}", currentDoctorId);
            throw new IllegalArgumentException("医生ID无效");
        }

        if (newStatus == null) {
            log.error("【IDOR防御】验证更新挂号状态失败: 新状态为null，挂号ID: {}", regId);
            throw new IllegalArgumentException("新状态不能为空");
        }

        log.info("【IDOR防御】验证并更新挂号状态，挂号ID: {}, 当前医生ID: {}, 新状态: {}",
                regId, currentDoctorId, newStatus);

        // 防御性编程2: 查询挂号记录及其医生信息
        Registration registration = registrationRepository.findById(regId)
                .orElseThrow(() -> {
                    log.warn("【IDOR防御】验证失败: 挂号记录不存在，ID: {}, 医生ID: {}", regId, currentDoctorId);
                    return new IllegalArgumentException("挂号记录不存在，ID: " + regId);
                });

        // 防御性编程3: 检查记录是否被删除
        if (registration.getIsDeleted() != null && registration.getIsDeleted().equals(CommonConstants.DELETED)) {
            log.warn("【IDOR防御】验证失败: 挂号记录已被删除，ID: {}, 医生ID: {}", regId, currentDoctorId);
            throw new IllegalArgumentException("该挂号记录已被删除，无法操作");
        }

        // 【关键安全检查】验证医生身份
        // 获取该挂号记录的医生ID
        if (registration.getDoctor() == null || registration.getDoctor().getMainId() == null) {
            log.error("【IDOR防御】验证失败: 挂号记录缺少医生信息，ID: {}, 当前医生ID: {}", regId, currentDoctorId);
            throw new IllegalArgumentException("挂号记录医生信息不完整");
        }

        Long registrationDoctorId = registration.getDoctor().getMainId();

        // ✅ 核心IDOR防御: 对比医生ID
        if (!registrationDoctorId.equals(currentDoctorId)) {
            log.warn("【IDOR防御】水平越权攻击被拦截！");
            log.warn("  - 挂号ID: {}", regId);
            log.warn("  - 挂号医生ID: {}", registrationDoctorId);
            log.warn("  - 当前医生ID: {}", currentDoctorId);
            log.warn("  - 医生尝试修改不属于自己的挂号");

            throw new IllegalArgumentException(
                    String.format("无权限操作: 该挂号属于其他医生（ID: %d），您只能操作自己的挂号", registrationDoctorId)
            );
        }

        log.info("【IDOR防御】医生身份验证通过，挂号ID: {}, 医生ID: {}", regId, currentDoctorId);

        // 验证通过，调用原有的updateStatus()方法进行状态更新及所有验证
        updateStatus(regId, newStatus);
    }


    /**
     * 【新增】查询患者详细信息（包含数据脱敏）
     * 防御性编程: 完整的参数验证和数据脱敏处理
     *
     * @param patientId 患者ID
     * @return 患者详细信息VO
     * @throws IllegalArgumentException 当患者不存在或已被删除时
     */
    @Override
    @Transactional(readOnly = true)
    public PatientDetailVO getPatientDetail(Long patientId) {
        log.info("查询患者详细信息，患者ID: {}", patientId);

        // 防御性编程1: 参数验证
        if (patientId == null || patientId <= 0) {
            log.error("查询患者详细信息失败: 患者ID无效 - {}", patientId);
            throw new IllegalArgumentException("患者ID必须大于0，当前值: " + patientId);
        }

        // 防御性编程2: 查询患者记录
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> {
                    log.warn("查询患者详细信息失败: 患者不存在，ID: {}", patientId);
                    return new IllegalArgumentException("患者信息不存在，ID: " + patientId);
                });

        // 防御性编程3: 检查是否被删除
        if (patient.getIsDeleted() != null && patient.getIsDeleted().equals(CommonConstants.DELETED)) {
            log.warn("查询患者详细信息失败: 患者已被删除，ID: {}", patientId);
            throw new IllegalArgumentException("该患者档案已被删除");
        }

        log.info("查询患者信息成功，患者ID: {}, 姓名: {}", patientId, patient.getName());

        // 转换为VO（包含脱敏处理）
        return convertToPatientDetailVO(patient);
    }

    /**
     * 转换为患者详细信息VO（带脱敏处理）
     */
    private PatientDetailVO convertToPatientDetailVO(Patient patient) {
        // 防御性编程: 检查入参
        if (patient == null) {
            log.error("convertToPatientDetailVO失败: patient为null");
            throw new IllegalArgumentException("患者信息不能为空");
        }

        // 获取性别描述
        String genderDesc = "未知";
        try {
            if (patient.getGender() != null) {
                genderDesc = GenderEnum.fromCode(patient.getGender()).getDescription();
            }
        } catch (Exception e) {
            log.warn("无法解析性别代码: {}", patient.getGender());
        }

        // 构建VO
        return PatientDetailVO.builder()
                .patientId(patient.getMainId())
                .patientNo(patient.getPatientNo())
                .name(patient.getName())
                .gender(patient.getGender())
                .genderDesc(genderDesc)
                .age(patient.getAge())
                .birthDate(patient.getBirthDate())
                .address(patient.getAddress())
                .medicalCardNo(patient.getMedicalCardNo())
                .bloodType(patient.getBloodType())
                .allergyHistory(patient.getAllergyHistory())
                .medicalHistory(patient.getMedicalHistory())
                .emergencyContact(patient.getEmergencyContact())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                // 脱敏处理敏感信息
                .phone(maskPhone(patient.getPhone()))
                .idCard(maskIdCard(patient.getIdCard()))
                .emergencyPhone(maskPhone(patient.getEmergencyPhone()))
                .build();
    }

    /**
     * 身份证号脱敏
     * 保留前3位和后4位，中间用11个*替代
     * 示例: 320***********1234
     */
    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏
     * 保留前3位和后4位，中间用4个*替代
     * 示例: 138****5678
     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 【新增】验证医生存在并返回医生信息（用于防止IDOR攻击）
     *
     * <p><b>安全验证：</b></p>
     * <ul>
     *   <li>验证医生ID是否有效</li>
     *   <li>验证医生是否存在且未被删除</li>
     *   <li>返回医生实体（包含科室信息）供Controller使用</li>
     * </ul>
     *
     * <p><b>分层架构：</b></p>
     * <ul>
     *   <li>此方法将DoctorController中的验证逻辑移到Service层</li>
     *   <li>遵守分层架构原则：Controller不直接依赖Repository</li>
     *   <li>保持安全验证的完整性</li>
     * </ul>
     *
     * @param doctorId 医生ID
     * @return 医生实体（包含科室信息）
     * @throws IllegalArgumentException 如果医生不存在或已被删除
     */
    @Override
    @Transactional(readOnly = true)
    public Doctor getAndValidateDoctor(Long doctorId) {
        log.debug("验证医生信息，医生ID: {}", doctorId);

        // 防御性编程: 参数验证
        if (doctorId == null || doctorId <= 0) {
            log.error("验证医生失败: 医生ID无效 - {}", doctorId);
            throw new IllegalArgumentException("医生ID必须大于0，当前值: " + doctorId);
        }

        // 查询医生记录
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.warn("验证医生失败: 医生不存在，ID: {}", doctorId);
                    return new IllegalArgumentException("指定的医生不存在，ID: " + doctorId);
                });

        // 检查是否被删除
        if (doctor.getIsDeleted() != null && doctor.getIsDeleted().equals(CommonConstants.DELETED)) {
            log.warn("验证医生失败: 医生已被删除，ID: {}, 姓名: {}", doctorId, doctor.getName());
            throw new IllegalArgumentException("该医生已被删除，无法操作");
        }

        // 验证科室信息是否存在
        if (doctor.getDepartment() == null) {
            log.error("验证医生失败: 医生科室信息为空，医生ID: {}", doctorId);
            throw new IllegalArgumentException("医生科室信息不完整，无法操作");
        }

        // 检查科室是否被删除
        if (doctor.getDepartment().getIsDeleted() != null && doctor.getDepartment().getIsDeleted().equals(CommonConstants.DELETED)) {
            log.warn("验证医生失败: 医生所属科室已被删除，医生ID: {}, 科室ID: {}",
                    doctorId, doctor.getDepartment().getMainId());
            throw new IllegalArgumentException("医生所属科室已被删除，无法操作");
        }

        log.debug("医生验证通过，医生ID: {}, 姓名: {}, 科室: {}",
                doctorId, doctor.getName(), doctor.getDepartment().getName());

        return doctor;
    }
}
