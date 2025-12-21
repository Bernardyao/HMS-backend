package com.his.service;

import com.his.enums.RegStatusEnum;
import com.his.vo.RegistrationVO;

import java.util.List;

/**
 * 医生工作站服务接口
 */
public interface DoctorService {

    /**
     * 获取今日候诊列表（支持个人/科室混合视图）
     *
     * @param doctorId 医生ID（个人视图时使用）
     * @param deptId 科室ID（科室视图时使用，或用于验证）
     * @param showAllDept 是否显示科室所有患者（true=科室视图，false=个人视图）
     * @return 候诊列表
     */
    List<RegistrationVO> getWaitingList(Long doctorId, Long deptId, boolean showAllDept);

    /**
     * 更新挂号状态（接诊或完成就诊）
     *
     * @param regId 挂号记录ID
     * @param newStatus 新状态
     */
    void updateStatus(Long regId, RegStatusEnum newStatus);

    /**
     * 【新增】验证并更新挂号状态（带医生身份验证，防止水平越权IDOR）
     *
     * <p><b>安全性说明：</b></p>
     * <ul>
     *   <li>这是推荐的使用方式，会验证当前医生是否拥有该挂号记录</li>
     *   <li>医生只能更新自己患者（通过该医生接诊）的挂号状态</li>
     *   <li>如果医生尝试更新其他医生的患者，会抛出异常</li>
     * </ul>
     *
     * @param regId 挂号记录ID
     * @param currentDoctorId 当前医生ID（从JWT Token获取）
     * @param newStatus 新状态
     * @throws IllegalArgumentException 如果挂号不存在或医生无权限
     */
    void validateAndUpdateStatus(Long regId, Long currentDoctorId, RegStatusEnum newStatus);
}

