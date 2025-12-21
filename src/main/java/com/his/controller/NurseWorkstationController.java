package com.his.controller;

import com.his.common.Result;
import com.his.dto.NurseWorkstationDTO;
import com.his.service.NurseWorkstationService;
import com.his.vo.NurseRegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 护士工作站控制器
 */
@Tag(name = "护士工作站-今日挂号", description = "护士工作站的今日挂号列表查询接口")
@Slf4j
@RestController
@RequestMapping("/api/nurse/registrations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
public class NurseWorkstationController {

    private final NurseWorkstationService nurseWorkstationService;

    /**
     * 查询今日挂号列表
     */
    @Operation(
            summary = "查询今日挂号列表",
            description = "护士查看今日挂号列表，支持按科室、状态、就诊类型、关键字筛选。默认查询当天所有挂号记录"
    )
    @PostMapping("/today")
    public Result<List<NurseRegistrationVO>> getTodayRegistrations(
            @RequestBody(required = false) NurseWorkstationDTO dto
    ) {
        try {
            log.info("护士查询今日挂号列表，查询条件: {}", dto);
            List<NurseRegistrationVO> registrations = nurseWorkstationService.getTodayRegistrations(dto);
            String message = String.format("查询成功，共 %d 条挂号记录", registrations.size());
            return Result.success(message, registrations);
        } catch (IllegalArgumentException e) {
            log.warn("查询参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询今日挂号列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
