package com.his.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.his.entity.RegistrationStatusHistory;

/**
 * 挂号状态转换历史 Repository
 */
@Repository
public interface RegistrationStatusHistoryRepository extends JpaRepository<RegistrationStatusHistory, Long> {

    /**
     * 根据挂号ID查询状态转换历史（按时间倒序）
     */
    List<RegistrationStatusHistory> findByRegistrationMainIdOrderByCreatedAtDesc(Long registrationMainId);
}
