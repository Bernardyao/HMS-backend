package com.his.converter;

import com.his.entity.Medicine;
import com.his.entity.Prescription;
import com.his.entity.MedicalRecord;
import com.his.entity.Registration;
import com.his.enums.RegStatusEnum;
import com.his.vo.MedicineVO;
import com.his.vo.PrescriptionVO;
import com.his.vo.MedicalRecordVO;
import com.his.vo.RegistrationVO;
import com.his.vo.views.MedicineViews;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VO转换工具类
 * 统一处理Entity到VO的转换逻辑，消除代码重复
 *
 * @author HIS Team
 * @since 2025-12-27
 */
@Slf4j
public class VoConverter {

    private VoConverter() {
        // 工具类，禁止实例化
    }

    /**
     * Medicine实体转MedicineVO（默认使用Public视图）
     *
     * @param medicine 药品实体
     * @return MedicineVO
     */
    public static MedicineVO toMedicineVO(Medicine medicine) {
        return toMedicineVO(medicine, MedicineViews.Public.class);
    }

    /**
     * Medicine实体转MedicineVO（支持JsonView）
     * <p>
     * 根据指定的视图类型返回不同字段的数据，避免敏感信息泄露
     * </p>
     *
     * <h3>视图类型说明</h3>
     * <ul>
     *   <li><b>Public</b>: 公共视图 - 所有认证用户可见（基础信息）</li>
     *   <li><b>Doctor</b>: 医生视图 - 医生和药师可见（含用法用量等）</li>
     *   <li><b>Pharmacist</b>: 药师视图 - 仅药师可见（含进货价等敏感信息）</li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>
     * {@code
     * // 公共视图（基础信息）
     * MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Public.class);
     *
     * // 医生视图（含详细信息）
     * MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);
     *
     * // 药师视图（含敏感信息）
     * MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);
     * }
     * </pre>
     *
     * @param medicine 药品实体
     * @param viewClass JsonView类型（Public/Doctor/Pharmacist）
     * @return MedicineVO
     */
    public static MedicineVO toMedicineVO(Medicine medicine, Class<?> viewClass) {
        if (medicine == null) {
            return null;
        }

        // 创建基础Builder（Public视图字段）
        // 注意：Medicine实体使用Short，但MedicineVO使用Integer（更好的JSON序列化兼容性）
        MedicineVO.MedicineVOBuilder builder = MedicineVO.builder()
            .mainId(medicine.getMainId())
            .medicineCode(medicine.getMedicineCode())
            .name(medicine.getName())
            .genericName(medicine.getGenericName())
            .retailPrice(medicine.getRetailPrice())
            .stockQuantity(medicine.getStockQuantity())
            .category(medicine.getCategory())
            .isPrescription(medicine.getIsPrescription() != null ? medicine.getIsPrescription().intValue() : null)
            .status(medicine.getStatus() != null ? medicine.getStatus().intValue() : null);

        // Doctor视图及以上：添加详细信息
        if (viewClass != MedicineViews.Public.class) {
            builder
                .specification(medicine.getSpecification())
                .unit(medicine.getUnit())
                .dosageForm(medicine.getDosageForm())
                .manufacturer(medicine.getManufacturer())
                .stockStatus(computeStockStatus(medicine));
        }

        // Pharmacist视图：添加敏感信息
        if (viewClass == MedicineViews.Pharmacist.class) {
            builder
                .purchasePrice(medicine.getPurchasePrice())
                .minStock(medicine.getMinStock())
                .maxStock(medicine.getMaxStock())
                .storageCondition(medicine.getStorageCondition())
                .approvalNo(medicine.getApprovalNo())
                .expiryWarningDays(medicine.getExpiryWarningDays())
                .profitMargin(computeProfitMargin(medicine))
                .createdAt(medicine.getCreatedAt())
                .updatedAt(medicine.getUpdatedAt());
        }

        return builder.build();
    }

    /**
     * Prescription实体转PrescriptionVO
     * 用于医生工作站和药师工作站
     *
     * @param prescription 处方实体
     * @return PrescriptionVO
     */
    public static PrescriptionVO toPrescriptionVO(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        return PrescriptionVO.builder()
            .mainId(prescription.getMainId())
            .prescriptionNo(prescription.getPrescriptionNo())
            .recordId(prescription.getMedicalRecord() != null ? prescription.getMedicalRecord().getMainId() : null)
            .patientId(prescription.getPatient() != null ? prescription.getPatient().getMainId() : null)
            .patientName(prescription.getPatient() != null ? prescription.getPatient().getName() : null)
            .doctorId(prescription.getDoctor() != null ? prescription.getDoctor().getMainId() : null)
            .doctorName(prescription.getDoctor() != null ? prescription.getDoctor().getName() : null)
            .prescriptionType(prescription.getPrescriptionType())
            .totalAmount(prescription.getTotalAmount())
            .itemCount(prescription.getItemCount())
            .status(prescription.getStatus())
            .validityDays(prescription.getValidityDays())
            .reviewDoctorId(prescription.getReviewDoctor() != null ? prescription.getReviewDoctor().getMainId() : null)
            .reviewDoctorName(prescription.getReviewDoctor() != null ? prescription.getReviewDoctor().getName() : null)
            .reviewTime(prescription.getReviewTime())
            .reviewRemark(prescription.getReviewRemark())
            .dispenseTime(prescription.getDispenseTime())
            .dispenseBy(prescription.getDispenseBy())
            .createdAt(prescription.getCreatedAt())
            .updatedAt(prescription.getUpdatedAt())
            .build();
    }

    /**
     * MedicalRecord实体转MedicalRecordVO
     *
     * @param record 病历实体
     * @return MedicalRecordVO
     */
    public static MedicalRecordVO toMedicalRecordVO(MedicalRecord record) {
        if (record == null) {
            return null;
        }

        return MedicalRecordVO.builder()
            .mainId(record.getMainId())
            .recordNo(record.getRecordNo())
            .registrationId(record.getRegistration() != null ? record.getRegistration().getMainId() : null)
            .patientId(record.getPatient() != null ? record.getPatient().getMainId() : null)
            .patientName(record.getPatient() != null ? record.getPatient().getName() : null)
            .doctorId(record.getDoctor() != null ? record.getDoctor().getMainId() : null)
            .doctorName(record.getDoctor() != null ? record.getDoctor().getName() : null)
            .chiefComplaint(record.getChiefComplaint())
            .presentIllness(record.getPresentIllness())
            .pastHistory(record.getPastHistory())
            .personalHistory(record.getPersonalHistory())
            .familyHistory(record.getFamilyHistory())
            .physicalExam(record.getPhysicalExam())
            .auxiliaryExam(record.getAuxiliaryExam())
            .diagnosis(record.getDiagnosis())
            .diagnosisCode(record.getDiagnosisCode())
            .treatmentPlan(record.getTreatmentPlan())
            .doctorAdvice(record.getDoctorAdvice())
            .status(record.getStatus())
            .visitTime(record.getVisitTime())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
    }

    /**
     * Registration实体转RegistrationVO
     * 包含防御性编程，处理null值
     *
     * @param registration 挂号实体
     * @return RegistrationVO
     */
    public static RegistrationVO toRegistrationVO(Registration registration) {
        if (registration == null) {
            log.error("convertToVO失败: registration为null");
            throw new IllegalArgumentException("挂号记录不能为空");
        }

        RegistrationVO vo = new RegistrationVO();
        vo.setId(registration.getMainId());
        vo.setRegNo(registration.getRegNo());

        // 防御性编程: 检查患者信息
        if (registration.getPatient() != null) {
            vo.setPatientId(registration.getPatient().getMainId());
            vo.setPatientName(registration.getPatient().getName());
            vo.setGender(registration.getPatient().getGender());
            vo.setAge(registration.getPatient().getAge());
        } else {
            log.warn("挂号记录缺少患者信息,挂号ID: {}", registration.getMainId());
        }

        // 防御性编程: 检查科室信息
        if (registration.getDepartment() != null) {
            vo.setDeptId(registration.getDepartment().getMainId());
            vo.setDeptName(registration.getDepartment().getName());
        } else {
            log.warn("挂号记录缺少科室信息,挂号ID: {}", registration.getMainId());
        }

        // 防御性编程: 检查医生信息
        if (registration.getDoctor() != null) {
            vo.setDoctorId(registration.getDoctor().getMainId());
            vo.setDoctorName(registration.getDoctor().getName());
        } else {
            log.warn("挂号记录缺少医生信息,挂号ID: {}", registration.getMainId());
        }

        vo.setStatus(registration.getStatus());

        // 安全地获取状态描述
        try {
            if (registration.getStatus() != null) {
                vo.setStatusDesc(RegStatusEnum.fromCode(registration.getStatus()).getDescription());
            }
        } catch (Exception e) {
            log.warn("无法解析挂号状态,挂号ID: {}, 状态: {}", registration.getMainId(), registration.getStatus());
        }

        vo.setVisitDate(registration.getVisitDate());
        vo.setRegistrationFee(registration.getRegistrationFee());
        vo.setQueueNo(registration.getQueueNo());
        vo.setAppointmentTime(registration.getAppointmentTime());
        vo.setCreatedAt(registration.getCreatedAt());

        return vo;
    }

    /**
     * 计算库存状态
     * <p>
     * 库存状态判断规则：
     * <ul>
     *   <li>OUT_OF_STOCK: 库存 = 0（缺货）</li>
     *   <li>LOW_STOCK: 库存 <= 最低库存（低库存）</li>
     *   <li>IN_STOCK: 库存 > 最低库存（正常）</li>
     * </ul>
     * </p>
     *
     * @param medicine 药品实体
     * @return 库存状态字符串
     */
    private static String computeStockStatus(Medicine medicine) {
        if (medicine.getStockQuantity() == null) {
            return "OUT_OF_STOCK";
        }

        if (medicine.getStockQuantity() == 0) {
            return "OUT_OF_STOCK";
        } else if (medicine.getMinStock() != null &&
                   medicine.getStockQuantity() <= medicine.getMinStock()) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }

    /**
     * 计算利润率
     * <p>
     * 利润率计算公式：
     * <pre>
     * 利润率 = ((零售价 - 进货价) / 进货价) × 100%
     * </pre>
     * 如果进货价为null或0，则返回0。
     * </p>
     *
     * @param medicine 药品实体
     * @return 利润率（百分比），保留2位小数
     */
    private static BigDecimal computeProfitMargin(Medicine medicine) {
        if (medicine.getPurchasePrice() == null ||
            medicine.getPurchasePrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return medicine.getRetailPrice()
            .subtract(medicine.getPurchasePrice())
            .multiply(new BigDecimal("100"))
            .divide(medicine.getPurchasePrice(), 2, RoundingMode.HALF_UP);
    }
}
