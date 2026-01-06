package com.his.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.Prescription;

/**
 * 处方 Repository
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long>, JpaSpecificationExecutor<Prescription> {

    /**
     * 根据处方号查询（未删除）
     */
    Optional<Prescription> findByPrescriptionNoAndIsDeleted(String prescriptionNo, Short isDeleted);

    /**
     * 根据病历ID查询处方列表
     */
    List<Prescription> findByMedicalRecord_MainIdAndIsDeleted(Long recordId, Short isDeleted);

    /**
     * 根据患者ID查询处方列表
     */
    List<Prescription> findByPatient_MainIdAndIsDeletedOrderByCreatedAtDesc(Long patientId, Short isDeleted);

    /**
     * 根据医生ID查询处方列表
     */
    List<Prescription> findByDoctor_MainIdAndIsDeletedOrderByCreatedAtDesc(Long doctorId, Short isDeleted);

    /**
     * 根据状态查询
     */
    List<Prescription> findByStatusAndIsDeleted(Short status, Short isDeleted);

    /**
     * 检查处方号是否存在
     */
    boolean existsByPrescriptionNoAndIsDeleted(String prescriptionNo, Short isDeleted);

    /**
     * 查询待审核的处方
     */
    @Query("SELECT p FROM Prescription p WHERE p.status = 1 AND p.isDeleted = 0 ORDER BY p.createdAt")
    List<Prescription> findPendingReview();

    /**
     * 查询待发药的处方
     */
    @Query("SELECT p FROM Prescription p WHERE p.status = 2 AND p.isDeleted = 0 ORDER BY p.createdAt")
    List<Prescription> findPendingDispense();

    /**
     * 根据处方类型查询
     */
    List<Prescription> findByPrescriptionTypeAndIsDeleted(Short prescriptionType, Short isDeleted);

    /**
     * 查询时间范围内的处方
     */
    @Query("SELECT p FROM Prescription p WHERE p.createdAt BETWEEN :startTime AND :endTime AND p.isDeleted = 0 ORDER BY p.createdAt DESC")
    List<Prescription> findByCreatedAtRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询审核医生审核的处方
     */
    List<Prescription> findByReviewDoctor_MainIdAndIsDeleted(Long reviewDoctorId, Short isDeleted);

    // ========== 优化查询方法 - 使用 JOIN FETCH 避免 N+1 查询 ==========

    /**
     * 根据ID查询处方（包含明细）
     * 使用 JOIN FETCH 避免 N+1 查询问题
     *
     * <p><b>注意：</b>此方法目前未在业务代码中使用，保留用于：
     * <ul>
     *   <li>未来可能的性能优化场景</li>
     *   <li>查询优化参考</li>
     *   <li>调试和分析工具</li>
     * </ul>
     *
     * <p>当前实现使用 {@code PrescriptionDetailRepository} 直接查询明细列表，
     * 在事务环境中更加可靠，避免了 Hibernate 懒加载的潜在问题。</p>
     *
     * <p><b>TODO:</b> 考虑性能基准测试，比较以下两种方案：
     * <ul>
     *   <li>单次 JOIN FETCH 查询（此方法）</li>
     *   <li>两次独立查询（当前实现）</li>
     * </ul>
     */
    @Query("SELECT p FROM Prescription p LEFT JOIN FETCH p.details WHERE p.mainId = :mainId AND p.isDeleted = 0")
    Optional<Prescription> findByIdWithDetails(@Param("mainId") Long mainId);

    /**
     * 统计医生的处方数量
     */
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.doctor.mainId = :doctorId AND p.isDeleted = 0")
    long countByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * 获取药师今日工作统计
     * @param pharmacistId 药师ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    @Query("SELECT new com.his.dto.PharmacistStatisticsDTO(" +
            "COUNT(p), " +
            "COALESCE(SUM(p.totalAmount), 0), " +
            "COALESCE(SUM(p.itemCount), 0)) " +
            "FROM Prescription p " +
            "WHERE p.dispenseBy = :pharmacistId " +
            "AND p.dispenseTime BETWEEN :startTime AND :endTime " +
            "AND p.isDeleted = 0")
    com.his.dto.PharmacistStatisticsDTO getPharmacistStatistics(@Param("pharmacistId") Long pharmacistId,
                                                                @Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    // ========== 编号生成方法 - 使用数据库序列保证线程安全 ==========

    /**
     * 通过数据库原生函数生成处方号（线程安全）
     *
     * <p>使用PostgreSQL序列保证唯一性，格式：PRE + yyyyMMdd + 6位序列</p>
     *
     * @return 唯一的处方号
     */
    @Query(value = "SELECT generate_prescription_no()", nativeQuery = true)
    String generatePrescriptionNo();
}
