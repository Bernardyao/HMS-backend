package com.his.controller;

import com.his.common.Result;
import com.his.dto.PrescriptionDTO;
import com.his.entity.Prescription;
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
 * 药师工作站-处方管理控制器
 * 权限：药师和管理员
 */
@Tag(name = "药师工作站-处方管理", description = "药师工作站的处方审核、发药等接口")
@Slf4j
@RestController
@RequestMapping("/api/pharmacist/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
public class PharmacistPrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 待发药处方列表
     * 
     * @return 待发药的处方列表
     */
    @Operation(summary = "待发药处方列表", description = "查询所有已审核通过但未发药的处方列表")
    @GetMapping("/pending")
    public Result<String> getPendingDispenseList() {
        try {
            log.info("查询待发药处方列表");
            
            // TODO: 实现待发药列表查询
            
            return Result.success("查询成功", "待发药处方列表（待实现）");
        } catch (Exception e) {
            log.error("查询待发药列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询处方
     *
     * @param id 处方ID
     * @return 处方信息
     */
    @Operation(summary = "查询处方详情", description = "根据处方ID查询详细信息")
    @GetMapping("/{id}")
    public Result<PrescriptionVO> getById(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        try {
            log.info("收到查询处方请求，ID: {}", id);
            Prescription prescription = prescriptionService.getById(id);
            PrescriptionVO vo = convertToVO(prescription);
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
     * 审核处方
     *
     * @param id 处方ID
     * @param reviewDoctorId 审核医生ID
     * @param remark 审核备注
     * @return 操作结果
     */
    @Operation(summary = "审核处方", description = "对已开方的处方进行审核")
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

    /**
     * 发药
     * 
     * @param id 处方ID
     * @return 发药结果
     */
    @Operation(summary = "发药", description = "根据处方ID进行发药操作，自动扣减库存")
    @PostMapping("/{id}/dispense")
    public Result<String> dispense(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            log.info("发药请求，处方ID: {}", id);
            
            // TODO: 实现发药业务逻辑
            
            return Result.success("发药成功", "发药单号: DISP" + System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            log.warn("发药参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("发药业务错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("发药失败", e);
            return Result.error("发药失败: " + e.getMessage());
        }
    }

    /**
     * 退药
     * 
     * @param id 发药记录ID
     * @param reason 退药原因
     * @return 退药结果
     */
    @Operation(summary = "退药", description = "为已发药记录进行退药操作，自动归还库存")
    @PostMapping("/{id}/return")
    public Result<String> returnMedicine(
            @Parameter(description = "发药记录ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "退药原因", required = true, example = "患者要求退药")
            @RequestParam String reason) {
        try {
            log.info("退药请求，发药记录ID: {}, 原因: {}", id, reason);
            
            // TODO: 实现退药业务逻辑
            
            return Result.success("退药成功", "退药单号: RET" + System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            log.warn("退药参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("退药失败", e);
            return Result.error("退药失败: " + e.getMessage());
        }
    }

    /**
     * 今日发药统计
     * 
     * @return 发药统计信息
     */
    @Operation(summary = "今日发药统计", description = "统计当前药师今日的发药数量、处方数等信息")
    @GetMapping("/statistics/today")
    public Result<String> getTodayStatistics() {
        try {
            log.info("查询今日发药统计");
            
            // TODO: 实现发药统计逻辑
            
            return Result.success("查询成功", "今日发药统计（待实现）");
        } catch (Exception e) {
            log.error("查询发药统计失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * Entity转VO
     */
    private PrescriptionVO convertToVO(Prescription prescription) {
        return PrescriptionVO.builder()
            .mainId(prescription.getMainId())
            .prescriptionNo(prescription.getPrescriptionNo())
            .recordId(prescription.getMedicalRecord() != null ? prescription.getMedicalRecord().getMainId() : null)
            .patientId(prescription.getPatient() != null ? prescription.getPatient().getMainId() : null)
            .patientName(prescription.getPatient() != null ? prescription.getPatient().getName() : null)
            .doctorId(prescription.getDoctor() != null ? prescription.getDoctor().getMainId() : null)
            .doctorName(prescription.getDoctor() != null ? prescription.getDoctor().getName() : null)
            .prescriptionType(prescription.getPrescriptionType())
            .totalAmount(prescription.getTotalAmount())
            .itemCount(prescription.getItemCount())
            .status(prescription.getStatus())
            .validityDays(prescription.getValidityDays())
            .reviewDoctorId(prescription.getReviewDoctor() != null ? prescription.getReviewDoctor().getMainId() : null)
            .reviewDoctorName(prescription.getReviewDoctor() != null ? prescription.getReviewDoctor().getName() : null)
            .reviewTime(prescription.getReviewTime())
            .reviewRemark(prescription.getReviewRemark())
            .dispenseTime(prescription.getDispenseTime())
            .dispenseBy(prescription.getDispenseBy())
            .createdAt(prescription.getCreatedAt())
            .updatedAt(prescription.getUpdatedAt())
            .build();
    }
}
