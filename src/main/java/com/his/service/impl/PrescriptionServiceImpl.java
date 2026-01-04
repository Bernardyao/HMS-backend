package com.his.service.impl;

import com.his.dto.PrescriptionDTO;
import com.his.entity.*;
import com.his.enums.PrescriptionStatusEnum;
import com.his.repository.*;
import com.his.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处方服务实现类
 *
 * <p>负责处方管理的核心业务逻辑，包括处方创建、审核、发药、退药等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>处方创建：医生开具处方，包含多个药品和用药说明</li>
 *   <li>处方审核：药师审核处方的合理性和合法性</li>
 *   <li>处方发药：药师根据已缴费处方发放药品并扣减库存</li>
 *   <li>处方退药：退回已发放的药品并恢复库存</li>
 *   <li>库存管理：发药时扣减库存，退药时恢复库存</li>
 *   <li>工作统计：统计药师工作量和发药金额</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>处方必须关联有效的挂号单和病历</li>
 *   <li>处方号使用PostgreSQL序列生成，保证唯一性和并发安全性</li>
 *   <li>药品必须存在且未停用才能添加到处方</li>
 *   <li>处方总金额 = Σ(单价 × 数量)</li>
 *   <li>发药时检查库存，库存不足则抛出异常</li>
 *   <li>发药后自动扣减药品库存</li>
 *   <li>退药时自动恢复药品库存</li>
 * </ul>
 *
 * <h3>状态流转</h3>
 * <ul>
 *   <li>DRAFT（草稿）→ ISSUED（已开方）→ REVIEWED（已审核）→ PAID（已缴费）→ DISPENSED（已发药）</li>
 *   <li>DISPENSED（已发药）→ REFUNDED（已退药/已退费）</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.Prescription} - 处方主表</li>
 *   <li>{@link com.his.entity.PrescriptionDetail} - 处方明细表（包含药品信息）</li>
 *   <li>{@link com.his.entity.Medicine} - 药品信息（包含库存）</li>
 *   <li>{@link com.his.entity.MedicalRecord} - 病历（处方必须关联病历）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.PrescriptionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final RegistrationRepository registrationRepository;
    private final MedicineRepository medicineRepository;

    /**
     * 创建处方
     *
     * <p>医生为患者开具处方，包含药品列表、用药说明等信息</p>
     *
     * <p><b>业务流程：</b></p>
     * <ol>
     *   <li>参数校验：验证挂号单ID、药品列表、药品ID、数量等</li>
     *   <li>查询挂号单和病历：处方必须关联有效的挂号单和病历</li>
     *   <li>创建处方主记录：生成处方号，设置类型、有效期等</li>
     *   <li>创建处方明细：为每个药品创建明细记录，包含单价、数量、小计等</li>
     *   <li>计算总金额：处方总金额 = Σ(单价 × 数量)</li>
     *   <li>设置初始状态为 ISSUED（已开方）</li>
     * </ol>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>处方必须关联有效的挂号单且挂号单未删除</li>
     *   <li>必须先创建病历才能开具处方</li>
     *   <li>药品必须存在且未删除、未停用</li>
     *   <li>药品数量必须大于0</li>
     *   <li>处方号自动生成（格式：PRE+yyyyMMdd+6位序列号）</li>
     *   <li>处方总金额保留2位小数（四舍五入）</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>挂号单存在且未删除</li>
     *   <li>病历已创建</li>
     *   <li>药品列表不为空</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>处方记录已创建（状态为ISSUED）</li>
     *   <li>处方明细已创建</li>
     *   <li>处方总金额已计算</li>
     * </ul>
     *
     * @param dto 处方信息DTO
     *            <ul>
     *              <li>registrationId: 挂号单ID（必填）</li>
     *              <li>prescriptionType: 处方类型（可选，默认为1=普通处方）</li>
     *              <li>validityDays: 有效天数（可选，默认为3天）</li>
     *              <li>items: 处方明细列表（必填，至少包含一个药品）</li>
     *              <ul>
     *                <li>medicineId: 药品ID（必填）</li>
     *                <li>quantity: 数量（必填，必须大于0）</li>
     *                <li>frequency: 用药频次（如：一日三次）</li>
     *                <li>dosage: 每次剂量（如：1片）</li>
     *                <li>route: 用药途径（如：口服）</li>
     *                <li>days: 用药天数</li>
     *                <li>instructions: 用药说明</li>
     *              </ul>
     *            </ul>
     * @return 创建的处方实体（包含ID、处方号、状态等）
     * @throws IllegalArgumentException 如果参数校验失败
     * @throws IllegalArgumentException 如果挂号单不存在或已删除
     * @throws IllegalArgumentException 如果病历不存在（需要先创建病历）
     * @throws IllegalArgumentException 如果药品不存在、已删除或已停用
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Prescription createPrescription(PrescriptionDTO dto) {
        log.info("开始创建处方，挂号单ID: {}", dto.getRegistrationId());

        validatePrescriptionDTO(dto);

        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        MedicalRecord medicalRecord = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(dto.getRegistrationId(), (short) 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "请先创建病历再开处方，挂号单ID: " + dto.getRegistrationId()));

        Prescription prescription = createPrescriptionEntity(registration, medicalRecord, dto);

        List<PrescriptionDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;

        for (PrescriptionDTO.PrescriptionItemDTO itemDTO : dto.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDTO.getMedicineId())
                    .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + itemDTO.getMedicineId()));

            if (medicine.getIsDeleted() == 1) {
                throw new IllegalArgumentException("药品已被删除，ID: " + itemDTO.getMedicineId());
            }

            if (medicine.getStatus() != 1) {
                throw new IllegalArgumentException("药品已停用，ID: " + itemDTO.getMedicineId());
            }

            BigDecimal unitPrice = medicine.getRetailPrice();
            int quantity = itemDTO.getQuantity();

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);

            totalAmount = totalAmount.add(subtotal);
            itemCount += quantity;

            PrescriptionDetail detail = createPrescriptionDetail(prescription, medicine, itemDTO, unitPrice, subtotal);
            details.add(detail);

            log.info("添加处方明细：药品ID={}, 名称={}, 单价={}, 数量={}, 小计={}",
                    medicine.getMainId(), medicine.getName(), unitPrice, quantity, subtotal);
        }

        prescription.setTotalAmount(totalAmount);
        prescription.setItemCount(itemCount);
        prescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode()); // 1=已开方

        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("处方主表保存成功，ID: {}, 处方号: {}, 总金额: {}, 药品数量: {}",
                savedPrescription.getMainId(), savedPrescription.getPrescriptionNo(),
                savedPrescription.getTotalAmount(), savedPrescription.getItemCount());

        for (PrescriptionDetail detail : details) {
            detail.setPrescription(savedPrescription);
        }
        prescriptionDetailRepository.saveAll(details);
        log.info("处方明细保存成功，共 {} 条", details.size());

        initializeLazyFields(savedPrescription);

        return savedPrescription;
    }

    /**
     * 根据ID查询处方详情
     *
     * <p>查询指定ID的处方及其明细信息</p>
     *
     * @param id 处方ID
     * @return 处方实体（包含明细列表）
     * @throws IllegalArgumentException 如果处方ID为空
     * @throws IllegalArgumentException 如果处方不存在
     * @throws IllegalArgumentException 如果处方已删除
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public Prescription getById(Long id) {
        log.info("查询处方，ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("处方ID不能为空");
        }

        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("处方不存在，ID: " + id));

        if (prescription.getIsDeleted() == 1) {
            throw new IllegalArgumentException("处方已被删除，ID: " + id);
        }

        initializeLazyFields(prescription);

        return prescription;
    }

    /**
     * 根据病历ID查询处方列表
     *
     * <p>查询指定病历的所有未删除处方</p>
     *
     * @param recordId 病历ID
     * @return 处方列表
     * @throws IllegalArgumentException 如果病历ID为空
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getByRecordId(Long recordId) {
        log.info("根据病历ID查询处方列表，病历ID: {}", recordId);

        if (recordId == null) {
            throw new IllegalArgumentException("病历ID不能为空");
        }

        List<Prescription> prescriptions = prescriptionRepository.findByMedicalRecord_MainIdAndIsDeleted(recordId, (short) 0);
        
        prescriptions.forEach(this::initializeLazyFields);
        
        return prescriptions;
    }

    /**
     * 审核处方
     *
     * <p>药师审核处方的合理性和合法性，审核通过后处方才能缴费和发药</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只有状态为 ISSUED（已开方）的处方才能审核</li>
     *   <li>审核通过后处方状态变更为 REVIEWED（已审核）</li>
     *   <li>记录审核时间和审核意见</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>处方存在</li>
     *   <li>处方状态为 ISSUED（已开方）</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>处方状态更新为 REVIEWED</li>
     *   <li>记录审核时间和审核意见</li>
     * </ul>
     *
     * @param id 处方ID
     * @param reviewDoctorId 审核医生ID
     * @param remark 审核意见（可选）
     * @throws IllegalArgumentException 如果处方不存在
     * @throws IllegalStateException 如果处方状态不是已开方
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Long reviewDoctorId, String remark) {
        log.info("审核处方，ID: {}, 审核医生ID: {}", id, reviewDoctorId);

        Prescription prescription = getById(id);

        if (!PrescriptionStatusEnum.ISSUED.getCode().equals(prescription.getStatus())) {
            throw new IllegalStateException("只有已开方状态的处方才能审核");
        }

        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode()); // 2=已审核
        prescription.setReviewTime(LocalDateTime.now());
        prescription.setReviewRemark(remark);
        prescription.setUpdatedAt(LocalDateTime.now());

        prescriptionRepository.save(prescription);

        log.info("处方审核成功，ID: {}", id);
    }

    /**
     * 查询待发药处方列表
     *
     * <p>查询所有状态为 PAID（已缴费）的处方</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>药师工作站：获取待发药处方列表</li>
     * </ul>
     *
     * @return 待发药处方列表
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPendingDispenseList() {
        log.info("查询待发药处方列表");
        // 查询状态为已缴费(5)的处方
        List<Prescription> prescriptions = prescriptionRepository.findByStatusAndIsDeleted(
                PrescriptionStatusEnum.PAID.getCode(), (short) 0);
        prescriptions.forEach(this::initializeLazyFields);
        return prescriptions;
    }

    /**
     * 发药
     *
     * <p>药师根据已缴费处方发放药品并扣减库存</p>
     *
     * <p><b>业务流程：</b></p>
     * <ol>
     *   <li>查询处方和明细</li>
     *   <li>检查库存是否充足</li>
     *   <li>扣减药品库存</li>
     *   <li>更新处方状态为 DISPENSED（已发药）</li>
     * </ol>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只有状态为 PAID（已缴费）的处方才能发药</li>
     *   <li>发药时检查每个药品的库存是否充足</li>
     *   <li>如果任意药品库存不足，抛出异常并终止发药流程</li>
     *   <li>发药成功后扣减所有药品的库存</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>处方存在</li>
     *   <li>处方状态为 PAID（已缴费）</li>
     *   <li>所有药品库存充足</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>处方状态更新为 DISPENSED</li>
     *   <li>所有药品库存已扣减</li>
     *   <li>记录发药人和发药时间</li>
     * </ul>
     *
     * @param id 处方ID
     * @param dispenseBy 发药人ID
     * @throws IllegalArgumentException 如果处方不存在
     * @throws IllegalStateException 如果处方状态不是已缴费
     * @throws IllegalStateException 如果处方明细为空
     * @throws IllegalStateException 如果药品库存不足
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispense(Long id, Long dispenseBy) {
        log.info("开始发药，处方ID: {}, 发药人ID: {}", id, dispenseBy);

        // 查询处方
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("处方不存在，ID: " + id));

        // 必须是已缴费状态才能发药
        if (!PrescriptionStatusEnum.PAID.getCode().equals(prescription.getStatus())) {
            throw new IllegalStateException("只有已缴费状态的处方才能发药，当前状态: " + prescription.getStatus());
        }

        // 直接查询处方明细列表
        List<PrescriptionDetail> details = prescriptionDetailRepository.findByPrescription_MainIdAndIsDeletedOrderBySortOrder(id, (short) 0);
        if (details == null || details.isEmpty()) {
            throw new IllegalStateException("处方明细为空，无法发药");
        }

        for (PrescriptionDetail detail : details) {
            Medicine medicine = medicineRepository.findById(detail.getMedicine().getMainId())
                    .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + detail.getMedicine().getMainId()));

            if (medicine.getStockQuantity() < detail.getQuantity()) {
                throw new IllegalStateException("药品 [" + medicine.getName() + "] 库存不足，当前库存: " 
                        + medicine.getStockQuantity() + ", 需求数量: " + detail.getQuantity());
            }

            medicine.setStockQuantity(medicine.getStockQuantity() - detail.getQuantity());
            medicine.setUpdatedAt(LocalDateTime.now());
            medicineRepository.save(medicine);
            
            log.info("药品库存已扣减：药品ID={}, 名称={}, 扣减数量={}, 剩余库存={}",
                    medicine.getMainId(), medicine.getName(), detail.getQuantity(), medicine.getStockQuantity());
        }

        prescription.setStatus(PrescriptionStatusEnum.DISPENSED.getCode()); // 3=已发药
        prescription.setDispenseBy(dispenseBy);
        prescription.setDispenseTime(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        log.info("发药成功，处方ID: {}", id);
    }

    /**
     * 退药
     *
     * <p>退回已发放的药品并恢复库存</p>
     *
     * <p><b>业务流程：</b></p>
     * <ol>
     *   <li>查询处方</li>
     *   <li>恢复所有药品的库存</li>
     *   <li>更新处方状态为 REFUNDED（已退药/已退费）</li>
     *   <li>记录退药原因和退药时间</li>
     * </ol>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只有状态为 DISPENSED（已发药）的处方才能退药</li>
     *   <li>退药时恢复所有药品的库存</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>处方存在</li>
     *   <li>处方状态为 DISPENSED（已发药）</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>处方状态更新为 REFUNDED</li>
     *   <li>所有药品库存已恢复</li>
     *   <li>记录退药原因和退药时间</li>
     * </ul>
     *
     * @param id 处方ID
     * @param reason 退药原因
     * @throws IllegalArgumentException 如果处方不存在
     * @throws IllegalStateException 如果处方状态不是已发药
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnMedicine(Long id, String reason) {
        log.info("开始退药，处方ID: {}, 原因: {}", id, reason);

        Prescription prescription = getById(id);

        if (!PrescriptionStatusEnum.DISPENSED.getCode().equals(prescription.getStatus())) {
            throw new IllegalStateException("只有已发药状态的处方才能退药，当前状态: " + prescription.getStatus());
        }

        // 调用抽取出来的恢复库存逻辑
        restoreInventoryOnly(id);

        prescription.setStatus(PrescriptionStatusEnum.REFUNDED.getCode()); // 4=已退费(退药)
        prescription.setReturnReason(reason);
        prescription.setReturnTime(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        log.info("退药成功，处方ID: {}", id);
    }

    /**
     * 恢复处方库存（仅库存操作）
     *
     * <p>恢复处方中所有药品的库存，不修改处方状态</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>仅恢复库存，不修改处方状态</li>
     *   <li>适用于退费、退药等场景</li>
     *   <li>恢复数量 = 原发药数量</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>处方退费：已发药处方的退费流程中调用</li>
     *   <li>处方退药：退药时恢复库存</li>
     * </ul>
     *
     * @param id 处方ID
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreInventoryOnly(Long id) {
        log.info("恢复处方库存，处方ID: {}", id);

        // 直接查询处方明细列表
        List<PrescriptionDetail> details = prescriptionDetailRepository.findByPrescription_MainIdAndIsDeletedOrderBySortOrder(id, (short) 0);

        if (details != null && !details.isEmpty()) {
            for (PrescriptionDetail detail : details) {
                Medicine medicine = medicineRepository.findById(detail.getMedicine().getMainId())
                        .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + detail.getMedicine().getMainId()));

                medicine.setStockQuantity(medicine.getStockQuantity() + detail.getQuantity());
                medicine.setUpdatedAt(LocalDateTime.now());
                medicineRepository.save(medicine);

                log.info("药品库存已恢复：药品ID={}, 名称={}, 恢复数量={}, 当前库存={}",
                        medicine.getMainId(), medicine.getName(), detail.getQuantity(), medicine.getStockQuantity());
            }
        }
    }

    /**
     * 获取药师工作统计
     *
     * <p>统计指定药师在当天的发药工作量</p>
     *
     * <p><b>统计范围：</b></p>
     * <ul>
     *   <li>时间范围：当天 00:00:00 至 23:59:59</li>
     *   <li>发药单数：该药师当天发药的处方数量</li>
     *   <li>总金额：该药师当天发药的处方总金额</li>
     *   <li>药品总数：该药师当天发放的药品总数量</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>药师工作绩效考核</li>
     *   <li>发药工作量统计</li>
     * </ul>
     *
     * @param pharmacistId 药师ID
     * @return 药师统计结果（包含发药单数、总金额、药品总数）
     * @throws IllegalArgumentException 如果药师ID为空
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public com.his.dto.PharmacistStatisticsDTO getPharmacistStatistics(Long pharmacistId) {
        log.info("获取药师工作统计，药师ID: {}", pharmacistId);

        if (pharmacistId == null) {
            throw new IllegalArgumentException("药师ID不能为空");
        }

        LocalDateTime startTime = java.time.LocalDate.now().atStartOfDay();
        LocalDateTime endTime = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);

        com.his.dto.PharmacistStatisticsDTO stats = prescriptionRepository.getPharmacistStatistics(pharmacistId, startTime, endTime);
        
        if (stats == null) {
            return new com.his.dto.PharmacistStatisticsDTO();
        }
        
        log.info("统计结果: 发药单数={}, 总金额={}, 药品总数={}", 
                stats.getDispensedCount(), stats.getTotalAmount(), stats.getTotalItems());
        
        return stats;
    }

    private void validatePrescriptionDTO(PrescriptionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("处方数据不能为空");
        }
        if (dto.getRegistrationId() == null) {
            throw new IllegalArgumentException("挂号单ID不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("处方药品列表不能为空");
        }
        for (PrescriptionDTO.PrescriptionItemDTO item : dto.getItems()) {
            if (item.getMedicineId() == null) {
                throw new IllegalArgumentException("药品ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("药品数量必须大于0");
            }
        }
    }

    private Prescription createPrescriptionEntity(Registration registration, MedicalRecord medicalRecord, PrescriptionDTO dto) {
        Prescription prescription = new Prescription();
        String prescriptionNo = generatePrescriptionNo();
        prescription.setPrescriptionNo(prescriptionNo);
        prescription.setMedicalRecord(medicalRecord);
        prescription.setPatient(registration.getPatient());
        prescription.setDoctor(registration.getDoctor());
        prescription.setPrescriptionType(dto.getPrescriptionType() != null ? dto.getPrescriptionType() : (short) 1);
        prescription.setValidityDays(dto.getValidityDays() != null ? dto.getValidityDays() : 3);
        prescription.setStatus(PrescriptionStatusEnum.DRAFT.getCode()); // 0=草稿
        prescription.setIsDeleted((short) 0);
        prescription.setCreatedAt(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        return prescription;
    }

    private PrescriptionDetail createPrescriptionDetail(
            Prescription prescription,
            Medicine medicine,
            PrescriptionDTO.PrescriptionItemDTO itemDTO,
            BigDecimal unitPrice,
            BigDecimal subtotal) {

        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setMedicine(medicine);
        detail.setMedicineName(medicine.getName());
        detail.setUnitPrice(unitPrice);
        detail.setQuantity(itemDTO.getQuantity());
        detail.setSubtotal(subtotal);
        detail.setSpecification(medicine.getSpecification());
        detail.setUnit(medicine.getUnit());
        detail.setFrequency(itemDTO.getFrequency());
        detail.setDosage(itemDTO.getDosage());
        detail.setRoute(itemDTO.getRoute());
        detail.setDays(itemDTO.getDays());
        detail.setInstructions(itemDTO.getInstructions());
        detail.setIsDeleted((short) 0);
        detail.setCreatedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        return detail;
    }

    /**
     * 生成处方号（线程安全）
     *
     * <p>使用数据库序列生成唯一编号，避免并发冲突</p>
     * <p>格式：PRE + yyyyMMdd + 6位序列号</p>
     *
     * @return 唯一的处方号
     */
    private String generatePrescriptionNo() {
        try {
            String prescriptionNo = prescriptionRepository.generatePrescriptionNo();
            log.debug("生成处方号: {}", prescriptionNo);
            return prescriptionNo;
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("数据库访问失败，无法生成处方号", e);
            throw new IllegalStateException("生成处方号失败：数据库错误 - " + e.getMostSpecificCause().getMessage(), e);
        } catch (RuntimeException e) {
            log.error("生成处方号失败", e);
            throw new IllegalStateException("生成处方号失败：" + e.getMessage(), e);
        }
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
     * @param prescription 要初始化的处方实体
     */
    @SuppressWarnings("unused")
    private void initializeLazyFields(Prescription prescription) {
        // 在事务内主动触发 SQL 查询，加载关联实体
        // 这样即使事务关闭后，这些字段仍然可用
        if (prescription.getMedicalRecord() != null) {
            Long mainId = prescription.getMedicalRecord().getMainId(); // 触发 MedicalRecord 加载
            if (prescription.getMedicalRecord().getPatient() != null) {
                String patientName = prescription.getMedicalRecord().getPatient().getName(); // 级联加载 Patient
            }
            if (prescription.getMedicalRecord().getDoctor() != null) {
                String doctorName = prescription.getMedicalRecord().getDoctor().getName(); // 级联加载 Doctor
            }
        }
        if (prescription.getPatient() != null) {
            String patientName = prescription.getPatient().getName(); // 直接加载 Patient
        }
        if (prescription.getDoctor() != null) {
            String doctorName = prescription.getDoctor().getName(); // 直接加载 Doctor
        }
        if (prescription.getDetails() != null) {
            int size = prescription.getDetails().size(); // 触发 Details 集合加载
            prescription.getDetails().forEach(detail -> {
                if (detail.getMedicine() != null) {
                    //noinspection unused
                    String medicineName = detail.getMedicine().getName(); // 级联加载 Medicine
                }
            });
        }
    }
}