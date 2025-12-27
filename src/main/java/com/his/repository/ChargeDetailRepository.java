package com.his.repository;

import com.his.entity.ChargeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
