package com.his.service;

import java.util.List;

import com.his.vo.DepartmentBasicVO;
import com.his.vo.DoctorBasicVO;

/**
 * 基础数据服务接口
 */
public interface BasicDataService {

    /**
     * 获取所有启用的科室列表（用于前端下拉选择）
     *
     * @return 科室列表
     */
    List<DepartmentBasicVO> getAllDepartments();

    /**
     * 根据科室ID获取该科室下所有启用的医生列表（用于前端下拉选择）
     *
     * @param deptId 科室ID
     * @return 医生列表
     */
    List<DoctorBasicVO> getDoctorsByDepartment(Long deptId);
}
