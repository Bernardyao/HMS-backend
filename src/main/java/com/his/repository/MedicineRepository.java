package com.his.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.Medicine;

/**
 * 药品 Repository
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {

    /**
     * 根据药品编码查询（未删除）
     */
    Optional<Medicine> findByMedicineCodeAndIsDeleted(String medicineCode, Short isDeleted);

    /**
     * 根据药品名称模糊查询（未删除）
     */
    List<Medicine> findByNameContainingAndIsDeleted(String name, Short isDeleted);

    /**
     * 根据药品分类查询
     */
    List<Medicine> findByCategoryAndIsDeleted(String category, Short isDeleted);

    /**
     * 根据状态查询药品
     */
    List<Medicine> findByStatusAndIsDeleted(Short status, Short isDeleted);

    /**
     * 检查药品编码是否存在
     */
    boolean existsByMedicineCodeAndIsDeleted(String medicineCode, Short isDeleted);

    /**
     * 查询所有启用的药品
     */
    @Query("SELECT m FROM Medicine m WHERE m.status = 1 AND m.isDeleted = 0")
    List<Medicine> findAllActive();

    /**
     * 根据关键字搜索药品（名称或编码模糊匹配）
     */
    @Query("SELECT m FROM Medicine m WHERE m.isDeleted = :isDeleted AND m.status = 1 " +
           "AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.medicineCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Medicine> searchByKeyword(@Param("keyword") String keyword, @Param("isDeleted") Short isDeleted);

    /**
     * 查询库存低于最低库存的药品
     */
    @Query("SELECT m FROM Medicine m WHERE m.stockQuantity <= m.minStock AND m.isDeleted = 0")
    List<Medicine> findLowStock();

    /**
     * 更新库存数量
     */
    @Modifying
    @Query("UPDATE Medicine m SET m.stockQuantity = m.stockQuantity - :quantity WHERE m.mainId = :id AND m.stockQuantity >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 增加库存数量
     */
    @Modifying
    @Query("UPDATE Medicine m SET m.stockQuantity = m.stockQuantity + :quantity WHERE m.mainId = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 根据是否处方药查询
     */
    List<Medicine> findByIsPrescriptionAndIsDeleted(Short isPrescription, Short isDeleted);

    /**
     * 根据ID列表批量查询
     */
    List<Medicine> findByMainIdInAndIsDeleted(List<Long> ids, Short isDeleted);
}
