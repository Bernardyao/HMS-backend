package com.his.service.impl;

import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.service.BasicDataService;
import com.his.vo.DepartmentBasicVO;
import com.his.vo.DoctorBasicVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础数据服务实现类
 *
 * <p>提供系统基础数据的查询服务，主要用于前端下拉框、选择器等UI组件</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>科室列表查询：查询所有启用的科室信息</li>
 *   <li>医生列表查询：根据科室查询该科室下所有启用的医生</li>
 *   <li>数据脱敏：仅返回基础信息，不包含敏感数据</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>仅查询启用的科室（status=1）且未删除（isDeleted=0）</li>
 *   <li>仅查询启用的医生（status=1）且未删除（isDeleted=0）</li>
 *   <li>科室列表按排序字段（sortOrder）排序</li>
 *   <li>医生列表按科室分组</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>挂号页面：科室和医生下拉框数据源</li>
 *   <li>医生工作站：科室选择器</li>
 *   <li>数据统计：基础信息查询</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.BasicDataService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicDataServiceImpl implements BasicDataService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 获取所有启用的科室列表
     *
     * <p>查询所有启用且未删除的科室，返回基础信息</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li>科室状态为启用（status=1）</li>
     *   <li>科室未删除（isDeleted=0）</li>
     * </ul>
     *
     * <p><b>排序规则：</b></p>
     * <ul>
     *   <li>按排序字段（sortOrder）升序排列</li>
     * </ul>
     *
     * @return 科室基础信息列表（DepartmentBasicVO）
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentBasicVO> getAllDepartments() {
        log.info("查询所有启用的科室列表");
        
        // 查询所有启用且未删除的科室，按排序字段排序
        List<Department> departments = departmentRepository.findByStatusAndIsDeletedOrderBySortOrder(
                (short) 1, (short) 0);
        
        log.info("找到 {} 个启用的科室", departments.size());
        
        return departments.stream()
                .map(this::convertToDepartmentBasicVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据科室ID获取该科室下所有启用的医生列表
     *
     * <p>查询指定科室下所有启用且未删除的医生</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li>医生属于指定科室</li>
     *   <li>医生状态为启用（status=1）</li>
     *   <li>医生未删除（isDeleted=0）</li>
     * </ul>
     *
     * @param deptId 科室ID
     * @return 医生基础信息列表（DoctorBasicVO）
     * @throws IllegalArgumentException 如果科室不存在
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<DoctorBasicVO> getDoctorsByDepartment(Long deptId) {
        log.info("查询科室 {} 下的所有启用医生", deptId);
        
        // 验证科室是否存在
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new IllegalArgumentException("科室不存在，ID: " + deptId));
        
        // 查询该科室下所有启用且未删除的医生
        List<Doctor> doctors = doctorRepository.findByDepartment_MainIdAndStatusAndIsDeleted(
                deptId, (short) 1, (short) 0);
        
        log.info("科室 {} ({}) 下找到 {} 位启用的医生", 
                department.getName(), deptId, doctors.size());
        
        return doctors.stream()
                .map(doctor -> convertToDoctorBasicVO(doctor, department))
                .collect(Collectors.toList());
    }

    /**
     * 转换为科室基础VO
     */
    private DepartmentBasicVO convertToDepartmentBasicVO(Department department) {
        DepartmentBasicVO vo = DepartmentBasicVO.builder()
                .id(department.getMainId())
                .code(department.getDeptCode())
                .name(department.getName())
                .build();
        
        // 设置上级科室信息
        if (department.getParent() != null) {
            vo.setParentId(department.getParent().getMainId());
            vo.setParentName(department.getParent().getName());
        }
        
        return vo;
    }

    /**
     * 转换为医生基础VO
     */
    private DoctorBasicVO convertToDoctorBasicVO(Doctor doctor, Department department) {
        // 根据职称计算挂号费（实际业务中可能需要从配置表读取）
        BigDecimal registrationFee = calculateRegistrationFee(doctor.getTitle());
        
        return DoctorBasicVO.builder()
                .id(doctor.getMainId())
                .doctorNo(doctor.getDoctorNo())
                .name(doctor.getName())
                .gender(doctor.getGender())
                .genderText(getGenderText(doctor.getGender()))
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .status(doctor.getStatus())
                .statusText(getStatusText(doctor.getStatus()))
                .departmentId(department.getMainId())
                .departmentName(department.getName())
                .registrationFee(registrationFee)
                .build();
    }

    /**
     * 根据职称计算挂号费（简化版本，实际应从配置表读取）
     */
    private BigDecimal calculateRegistrationFee(String title) {
        if (title == null) {
            return new BigDecimal("20.00"); // 默认挂号费
        }
        
        // 根据职称返回不同的挂号费
        if (title.contains("主任医师")) {
            return new BigDecimal("50.00");
        } else if (title.contains("副主任医师")) {
            return new BigDecimal("40.00");
        } else if (title.contains("主治医师")) {
            return new BigDecimal("30.00");
        } else if (title.contains("住院医师") || title.contains("医师")) {
            return new BigDecimal("20.00");
        }
        
        return new BigDecimal("20.00"); // 默认挂号费
    }

    /**
     * 获取性别文本
     */
    private String getGenderText(Short gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 0: return "女";
            case 1: return "男";
            default: return "未知";
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Short status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "停用";
    }
}
