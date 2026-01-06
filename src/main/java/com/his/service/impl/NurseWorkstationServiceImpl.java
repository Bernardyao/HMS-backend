package com.his.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.his.dto.NurseWorkstationDTO;
import com.his.dto.PaymentDTO;
import com.his.common.CommonConstants;
import com.his.entity.Charge;
import com.his.entity.Registration;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.ChargeTypeEnum;
import com.his.enums.GenderEnum;
import com.his.enums.RegStatusEnum;
import com.his.enums.VisitTypeEnum;
import com.his.repository.ChargeRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.ChargeService;
import com.his.service.NurseWorkstationService;
import com.his.vo.ChargeVO;
import com.his.vo.NurseRegistrationVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 护士工作站服务实现类
 *
 * <p>为护士工作站提供挂号查询和筛选功能，支持多条件动态查询</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>挂号列表查询：查询指定日期的挂号记录</li>
 *   <li>多条件筛选：支持科室、状态、就诊类型、关键字等多维度筛选</li>
 *   <li>关键字搜索：支持患者姓名和挂号号的模糊查询</li>
 *   <li>数据脱敏：自动对患者敏感信息进行脱敏处理</li>
 * </ul>
 *
 * <h3>查询规则</h3>
 * <ul>
 *   <li>默认查询当天的挂号记录</li>
 *   <li>仅返回未删除的挂号（isDeleted=0）</li>
 *   <li>按创建时间升序排列（先挂号的在前）</li>
 *   <li>所有查询条件都是可选的</li>
 * </ul>
 *
 * <h3>支持的条件</h3>
 * <ul>
 *   <li>就诊日期：默认当天，可指定日期</li>
 *   <li>科室ID：筛选指定科室的挂号</li>
 *   <li>挂号状态：筛选指定状态的挂号</li>
 *   <li>就诊类型：筛选初诊或复诊</li>
 *   <li>关键字：患者姓名或挂号号模糊匹配</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>护士查询今日挂号列表</li>
 *   <li>按科室筛选挂号</li>
 *   <li>搜索特定患者的挂号信息</li>
 *   <li>统计各科室的挂号量</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.NurseWorkstationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NurseWorkstationServiceImpl implements NurseWorkstationService {

    private final RegistrationRepository registrationRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeService chargeService;

    /**
     * 获取挂号列表（支持多条件查询）
     *
     * <p>根据指定条件查询挂号记录，所有条件都是可选的</p>
     *
     * <p><b>默认查询：</b></p>
     * <ul>
     *   <li>默认查询当天的挂号记录</li>
     *   <li>仅返回未删除的挂号</li>
     * </ul>
     *
     * <p><b>支持的筛选条件：</b></p>
     * <ul>
     *   <li>就诊日期：可指定任意日期</li>
     *   <li>科室ID：筛选指定科室的挂号</li>
     *   <li>挂号状态：筛选指定状态的挂号（如：待就诊、已就诊等）</li>
     *   <li>就诊类型：筛选初诊或复诊</li>
     *   <li>关键字：患者姓名或挂号号的模糊查询</li>
     * </ul>
     *
     * <p><b>排序规则：</b></p>
     * <ul>
     *   <li>按创建时间升序排列（先挂号的在前）</li>
     * </ul>
     *
     * @param dto 查询条件DTO（所有字段都是可选的）
     *            <ul>
     *              <li>visitDate: 就诊日期（可选，默认为当天）</li>
     *              <li>departmentId: 科室ID（可选）</li>
     *              <li>status: 挂号状态（可选）</li>
     *              <li>visitType: 就诊类型（可选，1=初诊, 2=复诊）</li>
     *              <li>keyword: 搜索关键字（可选，支持患者姓名或挂号号）</li>
     *            </ul>
     * @return 挂号视图对象列表（NurseRegistrationVO）
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<NurseRegistrationVO> getTodayRegistrations(NurseWorkstationDTO dto) {
        // 默认查询当天
        LocalDate visitDate = dto != null && dto.getVisitDate() != null ? dto.getVisitDate() : LocalDate.now();

        // 构建动态查询条件
        Specification<Registration> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除
            predicates.add(cb.equal(root.get("isDeleted"), CommonConstants.NORMAL));

            // 就诊日期
            predicates.add(cb.equal(root.get("visitDate"), visitDate));

            // 科室ID
            if (dto != null && dto.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("mainId"), dto.getDepartmentId()));
            }

            // 状态
            if (dto != null && dto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), dto.getStatus()));
            }

            // 就诊类型
            if (dto != null && dto.getVisitType() != null) {
                predicates.add(cb.equal(root.get("visitType"), dto.getVisitType()));
            }

            // 关键字查询（患者姓名或挂号号）
            if (dto != null && StringUtils.hasText(dto.getKeyword())) {
                String keyword = "%" + dto.getKeyword().trim() + "%";
                Predicate namePredicate = cb.like(root.get("patient").get("name"), keyword);
                Predicate regNoPredicate = cb.like(root.get("regNo"), keyword);
                predicates.add(cb.or(namePredicate, regNoPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 按创建时间排序
        List<Registration> registrations = registrationRepository.findAll(spec,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt"));

        log.info("查询到 {} 条挂号记录，日期: {}", registrations.size(), visitDate);

        // 转换为 VO
        return registrations.stream()
                .map(this::convertToNurseVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 Registration 实体转换为护士工作站 VO
     */
    private NurseRegistrationVO convertToNurseVO(Registration reg) {
        NurseRegistrationVO vo = new NurseRegistrationVO();

        vo.setId(reg.getMainId());
        vo.setRegNo(reg.getRegNo());
        vo.setStatus(reg.getStatus());
        vo.setStatusDesc(RegStatusEnum.fromCode(reg.getStatus()).getDescription());
        vo.setVisitType(reg.getVisitType());
        vo.setVisitTypeDesc(getVisitTypeDesc(reg.getVisitType()));
        vo.setVisitDate(reg.getVisitDate());
        vo.setRegistrationFee(reg.getRegistrationFee());
        vo.setQueueNo(reg.getQueueNo());
        vo.setAppointmentTime(reg.getAppointmentTime());
        vo.setCreatedAt(reg.getCreatedAt());

        // 患者信息
        if (reg.getPatient() != null) {
            vo.setPatientId(reg.getPatient().getMainId());
            vo.setPatientName(reg.getPatient().getName());
            vo.setAge(reg.getPatient().getAge());
            vo.setGenderDesc(getGenderDesc(reg.getPatient().getGender()));
            // 脱敏处理
            vo.setIdCard(maskIdCard(reg.getPatient().getIdCard()));
            vo.setPhone(maskPhone(reg.getPatient().getPhone()));
        }

        // 科室信息
        if (reg.getDepartment() != null) {
            vo.setDeptId(reg.getDepartment().getMainId());
            vo.setDeptName(reg.getDepartment().getName());
        }

        // 医生信息
        if (reg.getDoctor() != null) {
            vo.setDoctorId(reg.getDoctor().getMainId());
            vo.setDoctorName(reg.getDoctor().getName());
            vo.setDoctorTitle(reg.getDoctor().getTitle());
        }

        // 是否有病历
        vo.setHasMedicalRecord(reg.getMedicalRecord() != null);

        return vo;
    }

    /**
     * 获取就诊类型描述
     */
    private String getVisitTypeDesc(Short visitType) {
        if (visitType == null) {
            return "未知";
        }
        try {
            return VisitTypeEnum.fromCode(visitType).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 获取性别描述
     */
    private String getGenderDesc(Short gender) {
        if (gender == null) {
            return "未知";
        }
        try {
            return GenderEnum.fromCode(gender).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 身份证号脱敏
     */
    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 护士站收取挂号费
     *
     * <p>为护士站提供一键收取挂号费的功能，无需患者到收费窗口</p>
     *
     * <p><b>实现逻辑：</b></p>
     * <ol>
     *   <li>验证挂号单存在且状态为 WAITING</li>
     *   <li>检查挂号费是否已支付（防止重复收费）</li>
     *   <li>查找或创建收费单：
     *       <ul>
     *         <li>查找已存在的未支付挂号收费单</li>
     *         <li>如果不存在，创建新的挂号收费单</li>
     *       </ul>
     *   </li>
     *   <li>自动填充支付金额：从收费单获取实际金额</li>
     *   <li>生成交易流水号：如果未提供，自动生成格式为 NR_REG_{regId}_{timestamp}</li>
     *   <li>调用收费服务完成支付（内部会触发状态机更新挂号状态）</li>
     * </ol>
     *
     * @param registrationId 挂号单ID
     * @param paymentDTO 支付信息（必须包含 paymentMethod，可选 transactionNo）
     * @return 支付后的收费单信息
     * @throws IllegalArgumentException 如果挂号单不存在或状态不为 WAITING
     * @throws IllegalStateException 如果挂号费已支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO payRegistrationFee(Long registrationId, PaymentDTO paymentDTO) {
        log.info("护士站开始收取挂号费，挂号ID: {}", registrationId);

        // 1. 检查是否已支付（防止重复收费，最优先检查）
        if (chargeService.isRegistrationFeePaid(registrationId)) {
            throw new IllegalStateException("该挂号的挂号费已支付，无需重复缴费");
        }

        // 2. 验证挂号单状态
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + registrationId));

        if (!RegStatusEnum.WAITING.getCode().equals(registration.getStatus())) {
            throw new IllegalArgumentException("挂号单状态不正确，当前状态: " + RegStatusEnum.fromCode(registration.getStatus()).getDescription() + "，仅可为待就诊状态的挂号缴费");
        }

        // 3. 查找或创建收费单
        Charge existingCharge = findUnpaidRegistrationCharge(registrationId);
        ChargeVO charge;

        if (existingCharge != null) {
            log.info("找到已存在的未支付挂号收费单，收费单ID: {}", existingCharge.getMainId());
            charge = chargeService.getById(existingCharge.getMainId());
        } else {
            log.info("创建新的挂号收费单，挂号ID: {}", registrationId);
            charge = chargeService.createRegistrationCharge(registrationId);
        }

        // 4. 自动填充支付金额
        paymentDTO.setPaidAmount(charge.getTotalAmount());

        // 5. 生成交易流水号（如果未提供）
        if (paymentDTO.getTransactionNo() == null || paymentDTO.getTransactionNo().isEmpty()) {
            String transactionNo = String.format("NR_REG_%d_%d", registrationId, System.currentTimeMillis());
            paymentDTO.setTransactionNo(transactionNo);
            log.info("自动生成交易流水号: {}", transactionNo);
        }

        // 6. 调用收费服务完成支付
        ChargeVO result = chargeService.processPayment(charge.getId(), paymentDTO);
        log.info("护士站收取挂号费成功，挂号ID: {}, 收费单ID: {}", registrationId, result.getId());

        return result;
    }

    /**
     * 查找未支付的挂号收费单
     *
     * <p>在创建新收费单前，先检查是否已存在未支付的挂号收费单，避免重复创建</p>
     *
     * @param registrationId 挂号单ID
     * @return 未支付的挂号收费单，如果不存在返回 null
     */
    private Charge findUnpaidRegistrationCharge(Long registrationId) {
        List<Charge> charges = chargeRepository.findByRegistration_MainIdAndChargeTypeAndIsDeleted(
                registrationId,
                ChargeTypeEnum.REGISTRATION_ONLY.getCode(), // 1=仅挂号费
                CommonConstants.NORMAL  // 未删除
        );

        return charges.stream()
                .filter(c -> ChargeStatusEnum.UNPAID.getCode().equals(c.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
