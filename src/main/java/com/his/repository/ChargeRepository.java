package com.his.repository;

import com.his.entity.Charge;
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
}
