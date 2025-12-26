package com.his.service.impl;

import com.his.dto.PrescriptionDTO;
import com.his.entity.*;
import com.his.repository.*;
import com.his.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 处方服务实现类
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
     * 核心业务逻辑：
     * 1. 接收挂号单ID和药品列表
     * 2. 遍历药品列表，从数据库查出当前单价（防止前端篡改价格）
     * 3. 计算总金额
     * 4. 组装并保存处方主表和详情表（使用 @Transactional 确保原子性）
     * 注意：暂时不扣减库存，库存扣减在发药阶段进行
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Prescription createPrescription(PrescriptionDTO dto) {
        log.info("开始创建处方，挂号单ID: {}", dto.getRegistrationId());

        // 1. 参数校验
        validatePrescriptionDTO(dto);

        // 2. 查询挂号单信息
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        // 3. 查询或创建病历
        MedicalRecord medicalRecord = medicalRecordRepository
                .findByRegistration_MainIdAndIsDeleted(dto.getRegistrationId(), (short) 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "请先创建病历再开处方，挂号单ID: " + dto.getRegistrationId()));

        // 4. 创建处方主表
        Prescription prescription = createPrescriptionEntity(registration, medicalRecord, dto);

        // 5. 创建处方明细（从数据库读取单价，防止前端篡改）
        List<PrescriptionDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;

        for (PrescriptionDTO.PrescriptionItemDTO itemDTO : dto.getItems()) {
            // 从数据库查询药品，获取真实单价
            Medicine medicine = medicineRepository.findById(itemDTO.getMedicineId())
                    .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + itemDTO.getMedicineId()));

            if (medicine.getIsDeleted() == 1) {
                throw new IllegalArgumentException("药品已被删除，ID: " + itemDTO.getMedicineId());
            }

            if (medicine.getStatus() != 1) {
                throw new IllegalArgumentException("药品已停用，ID: " + itemDTO.getMedicineId());
            }

            // 使用数据库中的单价（这是防止价格篡改的关键）
            BigDecimal unitPrice = medicine.getRetailPrice();
            int quantity = itemDTO.getQuantity();

            // 计算小计 = 单价 × 数量
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);

            // 累计总金额和数量
            totalAmount = totalAmount.add(subtotal);
            itemCount += quantity;

            // 创建处方明细
            PrescriptionDetail detail = createPrescriptionDetail(prescription, medicine, itemDTO, unitPrice, subtotal);
            details.add(detail);

            log.info("添加处方明细：药品ID={}, 名称={}, 单价={}, 数量={}, 小计={}",
                    medicine.getMainId(), medicine.getName(), unitPrice, quantity, subtotal);
        }

        // 6. 设置处方总金额和药品数量
        prescription.setTotalAmount(totalAmount);
        prescription.setItemCount(itemCount);
        prescription.setStatus((short) 1); // 状态设为"已开方"

        // 7. 保存处方主表
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("处方主表保存成功，ID: {}, 处方号: {}, 总金额: {}, 药品数量: {}",
                savedPrescription.getMainId(), savedPrescription.getPrescriptionNo(),
                savedPrescription.getTotalAmount(), savedPrescription.getItemCount());

        // 8. 关联处方主表到明细，并保存明细
        for (PrescriptionDetail detail : details) {
            detail.setPrescription(savedPrescription);
        }
        prescriptionDetailRepository.saveAll(details);
        log.info("处方明细保存成功，共 {} 条", details.size());

        // 初始化懒加载字段
        initializeLazyFields(savedPrescription);

        return savedPrescription;
    }

    /**
     * 根据ID查询处方
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

        // 初始化懒加载字段
        initializeLazyFields(prescription);

        return prescription;
    }

    /**
     * 根据病历ID查询处方列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getByRecordId(Long recordId) {
        log.info("根据病历ID查询处方列表，病历ID: {}", recordId);

        if (recordId == null) {
            throw new IllegalArgumentException("病历ID不能为空");
        }

        List<Prescription> prescriptions = prescriptionRepository.findByMedicalRecord_MainIdAndIsDeleted(recordId, (short) 0);
        
        // 初始化每个处方的懒加载字段
        prescriptions.forEach(this::initializeLazyFields);
        
        return prescriptions;
    }

    /**
     * 审核处方
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Long reviewDoctorId, String remark) {
        log.info("审核处方，ID: {}, 审核医生ID: {}", id, reviewDoctorId);

        Prescription prescription = getById(id);

        if (prescription.getStatus() != 1) {
            throw new IllegalStateException("只有已开方状态的处方才能审核");
        }

        // 设置审核信息
        prescription.setStatus((short) 2); // 状态改为"已审核"
        prescription.setReviewTime(LocalDateTime.now());
        prescription.setReviewRemark(remark);
        prescription.setUpdatedAt(LocalDateTime.now());

        // 设置审核医生ID（需要从Doctor实体获取）
        // 这里简化处理，直接设置ID
        // 在实际项目中，应该从DoctorRepository查询Doctor对象并设置
        // prescription.setReviewDoctor(reviewDoctor);

        prescriptionRepository.save(prescription);

        log.info("处方审核成功，ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> getPendingDispenseList() {
        log.info("查询待发药处方列表");
        List<Prescription> prescriptions = prescriptionRepository.findByStatusAndIsDeleted((short) 2, (short) 0);
        prescriptions.forEach(this::initializeLazyFields);
        return prescriptions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispense(Long id, Long dispenseBy) {
        log.info("开始发药，处方ID: {}, 发药人ID: {}", id, dispenseBy);

        Prescription prescription = getById(id);

        if (prescription.getStatus() != 2) {
            throw new IllegalStateException("只有已审核通过的处方才能发药，当前状态: " + prescription.getStatus());
        }

        // 1. 扣减药品库存
        List<PrescriptionDetail> details = prescription.getDetails();
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

            // 扣减库存
            medicine.setStockQuantity(medicine.getStockQuantity() - detail.getQuantity());
            medicine.setUpdatedAt(LocalDateTime.now());
            medicineRepository.save(medicine);
            
            log.info("药品库存已扣减：药品ID={}, 名称={}, 扣减数量={}, 剩余库存={}",
                    medicine.getMainId(), medicine.getName(), detail.getQuantity(), medicine.getStockQuantity());
        }

        // 2. 更新处方状态
        prescription.setStatus((short) 3); // 已发药
        prescription.setDispenseBy(dispenseBy);
        prescription.setDispenseTime(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        log.info("发药成功，处方ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnMedicine(Long id, String reason) {
        log.info("开始退药，处方ID: {}, 原因: {}", id, reason);

        Prescription prescription = getById(id);

        if (prescription.getStatus() != 3) {
            throw new IllegalStateException("只有已发药状态的处方才能退药，当前状态: " + prescription.getStatus());
        }

        // 1. 恢复药品库存
        List<PrescriptionDetail> details = prescription.getDetails();
        if (details != null && !details.isEmpty()) {
            for (PrescriptionDetail detail : details) {
                Medicine medicine = medicineRepository.findById(detail.getMedicine().getMainId())
                        .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + detail.getMedicine().getMainId()));

                // 恢复库存
                medicine.setStockQuantity(medicine.getStockQuantity() + detail.getQuantity());
                medicine.setUpdatedAt(LocalDateTime.now());
                medicineRepository.save(medicine);

                log.info("药品库存已恢复：药品ID={}, 名称={}, 恢复数量={}, 当前库存={}",
                        medicine.getMainId(), medicine.getName(), detail.getQuantity(), medicine.getStockQuantity());
            }
        }

        // 2. 更新处方状态
        prescription.setStatus((short) 4); // 已退药
        prescription.setReturnReason(reason);
        prescription.setReturnTime(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        log.info("退药成功，处方ID: {}", id);
    }

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
        
        // 如果查询结果为null（虽然使用了COALESCE，但以防万一），返回空对象
        if (stats == null) {
            return new com.his.dto.PharmacistStatisticsDTO();
        }
        
        log.info("统计结果: 发药单数={}, 总金额={}, 药品总数={}", 
                stats.getDispensedCount(), stats.getTotalAmount(), stats.getTotalItems());
        
        return stats;
    }

    /**
     * 参数校验
     */
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

        // 校验每个药品明细
        for (PrescriptionDTO.PrescriptionItemDTO item : dto.getItems()) {
            if (item.getMedicineId() == null) {
                throw new IllegalArgumentException("药品ID不能为空");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("药品数量必须大于0");
            }
        }
    }

    /**
     * 创建处方实体
     */
    private Prescription createPrescriptionEntity(Registration registration, MedicalRecord medicalRecord, PrescriptionDTO dto) {
        Prescription prescription = new Prescription();

        // 生成处方号
        String prescriptionNo = generatePrescriptionNo();
        prescription.setPrescriptionNo(prescriptionNo);

        // 设置关联
        prescription.setMedicalRecord(medicalRecord);
        prescription.setPatient(registration.getPatient());
        prescription.setDoctor(registration.getDoctor());

        // 设置处方类型和有效期
        prescription.setPrescriptionType(dto.getPrescriptionType() != null ? dto.getPrescriptionType() : (short) 1);
        prescription.setValidityDays(dto.getValidityDays() != null ? dto.getValidityDays() : 3);

        // 设置状态和时间
        prescription.setStatus((short) 0); // 初始状态为草稿，后面会改为已开方
        prescription.setIsDeleted((short) 0);
        prescription.setCreatedAt(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());

        return prescription;
    }

    /**
     * 创建处方明细
     */
    private PrescriptionDetail createPrescriptionDetail(
            Prescription prescription,
            Medicine medicine,
            PrescriptionDTO.PrescriptionItemDTO itemDTO,
            BigDecimal unitPrice,
            BigDecimal subtotal) {

        PrescriptionDetail detail = new PrescriptionDetail();

        // 设置关联
        detail.setMedicine(medicine);
        detail.setMedicineName(medicine.getName());

        // 设置价格和数量（使用从数据库查询的单价）
        detail.setUnitPrice(unitPrice);
        detail.setQuantity(itemDTO.getQuantity());
        detail.setSubtotal(subtotal);

        // 设置药品规格信息
        detail.setSpecification(medicine.getSpecification());
        detail.setUnit(medicine.getUnit());

        // 设置用药信息
        detail.setFrequency(itemDTO.getFrequency());
        detail.setDosage(itemDTO.getDosage());
        detail.setRoute(itemDTO.getRoute());
        detail.setDays(itemDTO.getDays());
        detail.setInstructions(itemDTO.getInstructions());

        // 设置状态和时间
        detail.setIsDeleted((short) 0);
        detail.setCreatedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());

        return detail;
    }

    /**
     * 生成处方号
     * 格式：PRE + yyyyMMddHHmmss + 随机3位数
     */
    private String generatePrescriptionNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "PRE" + timestamp + random;
    }

    /**
     * 初始化懒加载字段，避免LazyInitializationException
     */
    private void initializeLazyFields(Prescription prescription) {
        if (prescription.getMedicalRecord() != null) {
            prescription.getMedicalRecord().getMainId();
            if (prescription.getMedicalRecord().getPatient() != null) {
                prescription.getMedicalRecord().getPatient().getName();
            }
            if (prescription.getMedicalRecord().getDoctor() != null) {
                prescription.getMedicalRecord().getDoctor().getName();
            }
        }
        if (prescription.getPatient() != null) {
            prescription.getPatient().getName();
        }
        if (prescription.getDoctor() != null) {
            prescription.getDoctor().getName();
        }
        // 初始化处方明细列表
        if (prescription.getDetails() != null) {
            prescription.getDetails().size();
            prescription.getDetails().forEach(detail -> {
                if (detail.getMedicine() != null) {
                    detail.getMedicine().getName();
                }
            });
        }
    }
}
