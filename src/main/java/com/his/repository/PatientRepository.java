package com.his.repository;

import com.his.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 患者 Repository
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    /**
     * 根据病历号查询（未删除）
     */
    Optional<Patient> findByPatientNoAndIsDeleted(String patientNo, Short isDeleted);

    /**
     * 根据身份证号查询（未删除）
     */
    Optional<Patient> findByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 统计指定身份证号的患者数量（未删除）
     */
    long countByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 根据姓名模糊查询（未删除）
     */
    List<Patient> findByNameContainingAndIsDeleted(String name, Short isDeleted);

    /**
     * 根据手机号查询（未删除）
     */
    List<Patient> findByPhoneAndIsDeleted(String phone, Short isDeleted);

    /**
     * 检查病历号是否存在
     */
    boolean existsByPatientNoAndIsDeleted(String patientNo, Short isDeleted);

    /**
     * 检查身份证号是否存在
     */
    boolean existsByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 查询所有未删除的患者
     */
    @Query("SELECT p FROM Patient p WHERE p.isDeleted = 0")
    List<Patient> findAllActive();

    /**
     * 根据姓名和手机号查询
     */
    @Query("SELECT p FROM Patient p WHERE p.name = :name AND p.phone = :phone AND p.isDeleted = 0")
    Optional<Patient> findByNameAndPhone(@Param("name") String name, @Param("phone") String phone);

    /**
     * 根据医保卡号查询
     */
    Optional<Patient> findByMedicalCardNoAndIsDeleted(String medicalCardNo, Short isDeleted);

    /**
     * 根据ID列表批量查询
     */
    List<Patient> findByMainIdInAndIsDeleted(List<Long> ids, Short isDeleted);
}
