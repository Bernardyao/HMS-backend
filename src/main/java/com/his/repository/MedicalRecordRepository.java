package com.his.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.MedicalRecord;

/**
 * 电子病历 Repository
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long>, JpaSpecificationExecutor<MedicalRecord> {

    /**
     * 根据病历编号查询（未删除）
     */
    Optional<MedicalRecord> findByRecordNoAndIsDeleted(String recordNo, Short isDeleted);

    /**
     * 根据挂号记录ID查询（一对一）
     */
    Optional<MedicalRecord> findByRegistration_MainIdAndIsDeleted(Long registrationId, Short isDeleted);

    /**
     * 根据患者ID查询病历列表
     */
    List<MedicalRecord> findByPatient_MainIdAndIsDeletedOrderByVisitTimeDesc(Long patientId, Short isDeleted);

    /**
     * 根据医生ID查询病历列表
     */
    List<MedicalRecord> findByDoctor_MainIdAndIsDeletedOrderByVisitTimeDesc(Long doctorId, Short isDeleted);

    /**
     * 根据状态查询
     */
    List<MedicalRecord> findByStatusAndIsDeleted(Short status, Short isDeleted);

    /**
     * 检查病历编号是否存在
     */
    boolean existsByRecordNoAndIsDeleted(String recordNo, Short isDeleted);

    /**
     * 检查挂号记录是否已有病历
     */
    boolean existsByRegistration_MainIdAndIsDeleted(Long registrationId, Short isDeleted);

    /**
     * 查询患者最近一次病历
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.mainId = :patientId AND m.isDeleted = 0 ORDER BY m.visitTime DESC")
    List<MedicalRecord> findLatestByPatientId(@Param("patientId") Long patientId);

    /**
     * 根据诊断模糊查询
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.diagnosis LIKE %:diagnosis% AND m.isDeleted = 0")
    List<MedicalRecord> findByDiagnosisContaining(@Param("diagnosis") String diagnosis);

    /**
     * 查询时间范围内的病历
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.visitTime BETWEEN :startTime AND :endTime AND m.isDeleted = 0 ORDER BY m.visitTime DESC")
    List<MedicalRecord> findByVisitTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待审核的病历
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.status = 1 AND m.isDeleted = 0 ORDER BY m.createdAt")
    List<MedicalRecord> findPendingReview();
}
