package com.his.repository;

import com.his.entity.ChargeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收费明细 Repository
 */
@Repository
public interface ChargeDetailRepository extends JpaRepository<ChargeDetail, Long> {

    /**
     * 根据收费主表ID查询明细
     */
    List<ChargeDetail> findByCharge_MainId(Long chargeId);

    /**
     * 根据项目ID和类型查询收费明细（用于防止重复收费）
     */
    List<ChargeDetail> findByItemIdAndItemType(Long itemId, String itemType);

    /**
     * 查询挂号费收费明细
     */
    @Query("SELECT cd FROM ChargeDetail cd WHERE cd.itemType = 'REGISTRATION' AND cd.itemId = :registrationId")
    List<ChargeDetail> findRegistrationChargeDetails(@Param("registrationId") Long registrationId);

    /**
     * 查询处方费收费明细
     */
    @Query("SELECT cd FROM ChargeDetail cd WHERE cd.itemType = 'PRESCRIPTION' AND cd.itemId = :prescriptionId")
    List<ChargeDetail> findPrescriptionChargeDetails(@Param("prescriptionId") Long prescriptionId);
}
