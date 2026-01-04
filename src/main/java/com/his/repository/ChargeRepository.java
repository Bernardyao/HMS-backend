package com.his.repository;

import com.his.entity.Charge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 缴费记录 Repository
 */
@Repository
public interface ChargeRepository extends JpaRepository<Charge, Long>, JpaSpecificationExecutor<Charge> {

    /**
     * 根据收费单号查询（未删除）
     */
    Optional<Charge> findByChargeNoAndIsDeleted(String chargeNo, Short isDeleted);

    /**
     * 根据患者ID查询缴费记录
     */
    List<Charge> findByPatient_MainIdAndIsDeletedOrderByCreatedAtDesc(Long patientId, Short isDeleted);

    /**
     * 根据挂号记录ID查询缴费记录
     */
    List<Charge> findByRegistration_MainIdAndIsDeleted(Long registrationId, Short isDeleted);

    /**
     * 根据状态查询
     */
    List<Charge> findByStatusAndIsDeleted(Short status, Short isDeleted);

    /**
     * 根据收费类型查询
     */
    List<Charge> findByChargeTypeAndIsDeleted(Short chargeType, Short isDeleted);

    /**
     * 检查收费单号是否存在
     */
    boolean existsByChargeNoAndIsDeleted(String chargeNo, Short isDeleted);

    /**
     * 查询患者未缴费记录
     */
    @Query("SELECT c FROM Charge c WHERE c.patient.mainId = :patientId AND c.status = 0 AND c.isDeleted = 0")
    List<Charge> findUnpaidByPatientId(@Param("patientId") Long patientId);

    /**
     * 查询时间范围内的缴费记录
     */
    @Query("SELECT c FROM Charge c WHERE c.chargeTime BETWEEN :startTime AND :endTime AND c.isDeleted = 0 ORDER BY c.chargeTime DESC")
    List<Charge> findByChargeTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计时间范围内的收费总额
     */
    @Query("SELECT COALESCE(SUM(c.actualAmount), 0) FROM Charge c WHERE c.chargeTime BETWEEN :startTime AND :endTime AND c.status = 1 AND c.isDeleted = 0")
    BigDecimal sumActualAmountByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据支付方式查询
     */
    List<Charge> findByPaymentMethodAndIsDeleted(Short paymentMethod, Short isDeleted);

    /**
     * 根据收费员ID查询
     */
    List<Charge> findByCashierMainIdAndIsDeleted(Long cashierId, Short isDeleted);

    /**
     * 根据发票号查询
     */
    Optional<Charge> findByInvoiceNoAndIsDeleted(String invoiceNo, Short isDeleted);

    /**
     * 根据交易流水号查询（用于幂等性检查）
     */
    Optional<Charge> findByTransactionNo(String transactionNo);

    /**
     * 统计患者消费总额
     */
    @Query("SELECT COALESCE(SUM(c.actualAmount), 0) FROM Charge c WHERE c.patient.mainId = :patientId AND c.status = 1 AND c.isDeleted = 0")
    BigDecimal sumActualAmountByPatientId(@Param("patientId") Long patientId);

    /**
     * 根据挂号单ID和收费类型查询（用于分阶段收费）
     */
    List<Charge> findByRegistration_MainIdAndChargeTypeAndIsDeleted(Long registrationId, Short chargeType, Short isDeleted);

    /**
     * 检查挂号费是否已支付
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Charge c " +
           "WHERE c.registration.mainId = :registrationId " +
           "AND c.chargeType = 1 " +
           "AND c.status = 1 " +
           "AND c.isDeleted = 0")
    boolean existsPaidRegistrationCharge(@Param("registrationId") Long registrationId);

    /**
     * 检查挂号费是否已支付（优化版本 - 使用EXISTS避免N+1查询）
     *
     * <p>通过关联查询 charge_detail 表，检查是否存在类型为 REGISTRATION 的已支付明细</p>
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM his_charge c
            INNER JOIN his_charge_detail d ON c.main_id = d.charge_main_id
            WHERE c.registration_main_id = :registrationId
              AND c.charge_type = 1
              AND c.status = 1
              AND c.is_deleted = 0
              AND d.item_type = 'REGISTRATION'
              AND d.is_deleted = 0
            LIMIT 1
        )
    """, nativeQuery = true)
    boolean isRegistrationFeePaidOptimized(@Param("registrationId") Long registrationId);

    /**
     * 检查处方费是否已支付（优化版本 - 使用EXISTS避免N+1查询）
     *
     * @param prescriptionId 处方ID
     * @return 是否已支付
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM his_charge_detail d
            INNER JOIN his_charge c ON d.charge_main_id = c.main_id
            WHERE d.item_id = :prescriptionId
              AND d.item_type = 'PRESCRIPTION'
              AND c.status = 1
              AND d.is_deleted = 0
              AND c.is_deleted = 0
            LIMIT 1
        )
    """, nativeQuery = true)
    boolean isPrescriptionFeePaidOptimized(@Param("prescriptionId") Long prescriptionId);

    // ========== 优化查询方法 - 使用 @EntityGraph 避免 N+1 查询 ==========

    /**
     * 根据ID查询收费记录（包含明细）
     * 使用 @EntityGraph 避免 N+1 查询问题
     */
    @EntityGraph(attributePaths = {"details"})
    @Query("SELECT c FROM Charge c WHERE c.mainId = :mainId AND c.isDeleted = 0")
    Optional<Charge> findByIdWithDetails(@Param("mainId") Long mainId);

    /**
     * 根据交易流水号查询收费记录（包含明细）
     * 使用 @EntityGraph 避免 N+1 查询问题
     */
    @EntityGraph(attributePaths = {"details"})
    @Query("SELECT c FROM Charge c WHERE c.transactionNo = :transactionNo")
    Optional<Charge> findByTransactionNoWithDetails(@Param("transactionNo") String transactionNo);

    /**
     * 根据挂号记录ID查询缴费记录（包含明细，按创建时间倒序）
     * 使用 @EntityGraph 避免 N+1 查询问题
     */
    @EntityGraph(attributePaths = {"details"})
    @Query("SELECT c FROM Charge c WHERE c.registration.mainId = :registrationMainId AND c.isDeleted = :isDeleted ORDER BY c.createdAt DESC")
    List<Charge> findByRegistrationIdWithDetailsOrderByCreatedAtDesc(
            @Param("registrationMainId") Long registrationMainId,
            @Param("isDeleted") Short isDeleted);

    // ========== 编号生成方法 - 使用数据库序列保证线程安全 ==========

    /**
     * 通过数据库原生函数生成收费单号（线程安全）
     *
     * <p>使用PostgreSQL序列保证唯一性，格式：CHG + yyyyMMdd + 6位序列</p>
     * <p>示例：CHG20260103000001</p>
     *
     * @return 唯一的收费单号
     */
    @Query(value = "SELECT generate_charge_no()", nativeQuery = true)
    String generateChargeNo();
}
