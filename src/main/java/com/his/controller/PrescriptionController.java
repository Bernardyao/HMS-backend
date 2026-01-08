package com.his.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.dto.PrescriptionDTO;
import com.his.entity.Prescription;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.service.PrescriptionService;
import com.his.vo.PrescriptionVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 处方管理控制器（医生工作站）
 *
 * <p>负责处方的创建等操作</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>创建处方</b>：医生根据挂号单开具处方</li>
 * </ul>
 *
 * <h3>重要说明</h3>
 * <p>本控制器只包含操作类接口，处方查询接口已迁移至 {@link com.his.controller.CommonPrescriptionController}</p>
 * <p>查询处方请使用：<code>GET /api/common/prescriptions</code></p>
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
 * @version 2.0
 * @since 2.0
 * @see com.his.service.PrescriptionService
 * @see com.his.controller.CommonPrescriptionController
 */
@Tag(name = "医生工作站-处方管理", description = "医生工作站的处方操作接口（查询功能已迁移至/common）")
@Slf4j
@RestController
@RequestMapping("/api/doctor/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 创建处方
     * <p>根据挂号单ID和药品列表创建处方，自动从数据库读取药品单价并计算总金额</p>
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
        log.info("【医生】创建处方 - 挂号单: {}, 药品数: {}",
                dto.getRegistrationId(),
                dto.getItems() != null ? dto.getItems().size() : 0);

        Prescription prescription = prescriptionService.createPrescription(dto);
        PrescriptionVO vo = VoConverter.toPrescriptionVO(prescription);

        return Result.success("处方创建成功", vo);
    }
}
