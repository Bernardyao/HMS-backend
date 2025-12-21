package com.his.service.impl;

import com.his.dto.NurseWorkstationDTO;
import com.his.entity.Registration;
import com.his.enums.GenderEnum;
import com.his.enums.RegStatusEnum;
import com.his.enums.VisitTypeEnum;
import com.his.repository.RegistrationRepository;
import com.his.service.NurseWorkstationService;
import com.his.vo.NurseRegistrationVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 护士工作站服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NurseWorkstationServiceImpl implements NurseWorkstationService {

    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NurseRegistrationVO> getTodayRegistrations(NurseWorkstationDTO dto) {
        // 默认查询当天
        LocalDate visitDate = dto != null && dto.getVisitDate() != null ? dto.getVisitDate() : LocalDate.now();

        // 构建动态查询条件
        Specification<Registration> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除
            predicates.add(cb.equal(root.get("isDeleted"), (short) 0));

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
}
