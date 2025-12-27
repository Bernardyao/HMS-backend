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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispense(Long id, Long dispenseBy) {
        log.info("开始发药，处方ID: {}, 发药人ID: {}", id, dispenseBy);

        Prescription prescription = getById(id);

        // 必须是已缴费状态才能发药
        if (!PrescriptionStatusEnum.PAID.getCode().equals(prescription.getStatus())) {
            throw new IllegalStateException("只有已缴费状态的处方才能发药，当前状态: " + prescription.getStatus());
        }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreInventoryOnly(Long id) {
        log.info("恢复处方库存，处方ID: {}", id);
        
        Prescription prescription = getById(id);
        List<PrescriptionDetail> details = prescription.getDetails();
        
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

    private String generatePrescriptionNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "PRE" + timestamp + random;
    }

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