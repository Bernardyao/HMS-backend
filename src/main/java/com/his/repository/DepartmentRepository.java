package com.his.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.his.entity.Department;

/**
 * 科室 Repository
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    /**
     * 根据科室代码查询（未删除）
     */
    Optional<Department> findByDeptCodeAndIsDeleted(String deptCode, Short isDeleted);

    /**
     * 根据科室名称查询（未删除）
     */
    List<Department> findByNameContainingAndIsDeleted(String name, Short isDeleted);

    /**
     * 根据状态查询所有科室（未删除）
     */
    List<Department> findByStatusAndIsDeletedOrderBySortOrder(Short status, Short isDeleted);

    /**
     * 查询顶级科室（parent_id为空）
     */
    List<Department> findByParentIsNullAndIsDeletedOrderBySortOrder(Short isDeleted);

    /**
     * 根据父科室ID查询子科室
     */
    List<Department> findByParent_MainIdAndIsDeletedOrderBySortOrder(Long parentId, Short isDeleted);

    /**
     * 检查科室代码是否存在
     */
    boolean existsByDeptCodeAndIsDeleted(String deptCode, Short isDeleted);

    /**
     * 查询所有启用的科室
     */
    @Query("SELECT d FROM Department d WHERE d.status = 1 AND d.isDeleted = 0 ORDER BY d.sortOrder")
    List<Department> findAllActive();

    /**
     * 根据ID列表批量查询
     */
    List<Department> findByMainIdInAndIsDeleted(List<Long> ids, Short isDeleted);
}
