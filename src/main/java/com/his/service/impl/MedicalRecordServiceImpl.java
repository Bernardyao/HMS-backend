package com.his.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.his.dto.MedicalRecordDTO;
import com.his.entity.MedicalRecord;
import com.his.entity.Registration;
import com.his.enums.MedicalRecordStatusEnum;
import com.his.common.CommonConstants;
import com.his.repository.MedicalRecordRepository;
import com.his.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 病历服务实现类
 *
 * <p>负责患者病历的管理，包括病历的创建、更新、查询和提交等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>病历创建：为挂号患者创建病历档案</li>
 *   <li>病历更新：更新已有病历的内容和状态</li>
 *   <li>病历查询：根据ID或挂号单ID查询病历</li>
 *   <li>病历提交：将草稿状态的病历提交为正式病历</li>
 *   <li>懒加载处理：自动初始化关联实体，避免LazyInitializationException</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>每个挂号单只能有一条有效病历</li>
 *   <li>如果挂号单已存在病历，则更新；否则创建新病历</li>
 *   <li>病历编号自动生成（格式：MR+yyyyMMddHHmmss+3位随机数）</li>
 *   <li>只允许提交草稿状态（status=0）的病历</li>
 *   <li>病历必须关联有效的挂号单</li>
 * </ul>
 *
 * <h3>状态流转</h3>
 * <ul>
 *   <li>DRAFT（0=草稿）→ SUBMITTED（1=已提交）</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.MedicalRecord} - 病历主表</li>
 *   <li>{@link com.his.entity.Registration} - 挂号单（病历必须关联挂号单）</li>
 *   <li>{@link com.his.entity.Patient} - 患者信息</li>
 *   <li>{@link com.his.entity.Doctor} - 医生信息</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicalRecordService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements com.his.service.MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final RegistrationRepository registrationRepository;

    /**
     * 保存或更新病历
     *
     * <p>如果挂号单已存在病历则更新，否则创建新病历</p>
     *
     * <p><b>业务流程：</b></p>
     * <ol>
     *   <li>参数校验：验证挂号单ID不为空</li>
     *   <li>查询挂号单：验证挂号单存在且未删除</li>
     *   <li>检查病历：查询是否已存在病历</li>
     *   <li>创建或更新：存在则更新，不存在则创建</li>
     *   <li>保存病历：持久化到数据库</li>
     *   <li>初始化懒加载：避免LazyInitializationException</li>
     * </ol>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>每个挂号单只能有一条有效病历（isDeleted=0）</li>
     *   <li>病历编号自动生成（格式：MR+yyyyMMddHHmmss+3位随机数）</li>
     *   <li>新病历默认状态为草稿（status=0）</li>
     *   <li>自动关联挂号单的患者和医生信息</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>挂号单ID不为空</li>
     *   <li>挂号单存在且未删除</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>病历已创建或更新</li>
     *   <li>关联实体的懒加载字段已初始化</li>
     * </ul>
     *
     * @param dto 病历信息DTO
     *            <ul>
     *              <li>registrationId: 挂号单ID（必填）</li>
     *              <li>chiefComplaint: 主诉</li>
     *              <li>presentIllness: 现病史</li>
     *              <li>pastHistory: 既往史</li>
     *              <li>personalHistory: 个人史</li>
     *              <li>familyHistory: 家族史</li>
     *              <li>physicalExam: 体格检查</li>
     *              <li>auxiliaryExam: 辅助检查</li>
     *              <li>diagnosis: 诊断</li>
     *              <li>diagnosisCode: 诊断编码</li>
     *              <li>treatmentPlan: 治疗方案</li>
     *              <li>doctorAdvice: 医嘱</li>
     *              <li>status: 状态（可选，默认为0=草稿）</li>
     *            </ul>
     * @return 保存或更新后的病历实体
     * @throws IllegalArgumentException 如果挂号单ID为空
     * @throws IllegalArgumentException 如果挂号单不存在或已删除
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unused")
    public MedicalRecord saveOrUpdate(MedicalRecordDTO dto) {
        log.info("开始保存或更新病历，挂号单ID: {}", dto.getRegistrationId());

        // 1. 参数校验
        if (dto.getRegistrationId() == null) {
            throw new IllegalArgumentException("挂号单ID不能为空");
        }

        // 2. 查询挂号单是否存在
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (CommonConstants.DELETED.equals(registration.getIsDeleted())) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        // 显式初始化懒加载字段，确保可以访问
        // 触发懒加载，避免在事务外访问
        if (registration.getPatient() != null) {
            String patientName = registration.getPatient().getName();
        }
        if (registration.getDoctor() != null) {
            String doctorName = registration.getDoctor().getName();
        }

        // 3. 检查是否已存在病历
        MedicalRecord medicalRecord = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(dto.getRegistrationId(), CommonConstants.NORMAL)
                .orElse(null);

        if (medicalRecord != null) {
            // 业务校验：已提交或已审核的病历不允许再修改，必须维持医疗记录的严肃性
            if (!MedicalRecordStatusEnum.DRAFT.getCode().equals(medicalRecord.getStatus())) {
                log.warn("保存/更新病历被拒绝：病历已提交或已审核，ID: {}, 当前状态: {}",
                        medicalRecord.getMainId(), medicalRecord.getStatus());
                throw new IllegalStateException("已提交或已审核的病历不允许修改");
            }

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
     *
     * <p>查询指定ID的病历及其关联信息</p>
     *
     * <p><b>查询内容：</b></p>
     * <ul>
     *   <li>病历基本信息（病历编号、内容、状态等）</li>
     *   <li>患者信息（姓名、性别、年龄等）</li>
     *   <li>医生信息（医生姓名）</li>
     *   <li>挂号单信息</li>
     * </ul>
     *
     * @param id 病历ID
     * @return 病历实体（包含关联信息）
     * @throws IllegalArgumentException 如果病历ID为空
     * @throws IllegalArgumentException 如果病历不存在
     * @throws IllegalArgumentException 如果病历已删除
     * @since 1.0
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

        if (CommonConstants.DELETED.equals(record.getIsDeleted())) {
            throw new IllegalArgumentException("病历已被删除，ID: " + id);
        }

        // 初始化懒加载字段
        initializeLazyFields(record);

        return record;
    }

    /**
     * 根据挂号单ID查询病历
     *
     * <p>查询指定挂号单的病历记录</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>每个挂号单最多有一条有效病历</li>
     *   <li>如果病历不存在，返回null（不抛出异常）</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>医生开具处方前查询病历</li>
     *   <li>查看患者病史记录</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @return 病历实体，如果不存在则返回null
     * @throws IllegalArgumentException 如果挂号单ID为空
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public MedicalRecord getByRegistrationId(Long registrationId) {
        log.info("根据挂号单ID查询病历，挂号单ID: {}", registrationId);

        if (registrationId == null) {
            throw new IllegalArgumentException("挂号单ID不能为空");
        }

        MedicalRecord record = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(registrationId, CommonConstants.NORMAL)
                .orElse(null);

        // 初始化懒加载字段
        if (record != null) {
            initializeLazyFields(record);
        }

        return record;
    }

    /**
     * 提交病历
     *
     * <p>将草稿状态的病历提交为正式病历</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只允许提交草稿状态（status=0）的病历</li>
     *   <li>提交后病历状态变更为已提交（status=1）</li>
     *   <li>已提交的病历不能再次修改</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>医生完成接诊后提交病历</li>
     *   <li>提交后的病历才能开具处方</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>病历存在且未删除</li>
     *   <li>病历状态为草稿（status=0）</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>病历状态更新为已提交（status=1）</li>
     * </ul>
     *
     * @param id 病历ID
     * @throws IllegalArgumentException 如果病历不存在
     * @throws IllegalStateException 如果病历状态不是草稿
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        log.info("提交病历，ID: {}", id);

        MedicalRecord record = getById(id);

        // 幂等处理：如果病历已经提交，则直接返回成功，避免重复点击报错
        if (MedicalRecordStatusEnum.SUBMITTED.getCode().equals(record.getStatus())) {
            log.info("病历已处于提交状态，无需重复操作，ID: {}", id);
            return;
        }

        if (!MedicalRecordStatusEnum.DRAFT.getCode().equals(record.getStatus())) {
            throw new IllegalStateException("只有草稿状态的病历才能提交");
        }

        record.setStatus(MedicalRecordStatusEnum.SUBMITTED.getCode());
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

        // 设置关联 - 直接使用registration对象及其关联
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());

        // 设置病历内容
        setMedicalRecordContent(record, dto);

        // 设置状态
        record.setStatus(dto.getStatus() != null ? dto.getStatus() : MedicalRecordStatusEnum.DRAFT.getCode());
        record.setIsDeleted(CommonConstants.NORMAL);

        // 时间字段将由@PrePersist自动设置，不需要手动设置

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

        // 更新时间将由@PreUpdate自动设置
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
     * 强制初始化 JPA 懒加载关联，避免 LazyInitializationException
     *
     * <p><b>重要说明：</b></p>
     * <ul>
     *   <li>这些方法调用是有副作用的（触发 SQL 查询）</li>
     *   <li>变量赋值仅为了避免编译器/SpotBugs "unused" 警告</li>
     *   <li>必须在事务内调用，否则会抛出 LazyInitializationException</li>
     * </ul>
     *
     * <p><b>为什么需要这个方法：</b></p>
     * <pre>
     * 1. Service 方法返回后，事务可能关闭
     * 2. Controller/序列化访问懒加载字段 → LazyInitializationException
     * 3. 解决方案：在事务内主动触发加载
     * </pre>
     *
     * @param record 要初始化的病历实体
     */
    @SuppressWarnings("unused")
    private void initializeLazyFields(MedicalRecord record) {
        // 在事务内主动触发 SQL 查询，加载关联实体
        // 这样即使事务关闭后，这些字段仍然可用
        if (record.getPatient() != null) {
            String patientName = record.getPatient().getName(); // 触发 Patient 加载
        }
        if (record.getDoctor() != null) {
            String doctorName = record.getDoctor().getName(); // 触发 Doctor 加载
        }
        if (record.getRegistration() != null) {
            Long mainId = record.getRegistration().getMainId(); // 触发 Registration 加载
        }
    }
}
