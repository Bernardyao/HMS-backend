package com.his.service.impl;

import com.his.dto.MedicalRecordDTO;
import com.his.entity.MedicalRecord;
import com.his.entity.Registration;
import com.his.repository.MedicalRecordRepository;
import com.his.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 病历服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements com.his.service.MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final RegistrationRepository registrationRepository;

    /**
     * 保存或更新病历
     * 如果该挂号单ID已存在病历，则更新；否则新建
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecord saveOrUpdate(MedicalRecordDTO dto) {
        log.info("开始保存或更新病历，挂号单ID: {}", dto.getRegistrationId());

        // 1. 参数校验
        if (dto.getRegistrationId() == null) {
            throw new IllegalArgumentException("挂号单ID不能为空");
        }

        // 2. 查询挂号单是否存在
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        // 3. 检查是否已存在病历
        MedicalRecord medicalRecord = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(dto.getRegistrationId(), (short) 0)
                .orElse(null);

        if (medicalRecord != null) {
            // 更新现有病历
            log.info("找到现有病历，ID: {}，进行更新", medicalRecord.getMainId());
            updateMedicalRecord(medicalRecord, dto);
        } else {
            // 创建新病历
            log.info("未找到现有病历，创建新病历");
            medicalRecord = createMedicalRecord(registration, dto);
        }

        // 4. 保存病历
        MedicalRecord savedRecord = medicalRecordRepository.save(medicalRecord);
        log.info("病历保存成功，ID: {}, 病历编号: {}", savedRecord.getMainId(), savedRecord.getRecordNo());

        // 5. 初始化懒加载字段，避免LazyInitializationException
        initializeLazyFields(savedRecord);

        return savedRecord;
    }

    /**
     * 根据ID查询病历
     */
    @Override
    @Transactional(readOnly = true)
    public MedicalRecord getById(Long id) {
        log.info("查询病历，ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("病历ID不能为空");
        }

        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("病历不存在，ID: " + id));

        if (record.getIsDeleted() == 1) {
            throw new IllegalArgumentException("病历已被删除，ID: " + id);
        }

        // 初始化懒加载字段
        initializeLazyFields(record);

        return record;
    }

    /**
     * 根据挂号单ID查询病历
     */
    @Override
    @Transactional(readOnly = true)
    public MedicalRecord getByRegistrationId(Long registrationId) {
        log.info("根据挂号单ID查询病历，挂号单ID: {}", registrationId);

        if (registrationId == null) {
            throw new IllegalArgumentException("挂号单ID不能为空");
        }

        MedicalRecord record = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(registrationId, (short) 0)
                .orElse(null);

        // 初始化懒加载字段
        if (record != null) {
            initializeLazyFields(record);
        }

        return record;
    }

    /**
     * 提交病历（状态改为已提交）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        log.info("提交病历，ID: {}", id);

        MedicalRecord record = getById(id);

        if (record.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的病历才能提交");
        }

        record.setStatus((short) 1);
        record.setUpdatedAt(LocalDateTime.now());
        medicalRecordRepository.save(record);

        log.info("病历提交成功，ID: {}", id);
    }

    /**
     * 创建新病历
     */
    private MedicalRecord createMedicalRecord(Registration registration, MedicalRecordDTO dto) {
        MedicalRecord record = new MedicalRecord();

        // 生成病历编号
        String recordNo = generateRecordNo();
        record.setRecordNo(recordNo);

        // 设置关联
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());

        // 设置病历内容
        setMedicalRecordContent(record, dto);

        // 设置状态和时间
        record.setStatus(dto.getStatus() != null ? dto.getStatus() : (short) 0);
        record.setIsDeleted((short) 0);
        record.setVisitTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        return record;
    }

    /**
     * 更新病历
     */
    private void updateMedicalRecord(MedicalRecord record, MedicalRecordDTO dto) {
        // 更新病历内容
        setMedicalRecordContent(record, dto);

        // 更新状态
        if (dto.getStatus() != null) {
            record.setStatus(dto.getStatus());
        }

        // 更新时间
        record.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 设置病历内容
     */
    private void setMedicalRecordContent(MedicalRecord record, MedicalRecordDTO dto) {
        record.setChiefComplaint(dto.getChiefComplaint());
        record.setPresentIllness(dto.getPresentIllness());
        record.setPastHistory(dto.getPastHistory());
        record.setPersonalHistory(dto.getPersonalHistory());
        record.setFamilyHistory(dto.getFamilyHistory());
        record.setPhysicalExam(dto.getPhysicalExam());
        record.setAuxiliaryExam(dto.getAuxiliaryExam());
        record.setDiagnosis(dto.getDiagnosis());
        record.setDiagnosisCode(dto.getDiagnosisCode());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setDoctorAdvice(dto.getDoctorAdvice());
    }

    /**
     * 生成病历编号
     * 格式：MR + yyyyMMddHHmmss + 随机3位数
     */
    private String generateRecordNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "MR" + timestamp + random;
    }

    /**
     * 初始化懒加载字段，避免LazyInitializationException
     */
    private void initializeLazyFields(MedicalRecord record) {
        if (record.getPatient() != null) {
            // 触发Patient的懒加载
            record.getPatient().getName();
        }
        if (record.getDoctor() != null) {
            // 触发Doctor的懒加载
            record.getDoctor().getName();
        }
        if (record.getRegistration() != null) {
            // 触发Registration的懒加载
            record.getRegistration().getMainId();
        }
    }
}
