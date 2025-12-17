package com.his.controller;

import com.his.common.Result;
import com.his.dto.RegistrationDTO;
import com.his.service.RegistrationService;
import com.his.vo.RegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 挂号控制器
 */
@Tag(name = "挂号管理", description = "患者挂号相关接口，包括挂号、查询、取消等操作")
@Slf4j
@RestController
@RequestMapping("/api/registrations")
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
    public Result<RegistrationVO> register(@RequestBody RegistrationDTO dto) {
        try {
            log.info("收到挂号请求: {}", dto);
            RegistrationVO vo = registrationService.register(dto);
            return Result.success("挂号成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("挂号参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("挂号失败", e);
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
    public Result<Void> refund(
            @Parameter(description = "挂号记录ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            log.info("挂号退费，ID: {}", id);
            registrationService.refund(id);
            return Result.success("退费成功", null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("退费失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("退费失败", e);
            return Result.error("退费失败: " + e.getMessage());
        }
    }
}
