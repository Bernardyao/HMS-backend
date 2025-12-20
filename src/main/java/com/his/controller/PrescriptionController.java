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
 * 处方管理控制器
 * 权限：医生和管理员
 */
@Tag(name = "处方管理", description = "处方相关接口")
@Slf4j
@RestController
@RequestMapping("/api/prescription")
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
    public Result<PrescriptionVO> createPrescription(@RequestBody PrescriptionDTO dto) {
        try {
            log.info("收到创建处方请求，挂号单ID: {}, 药品数量: {}", 
                    dto.getRegistrationId(), dto.getItems() != null ? dto.getItems().size() : 0);
            Prescription prescription = prescriptionService.createPrescription(dto);
            PrescriptionVO vo = convertToVO(prescription);
            return Result.success("处方创建成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("创建处方失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("创建处方失败", e);
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
                .map(this::convertToVO)
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
