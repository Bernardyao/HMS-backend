package com.his.service.impl;

import com.his.dto.RegistrationDTO;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.entity.Patient;
import com.his.entity.Registration;
import com.his.enums.RegStatusEnum;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.repository.RegistrationRepository;
import com.his.service.RegistrationService;
import com.his.vo.RegistrationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 挂号服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final PatientRepository patientRepository;
    private final RegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 挂号（老患查找 + 新患建档 + 创建挂号单）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationVO register(RegistrationDTO dto) {
        log.info("开始挂号，患者身份证: {}, 科室ID: {}, 医生ID: {}", 
                dto.getIdCard(), dto.getDeptId(), dto.getDoctorId());

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

        // 4. 创建挂号单
        Registration registration = createRegistration(patient, department, doctor, dto);
        Registration savedRegistration = registrationRepository.save(registration);
        log.info("挂号单创建成功，挂号ID: {}, 挂号流水号: {}", 
                savedRegistration.getMainId(), savedRegistration.getRegNo());

        // 5. 构建返回对象
        return buildRegistrationVO(savedRegistration, patient, department, doctor);
    }

    /**
     * 根据 ID 查询挂号记录
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
     * 注意：取消后如需退费，需调用退费接口将状态更新为 REFUNDED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂号记录不存在，ID: " + id));

        if (!RegStatusEnum.WAITING.getCode().equals(registration.getStatus())) {
            throw new IllegalStateException("只有待就诊状态的挂号才能取消");
        }

        registration.setStatus(RegStatusEnum.CANCELLED.getCode());
        registration.setCancelReason(reason);
        registrationRepository.save(registration);

        log.info("挂号已取消，挂号ID: {}, 取消原因: {}", id, reason);
    }

    /**
     * 退费（将已取消的挂号标记为已退费）
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
     * 生成病历号（简单实现，实际项目可以用更复杂的规则或序列）
     */
    private String generatePatientNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = patientRepository.count() + 1;
        return String.format("P%s%04d", datePrefix, count);
    }

    /**
     * 生成挂号流水号
     */
    private String generateRegNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = registrationRepository.count() + 1;
        return String.format("R%s%04d", datePrefix, count);
    }

    /**
     * 生成排队号
     */
    private String generateQueueNo(Long deptId) {
        LocalDate today = LocalDate.now();
        long count = registrationRepository.countByDateAndDepartment(today, deptId) + 1;
        return String.format("%03d", count);
    }
}
