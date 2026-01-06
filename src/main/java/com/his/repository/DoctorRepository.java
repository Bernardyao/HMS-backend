package com.his.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.Doctor;

/**
 * 医生 Repository
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

    /**
     * 根据医生工号查询（未删除）
     */
    Optional<Doctor> findByDoctorNoAndIsDeleted(String doctorNo, Short isDeleted);

    /**
     * 根据姓名模糊查询（未删除）
     */
    List<Doctor> findByNameContainingAndIsDeleted(String name, Short isDeleted);

    /**
     * 根据科室ID查询医生列表
     */
    List<Doctor> findByDepartment_MainIdAndIsDeleted(Long departmentId, Short isDeleted);

    /**
     * 根据科室ID和状态查询医生列表
     */
    List<Doctor> findByDepartment_MainIdAndStatusAndIsDeleted(Long departmentId, Short status, Short isDeleted);

    /**
     * 检查医生工号是否存在
     */
    boolean existsByDoctorNoAndIsDeleted(String doctorNo, Short isDeleted);

    /**
     * 根据职称查询医生
     */
    List<Doctor> findByTitleAndIsDeleted(String title, Short isDeleted);

    /**
     * 查询所有启用的医生
     */
    @Query("SELECT d FROM Doctor d WHERE d.status = 1 AND d.isDeleted = 0")
    List<Doctor> findAllActive();

    /**
     * 根据科室ID查询启用的医生
     */
    @Query("SELECT d FROM Doctor d WHERE d.department.mainId = :deptId AND d.status = 1 AND d.isDeleted = 0")
    List<Doctor> findActiveByDepartmentId(@Param("deptId") Long deptId);

    /**
     * 根据ID列表批量查询
     */
    List<Doctor> findByMainIdInAndIsDeleted(List<Long> ids, Short isDeleted);
}
