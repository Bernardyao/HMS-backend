package com.his.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.PrescriptionDetail;

/**
 * 处方明细 Repository
 */
@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Long>, JpaSpecificationExecutor<PrescriptionDetail> {

    /**
     * 根据处方ID查询明细列表
     */
    List<PrescriptionDetail> findByPrescription_MainIdAndIsDeletedOrderBySortOrder(Long prescriptionId, Short isDeleted);

    /**
     * 根据药品ID查询使用记录
     */
    List<PrescriptionDetail> findByMedicine_MainIdAndIsDeleted(Long medicineId, Short isDeleted);

    /**
     * 删除处方下的所有明细（软删除）
     */
    @Query("UPDATE PrescriptionDetail pd SET pd.isDeleted = 1 WHERE pd.prescription.mainId = :prescriptionId")
    int softDeleteByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 统计药品使用次数
     */
    @Query("SELECT COUNT(pd) FROM PrescriptionDetail pd WHERE pd.medicine.mainId = :medicineId AND pd.isDeleted = 0")
    long countByMedicineId(@Param("medicineId") Long medicineId);

    /**
     * 统计药品使用总量
     */
    @Query("SELECT COALESCE(SUM(pd.quantity), 0) FROM PrescriptionDetail pd WHERE pd.medicine.mainId = :medicineId AND pd.isDeleted = 0")
    long sumQuantityByMedicineId(@Param("medicineId") Long medicineId);

    /**
     * 查询处方的明细数量
     */
    @Query("SELECT COUNT(pd) FROM PrescriptionDetail pd WHERE pd.prescription.mainId = :prescriptionId AND pd.isDeleted = 0")
    long countByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 根据药品名称模糊查询
     */
    List<PrescriptionDetail> findByMedicineNameContainingAndIsDeleted(String medicineName, Short isDeleted);
}
