package com.his.testutils;

import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 测试数据构建器工具类
 * <p>
 * 提供统一的测试数据创建方法，避免测试代码中的重复。
 * 所有构建器支持链式调用，并提供合理的默认值。
 *
 * @author HIS 开发团队
 */
public class TestDataBuilders {

    /**
     * 患者实体构建器
     */
    public static class PatientBuilder {
        private final Patient patient = new Patient();

        private PatientBuilder() {
            // 设置默认值
            patient.setMainId(1L);
            patient.setPatientNo("P" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            patient.setName("测试患者");
            patient.setGender((short) 1);
            patient.setAge((short) 30);
            patient.setIdCard("110101199001011001");
            patient.setPhone("13800138000");
            patient.setBirthDate(LocalDate.of(1990, 1, 1));
            patient.setAddress("测试地址");
            patient.setIsDeleted((short) 0);
        }

        public static PatientBuilder builder() {
            return new PatientBuilder();
        }

        public PatientBuilder mainId(Long mainId) {
            patient.setMainId(mainId);
            return this;
        }

        public PatientBuilder patientNo(String patientNo) {
            patient.setPatientNo(patientNo);
            return this;
        }

        public PatientBuilder name(String name) {
            patient.setName(name);
            return this;
        }

        public PatientBuilder idCard(String idCard) {
            patient.setIdCard(idCard);
            return this;
        }

        public PatientBuilder gender(Short gender) {
            patient.setGender(gender);
            return this;
        }

        public PatientBuilder age(Short age) {
            patient.setAge(age);
            return this;
        }

        public PatientBuilder phone(String phone) {
            patient.setPhone(phone);
            return this;
        }

        public PatientBuilder isDeleted(Short isDeleted) {
            patient.setIsDeleted(isDeleted);
            return this;
        }

        public Patient build() {
            return patient;
        }
    }

    /**
     * 科室实体构建器
     */
    public static class DepartmentBuilder {
        private final Department department = new Department();

        private DepartmentBuilder() {
            // 设置默认值
            department.setMainId(1L);
            department.setDeptCode("D" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            department.setName("测试科室");
            department.setStatus((short) 1);
            department.setIsDeleted((short) 0);
            department.setSortOrder(1);
        }

        public static DepartmentBuilder builder() {
            return new DepartmentBuilder();
        }

        public DepartmentBuilder mainId(Long mainId) {
            department.setMainId(mainId);
            return this;
        }

        public DepartmentBuilder deptCode(String deptCode) {
            department.setDeptCode(deptCode);
            return this;
        }

        public DepartmentBuilder name(String name) {
            department.setName(name);
            return this;
        }

        public DepartmentBuilder status(Short status) {
            department.setStatus(status);
            return this;
        }

        public DepartmentBuilder isDeleted(Short isDeleted) {
            department.setIsDeleted(isDeleted);
            return this;
        }

        public DepartmentBuilder sortOrder(Integer sortOrder) {
            department.setSortOrder(sortOrder);
            return this;
        }

        public DepartmentBuilder parent(Department parent) {
            department.setParent(parent);
            return this;
        }

        public Department build() {
            return department;
        }
    }

    /**
     * 医生实体构建器
     */
    public static class DoctorBuilder {
        private final Doctor doctor = new Doctor();

        private DoctorBuilder() {
            // 设置默认值
            doctor.setMainId(1L);
            doctor.setDoctorNo("DOC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            doctor.setName("测试医生");
            doctor.setGender((short) 1);
            doctor.setTitle("主任医师");
            doctor.setStatus((short) 1);
            doctor.setIsDeleted((short) 0);
        }

        public static DoctorBuilder builder() {
            return new DoctorBuilder();
        }

        public DoctorBuilder mainId(Long mainId) {
            doctor.setMainId(mainId);
            return this;
        }

        public DoctorBuilder doctorNo(String doctorNo) {
            doctor.setDoctorNo(doctorNo);
            return this;
        }

        public DoctorBuilder name(String name) {
            doctor.setName(name);
            return this;
        }

        public DoctorBuilder gender(Short gender) {
            doctor.setGender(gender);
            return this;
        }

        public DoctorBuilder title(String title) {
            doctor.setTitle(title);
            return this;
        }

        public DoctorBuilder department(Department department) {
            doctor.setDepartment(department);
            return this;
        }

        public DoctorBuilder status(Short status) {
            doctor.setStatus(status);
            return this;
        }

        public DoctorBuilder isDeleted(Short isDeleted) {
            doctor.setIsDeleted(isDeleted);
            return this;
        }

        public Doctor build() {
            return doctor;
        }
    }

    /**
     * 挂号实体构建器
     */
    public static class RegistrationBuilder {
        private final Registration registration = new Registration();

        private RegistrationBuilder() {
            // 设置默认值
            registration.setMainId(1L);
            registration.setRegNo("R" + System.currentTimeMillis());
            registration.setVisitDate(LocalDate.now());
            registration.setRegistrationFee(new BigDecimal("20.00"));
            registration.setStatus(RegStatusEnum.WAITING.getCode());
            registration.setVisitType((short) 1);
            registration.setIsDeleted((short) 0);
            registration.setQueueNo("001");
        }

        public static RegistrationBuilder builder() {
            return new RegistrationBuilder();
        }

        public RegistrationBuilder mainId(Long mainId) {
            registration.setMainId(mainId);
            return this;
        }

        public RegistrationBuilder regNo(String regNo) {
            registration.setRegNo(regNo);
            return this;
        }

        public RegistrationBuilder patient(Patient patient) {
            registration.setPatient(patient);
            return this;
        }

        public RegistrationBuilder department(Department department) {
            registration.setDepartment(department);
            return this;
        }

        public RegistrationBuilder doctor(Doctor doctor) {
            registration.setDoctor(doctor);
            return this;
        }

        public RegistrationBuilder visitDate(LocalDate visitDate) {
            registration.setVisitDate(visitDate);
            return this;
        }

        public RegistrationBuilder registrationFee(BigDecimal fee) {
            registration.setRegistrationFee(fee);
            return this;
        }

        public RegistrationBuilder status(Short status) {
            registration.setStatus(status);
            return this;
        }

        public RegistrationBuilder isDeleted(Short isDeleted) {
            registration.setIsDeleted(isDeleted);
            return this;
        }

        public RegistrationBuilder queueNo(String queueNo) {
            registration.setQueueNo(queueNo);
            return this;
        }

        public Registration build() {
            return registration;
        }
    }

    /**
     * 药品实体构建器
     */
    public static class MedicineBuilder {
        private final Medicine medicine = new Medicine();

        private MedicineBuilder() {
            // 设置默认值
            medicine.setMainId(1L);
            medicine.setMedicineCode("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            medicine.setName("测试药品");
            medicine.setSpecification("0.25g*24粒");
            medicine.setRetailPrice(new BigDecimal("25.00"));
            medicine.setPurchasePrice(new BigDecimal("15.00"));
            medicine.setStockQuantity(1000);
            medicine.setMinStock(100);
            medicine.setIsDeleted((short) 0);
        }

        public static MedicineBuilder builder() {
            return new MedicineBuilder();
        }

        public MedicineBuilder mainId(Long mainId) {
            medicine.setMainId(mainId);
            return this;
        }

        public MedicineBuilder medicineCode(String code) {
            medicine.setMedicineCode(code);
            return this;
        }

        public MedicineBuilder name(String name) {
            medicine.setName(name);
            return this;
        }

        public MedicineBuilder specification(String specification) {
            medicine.setSpecification(specification);
            return this;
        }

        public MedicineBuilder retailPrice(BigDecimal price) {
            medicine.setRetailPrice(price);
            return this;
        }

        public MedicineBuilder purchasePrice(BigDecimal price) {
            medicine.setPurchasePrice(price);
            return this;
        }

        public MedicineBuilder stockQuantity(Integer quantity) {
            medicine.setStockQuantity(quantity);
            return this;
        }

        public MedicineBuilder isDeleted(Short isDeleted) {
            medicine.setIsDeleted(isDeleted);
            return this;
        }

        public Medicine build() {
            return medicine;
        }
    }

    /**
     * 处方实体构建器
     */
    public static class PrescriptionBuilder {
        private final Prescription prescription = new Prescription();

        private PrescriptionBuilder() {
            // 设置默认值
            prescription.setMainId(1L);
            prescription.setPrescriptionNo("PRE" + System.currentTimeMillis());
            prescription.setPrescriptionType((short) 1);
            prescription.setTotalAmount(BigDecimal.ZERO);
            prescription.setItemCount(0);
            prescription.setStatus(PrescriptionStatusEnum.DRAFT.getCode());
            prescription.setIsDeleted((short) 0);
            prescription.setValidityDays(3);
        }

        public static PrescriptionBuilder builder() {
            return new PrescriptionBuilder();
        }

        public PrescriptionBuilder mainId(Long mainId) {
            prescription.setMainId(mainId);
            return this;
        }

        public PrescriptionBuilder prescriptionNo(String prescriptionNo) {
            prescription.setPrescriptionNo(prescriptionNo);
            return this;
        }

        public PrescriptionBuilder medicalRecord(MedicalRecord medicalRecord) {
            prescription.setMedicalRecord(medicalRecord);
            return this;
        }

        public PrescriptionBuilder patient(Patient patient) {
            prescription.setPatient(patient);
            return this;
        }

        public PrescriptionBuilder doctor(Doctor doctor) {
            prescription.setDoctor(doctor);
            return this;
        }

        public PrescriptionBuilder prescriptionType(Short prescriptionType) {
            prescription.setPrescriptionType(prescriptionType);
            return this;
        }

        public PrescriptionBuilder totalAmount(BigDecimal totalAmount) {
            prescription.setTotalAmount(totalAmount);
            return this;
        }

        public PrescriptionBuilder itemCount(Integer itemCount) {
            prescription.setItemCount(itemCount);
            return this;
        }

        public PrescriptionBuilder status(Short status) {
            prescription.setStatus(status);
            return this;
        }

        public PrescriptionBuilder validityDays(Integer validityDays) {
            prescription.setValidityDays(validityDays);
            return this;
        }

        public Prescription build() {
            return prescription;
        }
    }

    /**
     * 收费单实体构建器
     */
    public static class ChargeBuilder {
        private final Charge charge = new Charge();

        private ChargeBuilder() {
            // 设置默认值
            charge.setMainId(1L);
            charge.setChargeNo("CHG" + System.currentTimeMillis());
            charge.setChargeType((short) 1);
            charge.setTotalAmount(BigDecimal.ZERO);
            charge.setActualAmount(BigDecimal.ZERO);
            charge.setStatus(ChargeStatusEnum.UNPAID.getCode());
            charge.setIsDeleted((short) 0);
        }

        public static ChargeBuilder builder() {
            return new ChargeBuilder();
        }

        public ChargeBuilder mainId(Long mainId) {
            charge.setMainId(mainId);
            return this;
        }

        public ChargeBuilder chargeNo(String chargeNo) {
            charge.setChargeNo(chargeNo);
            return this;
        }

        public ChargeBuilder patient(Patient patient) {
            charge.setPatient(patient);
            return this;
        }

        public ChargeBuilder registration(Registration registration) {
            charge.setRegistration(registration);
            return this;
        }

        public ChargeBuilder chargeType(Short chargeType) {
            charge.setChargeType(chargeType);
            return this;
        }

        public ChargeBuilder totalAmount(BigDecimal totalAmount) {
            charge.setTotalAmount(totalAmount);
            return this;
        }

        public ChargeBuilder actualAmount(BigDecimal actualAmount) {
            charge.setActualAmount(actualAmount);
            return this;
        }

        public ChargeBuilder status(Short status) {
            charge.setStatus(status);
            return this;
        }

        public ChargeBuilder paymentMethod(Short paymentMethod) {
            charge.setPaymentMethod(paymentMethod);
            return this;
        }

        public ChargeBuilder transactionNo(String transactionNo) {
            charge.setTransactionNo(transactionNo);
            return this;
        }

        public Charge build() {
            return charge;
        }
    }

    /**
     * 收费明细实体构建器
     */
    public static class ChargeDetailBuilder {
        private final ChargeDetail detail = new ChargeDetail();

        private ChargeDetailBuilder() {
            // 设置默认值
            detail.setMainId(1L);
            detail.setItemType("REGISTRATION");
            detail.setItemId(1L);
            detail.setItemName("挂号费");
            detail.setItemAmount(new BigDecimal("20.00"));
        }

        public static ChargeDetailBuilder builder() {
            return new ChargeDetailBuilder();
        }

        public ChargeDetailBuilder mainId(Long mainId) {
            detail.setMainId(mainId);
            return this;
        }

        public ChargeDetailBuilder charge(Charge charge) {
            detail.setCharge(charge);
            return this;
        }

        public ChargeDetailBuilder itemType(String itemType) {
            detail.setItemType(itemType);
            return this;
        }

        public ChargeDetailBuilder itemId(Long itemId) {
            detail.setItemId(itemId);
            return this;
        }

        public ChargeDetailBuilder itemName(String itemName) {
            detail.setItemName(itemName);
            return this;
        }

        public ChargeDetailBuilder itemAmount(BigDecimal itemAmount) {
            detail.setItemAmount(itemAmount);
            return this;
        }

        public ChargeDetail build() {
            return detail;
        }
    }

    /**
     * 病历实体构建器
     */
    public static class MedicalRecordBuilder {
        private final MedicalRecord record = new MedicalRecord();

        private MedicalRecordBuilder() {
            // 设置默认值
            record.setMainId(1L);
            record.setRecordNo("MR" + System.currentTimeMillis());
            record.setStatus((short) 1);
            record.setIsDeleted((short) 0);
            record.setChiefComplaint("测试主诉");
            record.setPresentIllness("现病史");
            record.setPastHistory("既往史");
        }

        public static MedicalRecordBuilder builder() {
            return new MedicalRecordBuilder();
        }

        public MedicalRecordBuilder mainId(Long mainId) {
            record.setMainId(mainId);
            return this;
        }

        public MedicalRecordBuilder recordNo(String recordNo) {
            record.setRecordNo(recordNo);
            return this;
        }

        public MedicalRecordBuilder registration(Registration registration) {
            record.setRegistration(registration);
            return this;
        }

        public MedicalRecordBuilder patient(Patient patient) {
            record.setPatient(patient);
            return this;
        }

        public MedicalRecordBuilder doctor(Doctor doctor) {
            record.setDoctor(doctor);
            return this;
        }

        public MedicalRecordBuilder status(Short status) {
            record.setStatus(status);
            return this;
        }

        public MedicalRecordBuilder chiefComplaint(String chiefComplaint) {
            record.setChiefComplaint(chiefComplaint);
            return this;
        }

        public MedicalRecordBuilder presentIllness(String presentIllness) {
            record.setPresentIllness(presentIllness);
            return this;
        }

        public MedicalRecordBuilder pastHistory(String pastHistory) {
            record.setPastHistory(pastHistory);
            return this;
        }

        public MedicalRecordBuilder personalHistory(String personalHistory) {
            record.setPersonalHistory(personalHistory);
            return this;
        }

        public MedicalRecordBuilder familyHistory(String familyHistory) {
            record.setFamilyHistory(familyHistory);
            return this;
        }

        public MedicalRecordBuilder physicalExam(String physicalExam) {
            record.setPhysicalExam(physicalExam);
            return this;
        }

        public MedicalRecordBuilder auxiliaryExam(String auxiliaryExam) {
            record.setAuxiliaryExam(auxiliaryExam);
            return this;
        }

        public MedicalRecordBuilder diagnosis(String diagnosis) {
            record.setDiagnosis(diagnosis);
            return this;
        }

        public MedicalRecordBuilder diagnosisCode(String diagnosisCode) {
            record.setDiagnosisCode(diagnosisCode);
            return this;
        }

        public MedicalRecordBuilder treatmentPlan(String treatmentPlan) {
            record.setTreatmentPlan(treatmentPlan);
            return this;
        }

        public MedicalRecordBuilder doctorAdvice(String doctorAdvice) {
            record.setDoctorAdvice(doctorAdvice);
            return this;
        }

        public MedicalRecord build() {
            return record;
        }
    }

    /**
     * 处方明细实体构建器
     */
    public static class PrescriptionDetailBuilder {
        private final PrescriptionDetail detail = new PrescriptionDetail();

        private PrescriptionDetailBuilder() {
            // 设置默认值
            detail.setMainId(1L);
            detail.setQuantity(1);
            detail.setDosage("每日3次，每次1粒");
            detail.setIsDeleted((short) 0);
        }

        public static PrescriptionDetailBuilder builder() {
            return new PrescriptionDetailBuilder();
        }

        public PrescriptionDetailBuilder mainId(Long mainId) {
            detail.setMainId(mainId);
            return this;
        }

        public PrescriptionDetailBuilder prescription(Prescription prescription) {
            detail.setPrescription(prescription);
            return this;
        }

        public PrescriptionDetailBuilder medicine(Medicine medicine) {
            detail.setMedicine(medicine);
            return this;
        }

        public PrescriptionDetailBuilder quantity(Integer quantity) {
            detail.setQuantity(quantity);
            return this;
        }

        public PrescriptionDetailBuilder dosage(String dosage) {
            detail.setDosage(dosage);
            return this;
        }

        public PrescriptionDetailBuilder unitPrice(BigDecimal unitPrice) {
            detail.setUnitPrice(unitPrice);
            return this;
        }

        public PrescriptionDetailBuilder subtotal(BigDecimal subtotal) {
            detail.setSubtotal(subtotal);
            return this;
        }

        public PrescriptionDetail build() {
            return detail;
        }
    }

    /**
     * 系统用户实体构建器
     */
    public static class SysUserBuilder {
        private final SysUser user = new SysUser();

        private SysUserBuilder() {
            // 设置默认值
            user.setId(1L);
            user.setUsername("test_user_" + System.currentTimeMillis());
            user.setPassword("$2a$10$encodedPassword");
            user.setRealName("测试用户");
            user.setRole("DOCTOR");
            user.setStatus(1);
        }

        public static SysUserBuilder builder() {
            return new SysUserBuilder();
        }

        public SysUserBuilder id(Long id) {
            user.setId(id);
            return this;
        }

        public SysUserBuilder username(String username) {
            user.setUsername(username);
            return this;
        }

        public SysUserBuilder password(String password) {
            user.setPassword(password);
            return this;
        }

        public SysUserBuilder realName(String realName) {
            user.setRealName(realName);
            return this;
        }

        public SysUserBuilder role(String role) {
            user.setRole(role);
            return this;
        }

        public SysUserBuilder status(Integer status) {
            user.setStatus(status);
            return this;
        }

        public SysUserBuilder departmentId(Long departmentId) {
            user.setDepartmentId(departmentId);
            return this;
        }

        public SysUserBuilder relatedId(Long relatedId) {
            user.setRelatedId(relatedId);
            return this;
        }

        public SysUser build() {
            return user;
        }
    }
}
