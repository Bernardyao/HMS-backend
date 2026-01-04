package com.his.controller;

import com.his.common.Result;
import com.his.common.SecurityUtils;
import com.his.converter.VoConverter;
import com.his.entity.Prescription;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
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


/**
 * 药师工作站-处方管理控制器
 *
 * <p>为药师工作站提供处方审核、发药、退药等核心功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>待发药列表</b>：查询所有已审核通过但未发药的处方</li>
 *   <li><b>处方发药</b>：根据处方发药，扣减药品库存</li>
 *   <li><b>处方退药</b>：处理退药申请，恢复药品库存</li>
 *   <li><b>今日统计</b>：查询今日发药统计数据</li>
 * </ul>
 *
 * <h3>重要说明</h3>
 * <p>本控制器只包含操作类接口，处方查询接口已迁移至 {@link com.his.controller.CommonPrescriptionController}</p>
 * <p>查询处方请使用：<code>GET /api/common/prescriptions</code></p>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要PHARMACIST（药师）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>只能发药已审核通过的处方</li>
 *   <li>发药时验证库存是否充足</li>
 *   <li>发药后自动扣减库存</li>
 *   <li>退药时恢复库存</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 * @see com.his.service.PrescriptionService
 * @see com.his.controller.CommonPrescriptionController
 */
@Tag(name = "药师工作站-处方管理", description = "药师工作站的处方操作接口（查询功能已迁移至/common）")
@Slf4j
@RestController
@RequestMapping("/api/pharmacist/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
public class PharmacistPrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 待发药处方列表
     * <p>查询所有已审核通过但未发药的处方列表，用于药师工作站的待发药队列</p>
     *
     * @return 待发药的处方列表
     */
    @Operation(summary = "待发药处方列表", description = "查询所有已审核通过但未发药的处方列表")
    @GetMapping("/pending")
    public Result<List<PrescriptionVO>> getPendingDispenseList() {
        log.info("【药师】查询待发药处方列表");

        List<Prescription> prescriptions = prescriptionService.getPendingDispenseList();
        List<PrescriptionVO> vos = prescriptions.stream()
                .map(VoConverter::toPrescriptionVO)
                .collect(java.util.stream.Collectors.toList());

        return Result.success(
            String.format("查询成功，共 %d 张待发药处方", vos.size()),
            vos
        );
    }

    /**
     * 审核处方
     * <p>对已开方的处方进行审核，审核通过后才能进行收费和发药</p>
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

        log.info("【药师】审核处方 - ID: {}, 审核医生ID: {}", id, reviewDoctorId);
        prescriptionService.review(id, reviewDoctorId, remark);

        return Result.success("审核成功", null);
    }

    /**
     * 发药
     * <p>根据处方ID进行发药操作，自动扣减药品库存</p>
     *
     * @param id 处方ID
     * @return 发药结果
     */
    @Operation(summary = "发药", description = "根据处方ID进行发药操作，自动扣减库存")
    @PostMapping("/{id}/dispense")
    @AuditLog(
        module = "药房管理",
        action = "发药",
        description = "药师发药",
        auditType = AuditType.BUSINESS
    )
    public Result<String> dispense(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable Long id) {

        log.info("【药师】发药 - 处方ID: {}", id);

        // 获取当前登录用户ID作为发药人
        Long pharmacistId = SecurityUtils.getCurrentUserId();
        prescriptionService.dispense(id, pharmacistId);

        return Result.success("发药成功", "发药成功");
    }

    /**
     * 退药
     * <p>为已发药记录进行退药操作，自动归还库存</p>
     *
     * @param id 处方ID
     * @param reason 退药原因
     * @return 退药结果
     */
    @Operation(summary = "退药", description = "为已发药记录进行退药操作，自动归还库存")
    @PostMapping("/{id}/return")
    @AuditLog(
        module = "药房管理",
        action = "退药",
        description = "患者退药",
        auditType = AuditType.SENSITIVE_OPERATION
    )
    public Result<String> returnMedicine(
            @Parameter(description = "处方ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "退药原因", required = true, example = "患者要求退药")
            @RequestParam String reason) {

        log.info("【药师】退药 - 处方ID: {}, 原因: {}", id, reason);
        prescriptionService.returnMedicine(id, reason);

        return Result.success("退药成功", null);
    }

    /**
     * 今日发药统计
     * <p>统计当前药师今日的发药数量、处方数等信息</p>
     *
     * @return 发药统计信息
     */
    @Operation(summary = "今日发药统计", description = "统计当前药师今日的发药数量、处方数等信息")
    @GetMapping("/statistics/today")
    public Result<com.his.dto.PharmacistStatisticsDTO> getTodayStatistics() {
        log.info("【药师】查询今日发药统计");

        Long pharmacistId = SecurityUtils.getCurrentUserId();
        com.his.dto.PharmacistStatisticsDTO stats = prescriptionService.getPharmacistStatistics(pharmacistId);

        return Result.success("查询成功", stats);
    }
}
