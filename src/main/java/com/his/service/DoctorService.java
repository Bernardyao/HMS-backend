package com.his.service;

import com.his.entity.Doctor;
import com.his.enums.RegStatusEnum;
import com.his.vo.PatientDetailVO;
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

    /**
     * 【新增】查询患者详细信息（包含数据脱敏）
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>根据患者ID查询完整患者信息</li>
     *   <li>自动脱敏敏感信息（身份证、手机号）</li>
     *   <li>验证患者记录有效性（未删除）</li>
     * </ul>
     *
     * @param patientId 患者ID
     * @return 患者详细信息VO
     * @throws IllegalArgumentException 当患者不存在或已被删除时
     */
    PatientDetailVO getPatientDetail(Long patientId);

    /**
     * 【新增】验证医生存在并返回医生信息（用于防止IDOR攻击）
     *
     * <p><b>安全用途：</b></p>
     * <ul>
     *   <li>验证医生ID是否有效（医生是否存在且未被删除）</li>
     *   <li>用于Controller层的安全验证，防止水平越权攻击</li>
     *   <li>从Controller移到Service层，遵守分层架构原则</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <pre>
     * // 管理员模式：验证指定的医生是否存在
     * Doctor doctor = doctorService.getAndValidateDoctor(adminDoctorId);
     * Long deptId = doctor.getDepartment().getMainId();
     *
     * // 普通医生模式：验证Token中的医生ID是否有效
     * Doctor doctor = doctorService.getAndValidateDoctor(doctorId);
     * Long deptId = doctor.getDepartment().getMainId();
     * </pre>
     *
     * @param doctorId 医生ID
     * @return 医生实体（包含科室信息）
     * @throws IllegalArgumentException 如果医生不存在或已被删除
     */
    Doctor getAndValidateDoctor(Long doctorId);
}

