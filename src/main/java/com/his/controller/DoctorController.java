package com.his.controller;

import com.his.common.Result;
import com.his.config.JwtAuthenticationToken;
import com.his.entity.Doctor;
import com.his.enums.RegStatusEnum;
import com.his.repository.DoctorRepository;
import com.his.service.DoctorService;
import com.his.vo.RegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生工作站控制器
 */
@Tag(name = "医生工作站", description = "医生工作站相关接口，包括候诊列表查询、接诊、完成就诊等操作")
@Slf4j
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;

    /**
     * 查询今日候诊列表（支持个人/科室混合视图）
     * 
     * 安全说明：
     * - doctorId 和 deptId 从 JWT Token 中获取，已集成 Spring Security
     * - 只有角色为 DOCTOR 的用户才能访问此接口
     * 
     * 业务逻辑：
     * - showAll=false（默认）：个人视图，仅查询分配给当前医生的待诊患者
     * - showAll=true：科室视图，查询当前科室下所有待诊患者（用于科室主任查看或医生协作）
     *
     * @param showAll 是否显示科室所有患者（默认false=个人视图，true=科室视图）
     * @return 候诊列表
     */
    @Operation(summary = "查询今日候诊列表（支持个人/科室视图切换）", 
               description = "默认显示分配给当前医生的候诊患者（个人视图），设置showAll=true可查看整个科室的候诊患者（科室视图）。" +
                           "按排队号升序排列。医生信息从JWT Token中自动获取。")
    @GetMapping("/waiting-list")
    public Result<List<RegistrationVO>> getWaitingList(
            @Parameter(description = "医生ID（仅测试/兼容用途；生产环境从JWT Token中获取）", required = false, example = "1")
            @RequestParam(required = false) Long doctorId,
            @Parameter(description = "科室ID（仅测试/兼容用途；生产环境从JWT Token中获取）", required = false, example = "1")
            @RequestParam(required = false) Long deptId,
            @Parameter(description = "是否显示科室所有患者（false=个人视图，true=科室视图）", required = false, example = "false")
            @RequestParam(defaultValue = "false") boolean showAll) {
        try {
            Long resolvedDoctorId = null;
            Long resolvedDeptId = null;

            // 优先从 SecurityContext 获取当前登录医生信息（生产环境）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken
                    && jwtAuthenticationToken.getRelatedId() != null) {

                resolvedDoctorId = jwtAuthenticationToken.getRelatedId(); // 对于医生，relatedId 就是 doctorId
                // 从数据库获取医生信息以获得科室ID
                Doctor doctor = doctorRepository.findById(resolvedDoctorId)
                        .orElseThrow(() -> new IllegalArgumentException("医生信息不存在"));
                resolvedDeptId = doctor.getDepartment().getMainId();
            } else {
                // 兼容测试/未携带JWT的场景：从请求参数获取
                if (doctorId == null) {
                    throw new IllegalArgumentException("doctorId不能为空");
                }
                if (deptId == null) {
                    throw new IllegalArgumentException("deptId不能为空");
                }
                resolvedDoctorId = doctorId;
                resolvedDeptId = deptId;
            }
            
            log.info("查询候诊列表请求，医生ID: {}, 科室ID: {}, 科室视图: {}", resolvedDoctorId, resolvedDeptId, showAll);
            
            List<RegistrationVO> waitingList = doctorService.getWaitingList(resolvedDoctorId, resolvedDeptId, showAll);
            
            // 返回带有业务说明的响应
            if (waitingList.isEmpty()) {
                String viewMode = showAll ? "科室" : "个人";
                log.info("{} [医生ID: {}, 科室ID: {}] 今日暂无候诊患者", viewMode, resolvedDoctorId, resolvedDeptId);
                return Result.success("今日暂无候诊患者", waitingList);
            }
            
            String viewMode = showAll ? "科室视图" : "个人视图";
            log.info("{} [医生ID: {}, 科室ID: {}] 查询到 {} 位候诊患者", 
                    viewMode, resolvedDoctorId, resolvedDeptId, waitingList.size());
            return Result.success(
                    String.format("查询成功（%s），共%d位候诊患者", viewMode, waitingList.size()), 
                    waitingList
            );
        } catch (IllegalArgumentException e) {
            log.warn("查询候诊列表参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询候诊列表系统异常", e);
            return Result.error("系统异常，请联系管理员: " + e.getMessage());
        }
    }

    /**
     * 更新挂号状态（接诊或完成就诊）
     *
     * @param id 挂号记录ID
     * @param status 新状态码（1=已就诊/完成就诊）
     * @return 操作结果
     */
    @Operation(summary = "更新就诊状态", description = "医生接诊或完成就诊，将挂号状态从待就诊更新为已就诊")
    @PutMapping("/registrations/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "新状态码（1=已就诊）", required = true, example = "1")
            @RequestParam Short status) {
        try {
            // 防御性编程: 参数验证
            if (id == null) {
                log.warn("更新就诊状态失败: 挂号ID为空");
                return Result.badRequest("挂号ID不能为空");
            }
            
            if (status == null) {
                log.warn("更新就诊状态失败: 状态码为空，挂号ID: {}", id);
                return Result.badRequest("状态码不能为空");
            }
            
            log.info("更新就诊状态请求，挂号ID: {}, 状态码: {}", id, status);
            
            // 将状态码转换为枚举（会验证状态码有效性）
            RegStatusEnum newStatus;
            try {
                newStatus = RegStatusEnum.fromCode(status);
            } catch (IllegalArgumentException e) {
                log.warn("更新就诊状态失败: 无效的状态码 - {}", status);
                return Result.badRequest("无效的状态码: " + status + "，有效值: 0=待就诊, 1=已就诊, 2=已取消, 3=已退号");
            }
            
            // 调用服务层更新状态
            doctorService.updateStatus(id, newStatus);
            
            return Result.success(String.format("状态更新成功: %s", newStatus.getDescription()), null);
        } catch (IllegalArgumentException e) {
            log.warn("状态更新参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("状态更新业务错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("状态更新系统异常", e);
            return Result.error("系统异常，请联系管理员: " + e.getMessage());
        }
    }
}
