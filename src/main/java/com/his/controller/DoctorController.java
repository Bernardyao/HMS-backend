package com.his.controller;

import com.his.common.Result;
import com.his.common.SecurityUtils;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生工作站控制器
 * 
 * <p><b>安全设计：防止水平越权（IDOR）攻击</b>
 * 
 * <p>本控制器的所有接口都使用 {@link SecurityUtils} 从 JWT Token 中获取当前医生的身份信息，
 * 而<b>不信任</b>前端传递的任何用户标识参数（如 doctorId）。这样可以防止攻击者通过修改请求参数
 * 来访问其他医生的数据。
 * 
 * @author HIS 开发团队
 * @see SecurityUtils
 * @see com.his.common.JwtUtils
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
     * 查询今日候诊列表（支持个人/科室视图）
     * 
     * <p><b>【安全特性】</b>强制从 JWT Token 获取医生ID，防止水平越权（IDOR）攻击。
     * 医生只能查看自己或本科室的候诊患者。
     *
     * @param showAll 是否显示科室所有患者（false=个人视图，true=科室视图）
     * @return 候诊列表
     */
    @Operation(
        summary = "查询今日候诊列表（支持个人/科室视图切换）", 
        description = "<b>【安全特性】强制从JWT Token获取医生ID，防止水平越权（IDOR）攻击</b><br/>" +
                      "默认显示分配给当前医生的候诊患者（个人视图），设置showAll=true可查看整个科室的候诊患者（科室视图）。<br/>" +
                      "按排队号升序排列。<br/><br/>" +
                      "<b>响应示例：</b><br/>" +
                      "<pre>{\n" +
                      "  \"code\": 200,\n" +
                      "  \"message\": \"查询成功（个人视图），共3位候诊患者\",\n" +
                      "  \"data\": [\n" +
                      "    {\n" +
                      "      \"id\": 1,\n" +
                      "      \"regNo\": \"REG20231201001\",\n" +
                      "      \"patientName\": \"张三\",\n" +
                      "      \"queueNo\": \"A001\",\n" +
                      "      \"status\": 0,\n" +
                      "      \"statusDesc\": \"待就诊\"\n" +
                      "    }\n" +
                      "  ],\n" +
                      "  \"timestamp\": 1701417600000\n" +
                      "}</pre>"
    )
    @GetMapping("/waiting-list")
    public Result<List<RegistrationVO>> getWaitingList(
            @Parameter(description = "是否显示科室所有患者（false=个人视图，true=科室视图）", required = false, example = "false")
            @RequestParam(defaultValue = "false") boolean showAll) {
        try {
            // ✅ 核心安全逻辑：从 JWT Token 获取医生ID（而非信任前端传参）
            // 这样可以防止攻击者通过修改请求参数查看其他医生的候诊列表（IDOR攻击）
            Long doctorId = SecurityUtils.getCurrentDoctorId();
            
            // 从数据库获取医生信息以获得科室ID
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("医生信息不存在，ID: " + doctorId));
            
            Long deptId = doctor.getDepartment().getMainId();
            
            log.info("【安全】查询候诊列表 - 医生ID: {} (从Token获取), 科室ID: {}, 科室视图: {}", 
                    doctorId, deptId, showAll);
            
            // 调用服务层查询候诊列表
            List<RegistrationVO> waitingList = doctorService.getWaitingList(doctorId, deptId, showAll);
            
            // 返回带有业务说明的响应
            if (waitingList.isEmpty()) {
                String viewMode = showAll ? "科室" : "个人";
                log.info("{} [医生ID: {}, 科室ID: {}] 今日暂无候诊患者", viewMode, doctorId, deptId);
                return Result.success("今日暂无候诊患者", waitingList);
            }
            
            String viewMode = showAll ? "科室视图" : "个人视图";
            log.info("{} [医生ID: {}, 科室ID: {}] 查询到 {} 位候诊患者", 
                    viewMode, doctorId, deptId, waitingList.size());
            return Result.success(
                    String.format("查询成功（%s），共%d位候诊患者", viewMode, waitingList.size()), 
                    waitingList
            );
        } catch (IllegalStateException e) {
            // SecurityUtils 抛出的异常：用户未登录或不是医生角色
            log.error("【安全】获取当前医生ID失败: {}", e.getMessage());
            return Result.unauthorized("认证失败，请重新登录");
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
     * <p><b>【安全特性】</b>验证当前医生是否为该挂号的责任医生，防止越权操作。
     *
     * @param id 挂号记录ID
     * @param status 新状态码（1=已就诊）
     * @return 操作结果
     */
    @Operation(
        summary = "更新就诊状态", 
        description = "<b>【安全特性】强制从JWT Token获取医生ID并验证权限，防止水平越权（IDOR）攻击</b><br/>" +
                      "医生接诊或完成就诊，将挂号状态从待就诊更新为已就诊。<br/>" +
                      "系统会验证当前医生是否是该挂号记录的责任医生，只有责任医生才能更新状态。<br/><br/>" +
                      "<b>状态码说明：</b><br/>" +
                      "0 = 待就诊<br/>" +
                      "1 = 已就诊<br/>" +
                      "2 = 已取消<br/>" +
                      "3 = 已退号<br/><br/>" +
                      "<b>成功响应示例：</b><br/>" +
                      "<pre>{\n" +
                      "  \"code\": 200,\n" +
                      "  \"message\": \"状态更新成功: 已就诊\",\n" +
                      "  \"data\": null,\n" +
                      "  \"timestamp\": 1701417600000\n" +
                      "}</pre><br/>" +
                      "<b>权限不足响应示例：</b><br/>" +
                      "<pre>{\n" +
                      "  \"code\": 400,\n" +
                      "  \"message\": \"无权限操作: 该挂号属于其他医生（ID: 100），您只能操作自己的挂号\",\n" +
                      "  \"data\": null,\n" +
                      "  \"timestamp\": 1701417600000\n" +
                      "}</pre>"
    )
    @PutMapping("/registrations/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "新状态码（0=待就诊, 1=已就诊, 2=已取消, 3=已退号）", required = true, example = "1")
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
            
            // ✅ 核心安全逻辑：验证医生是否有权更新该挂号
            // 从 JWT Token 获取当前医生ID
            Long currentDoctorId = SecurityUtils.getCurrentDoctorId();
            
            // 从数据库获取该挂号记录，检查医生ID是否匹配
            // 这个验证由服务层完成，确保医生只能更新自己患者的挂号
            doctorService.validateAndUpdateStatus(id, currentDoctorId, newStatus);
            
            return Result.success(String.format("状态更新成功: %s", newStatus.getDescription()), null);
        } catch (IllegalStateException e) {
            // SecurityUtils 或 service 层抛出的异常
            log.error("状态更新业务错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("状态更新参数错误或权限检查失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("状态更新系统异常", e);
            return Result.error("系统异常，请联系管理员: " + e.getMessage());
        }
    }
}
