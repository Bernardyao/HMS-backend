package com.his.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.service.BasicDataService;
import com.his.vo.DepartmentBasicVO;
import com.his.vo.DoctorBasicVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共数据接口控制器（用于各工作站）
 *
 * <p>为各工作站提供科室、医生等基础数据查询功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>科室查询</b>：查询所有启用的科室列表</li>
 *   <li><b>医生查询</b>：根据科室ID查询该科室的所有医生</li>
 *   <li><b>数据筛选</b>：只返回启用且未删除的数据</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>挂号界面</b>：科室选择器、医生选择器</li>
 *   <li><b>医生工作站</b>：科室切换</li>
 *   <li><b>护士工作站</b>：分配医生</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要已认证用户（isAuthenticated()）</p>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.BasicDataService
 */
@Slf4j
@RestController
@RequestMapping("/api/common/data")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "公共接口-基础数据", description = "提供科室、医生等基础数据查询，供各工作站使用")
public class BasicDataController {

    private final BasicDataService basicDataService;

    /**
     * 获取所有启用的科室列表
     */
    @GetMapping("/departments")
    @Operation(summary = "获取科室列表", description = "获取所有启用的科室列表，用于挂号界面下拉选择")
    public Result<List<DepartmentBasicVO>> getDepartments() {
        log.info("API调用：获取科室列表");
        List<DepartmentBasicVO> departments = basicDataService.getAllDepartments();
        return Result.success(departments);
    }

    /**
     * 根据科室ID获取医生列表
     */
    @GetMapping("/doctors")
    @Operation(summary = "获取医生列表", description = "根据科室ID获取该科室下所有启用的医生列表（含状态和挂号费），用于挂号界面下拉选择")
    public Result<List<DoctorBasicVO>> getDoctorsByDepartment(
            @Parameter(description = "科室ID", required = true, example = "1")
            @RequestParam("deptId") Long deptId) {

        log.info("API调用：获取科室 {} 的医生列表", deptId);
        List<DoctorBasicVO> doctors = basicDataService.getDoctorsByDepartment(deptId);
        return Result.success(doctors);
    }
}
