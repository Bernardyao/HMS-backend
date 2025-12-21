package com.his.controller;

import com.his.common.Result;
import com.his.service.BasicDataService;
import com.his.vo.DepartmentBasicVO;
import com.his.vo.DoctorBasicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公共数据接口控制器（用于各工作站）
 */
@Slf4j
@RestController
@RequestMapping("/api/common/data")
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
