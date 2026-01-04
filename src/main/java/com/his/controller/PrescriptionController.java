package com.his.controller;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.dto.PrescriptionDTO;
import com.his.entity.Prescription;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.log.utils.LogUtils;
import com.his.service.PrescriptionService;
import com.his.vo.PrescriptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处方管理控制器（医生工作站）
 *
 * <p>负责处方的创建、审核、查询等全生命周期管理</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>创建处方</b>：医生根据挂号单开具处方</li>
 *   <li><b>查询处方</b>：根据挂号单、处方ID查询处方信息</li>
 *   <li><b>处方提交</b>：将草稿状态的处方提交为正式处方</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要DOCTOR（医生）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>处方必须关联有效的挂号单</li>
 *   <li>已提交的处方不能修改</li>
 *   <li>处方自动计算总金额</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.PrescriptionService
 */
@Tag(name = "医生工作站-处方管理", description = "医生工作站的处方相关接口")
@Slf4j
@RestController
@RequestMapping("/api/doctor/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 创建处方
     *
     * @param dto 处方数据
     * @return 处方信息
     */
    @Operation(summary = "创建处方", description = "根据挂号单ID和药品列表创建处方，自动从数据库读取药品单价并计算总金额")
    @PostMapping("/create")
    @AuditLog(
        module = "处方管理",
        action = "开具处方",
        description = "医生开具处方",
        auditType = AuditType.BUSINESS
    )
    public Result<PrescriptionVO> createPrescription(@RequestBody PrescriptionDTO dto) {
        try {
            LogUtils.logBusinessOperation("处方管理", "开具处方",
                    String.format("挂号单: %d, 药品数: %d",
                            dto.getRegistrationId(),
                            dto.getItems() != null ? dto.getItems().size() : 0));
            Prescription prescription = prescriptionService.createPrescription(dto);
            PrescriptionVO vo = VoConverter.toPrescriptionVO(prescription);
            return Result.success("处方创建成功", vo);
        } catch (IllegalArgumentException e) {
            LogUtils.logValidationError("处方", e.getMessage(), dto.toString());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            LogUtils.logSystemError("处方管理", "创建处方失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询处方
     *
     * @param id 处方ID
     * @return 处方信息
     */
    @Operation(summary = "查询处方详情", description = "根据处方ID查询详细信息")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    @GetMapping("/{id}")
    public Result<PrescriptionVO> getById(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        try {
            log.info("收到查询处方请求，ID: {}", id);
            Prescription prescription = prescriptionService.getById(id);
            PrescriptionVO vo = VoConverter.toPrescriptionVO(prescription);
            return Result.success("查询成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("查询处方失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询处方失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据病历ID查询处方列表
     *
     * @param recordId 病历ID
     * @return 处方列表
     */
    @Operation(summary = "查询病历的处方列表", description = "根据病历ID查询所有处方")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    @GetMapping("/by-record/{recordId}")
    public Result<List<PrescriptionVO>> getByRecordId(
            @Parameter(description = "病历ID", required = true, example = "1")
            @PathVariable("recordId") Long recordId) {
        try {
            log.info("收到根据病历查询处方列表请求，病历ID: {}", recordId);
            List<Prescription> prescriptions = prescriptionService.getByRecordId(recordId);
            List<PrescriptionVO> voList = prescriptions.stream()
                .map(VoConverter::toPrescriptionVO)
                .collect(Collectors.toList());
            return Result.success("查询成功", voList);
        } catch (IllegalArgumentException e) {
            log.warn("查询处方列表失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询处方列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 审核处方
     *
     * @param id 处方ID
     * @param reviewDoctorId 审核医生ID
     * @param remark 审核备注
     * @return 操作结果
     */
    @Operation(summary = "审核处方", description = "对已开方的处方进行审核（仅药师和管理员）")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")  // 覆盖类级别权限，只允许药师和管理员审核
    @PostMapping("/{id}/review")
    public Result<Void> review(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Parameter(description = "审核医生ID", required = true, example = "1")
            @RequestParam("reviewDoctorId") Long reviewDoctorId,
            @Parameter(description = "审核备注", example = "处方合理，准予发药")
            @RequestParam(value = "remark", required = false) String remark) {
        try {
            log.info("收到审核处方请求，ID: {}, 审核医生ID: {}", id, reviewDoctorId);
            prescriptionService.review(id, reviewDoctorId, remark);
            return Result.success("审核成功", null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("审核处方失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("审核处方失败", e);
            return Result.error("审核失败: " + e.getMessage());
        }
    }
}
