package com.his.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.dto.RegistrationDTO;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.log.utils.LogUtils;
import com.his.service.RegistrationService;
import com.his.vo.RegistrationVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 挂号控制器（护士工作站）
 *
 * <p>负责患者挂号的创建、查询、取消、退费等核心业务功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>患者挂号</b>：支持老患者挂号和新患者建档挂号</li>
 *   <li><b>查询挂号</b>：根据挂号ID查询挂号详情</li>
 *   <li><b>取消挂号</b>：取消未就诊的挂号记录</li>
 *   <li><b>挂号退费</b>：退回已缴纳的挂号费</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要NURSE（护士）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>每个患者每天同一科室只能挂一个号</li>
 *   <li>未就诊的挂号可以取消，已就诊的不能取消</li>
 *   <li>退费时按原支付路径退回</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.RegistrationService
 */
@Tag(name = "护士工作站-挂号管理", description = "护士工作站的患者挂号相关接口，包括挂号、查询、取消等操作")
@Slf4j
@RestController
@RequestMapping("/api/nurse/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * 挂号
     *
     * @param dto 挂号请求数据
     * @return 挂号结果
     */
    @Operation(summary = "患者挂号", description = "创建新的挂号记录，如果患者不存在则自动创建患者档案")
    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    @AuditLog(
        module = "挂号管理",
        action = "患者挂号",
        description = "创建挂号记录",
        auditType = AuditType.BUSINESS
    )
    public Result<RegistrationVO> register(@RequestBody RegistrationDTO dto) {
        try {
            LogUtils.logBusinessOperation("挂号管理", "创建挂号",
                    String.format("科室: %d, 医生: %d", dto.getDeptId(), dto.getDoctorId()));
            RegistrationVO vo = registrationService.register(dto);
            return Result.success("挂号成功", vo);
        } catch (IllegalArgumentException e) {
            LogUtils.logValidationError("挂号", e.getMessage(), dto.toString());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            LogUtils.logSystemError("挂号管理", "挂号失败", e);
            return Result.error("挂号失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询挂号记录
     *
     * @param id 挂号记录 ID
     * @return 挂号信息
     */
    @Operation(summary = "查询挂号记录", description = "根据挂号记录ID查询详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'CASHIER', 'ADMIN')")
    public Result<RegistrationVO> getById(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            log.info("查询挂号记录，ID: {}", id);
            RegistrationVO vo = registrationService.getById(id);
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            log.warn("查询失败: {}", e.getMessage());
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("查询挂号记录失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 取消挂号
     *
     * @param id 挂号记录 ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @Operation(summary = "取消挂号", description = "取消指定的挂号记录，状态变更为已取消")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('NURSE', 'CASHIER', 'ADMIN')")
    public Result<Void> cancel(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "取消原因", example = "患者临时有事")
            @RequestParam(required = false) String reason) {
        try {
            log.info("取消挂号，ID: {}, 原因: {}", id, reason);
            registrationService.cancel(id, reason);
            return Result.success("挂号已取消", null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("取消挂号失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("取消挂号失败", e);
            return Result.error("取消失败: " + e.getMessage());
        }
    }

    /**
     * 退费
     *
     * @param id 挂号记录 ID
     * @return 操作结果
     */
    @Operation(summary = "挂号退费", description = "将已取消的挂号记录标记为已退费状态")
    @PutMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    @AuditLog(
        module = "挂号管理",
        action = "挂号退费",
        description = "取消挂号并退费",
        auditType = AuditType.SENSITIVE_OPERATION
    )
    public Result<Void> refund(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            LogUtils.logSensitiveOperation("挂号退费", "挂号记录", id);
            registrationService.refund(id);
            return Result.success("退费成功", null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LogUtils.logValidationError("退费", e.getMessage(), "挂号ID: " + id);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            LogUtils.logSystemError("挂号管理", "退费失败", e);
            return Result.error("退费失败: " + e.getMessage());
        }
    }
}
