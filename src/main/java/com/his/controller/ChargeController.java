package com.his.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.service.ChargeService;
import com.his.vo.ChargeVO;
import com.his.vo.DailySettlementVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 收费管理控制器
 *
 * <p>负责收费、支付、退费、日结算等收费相关的核心业务功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>创建收费单</b>：支持挂号费、处方费、混合收费等多种模式</li>
 *   <li><b>支付处理</b>：支持现金、医保、银行卡等多种支付方式，含幂等性保证</li>
 *   <li><b>退费处理</b>：按原支付路径退费，支持部分退费和全额退费</li>
 *   <li><b>日结算</b>：生成收费员的日结算报表，统计当日收费情况</li>
 *   <li><b>收费查询</b>：支持按挂号单、处方单、患者等多维度查询收费记录</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要CASHIER（收费员）或ADMIN（管理员）角色</p>
 *
 * <h3>审计日志</h3>
 * <p>所有收费、支付、退费操作都会记录审计日志，便于追踪和审计</p>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.ChargeService
 */
@Tag(name = "收费管理", description = "收费、支付、退费及结算相关接口")
@Slf4j
@RestController
@RequestMapping("/api/cashier/charges")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
public class ChargeController {

    private final ChargeService chargeService;

    @Operation(
        summary = "创建收费单",
        description = """
            创建收费单，支持以下模式：
            1. **仅挂号费**：prescriptionIds为空
            2. **仅处方费**：prescriptionIds不为空，挂号费已支付
            3. **合并收费**：同时收取挂号费和处方费（向后兼容）

            **业务规则**：
            - 处方收费：挂号单状态需为COMPLETED(1)，处方状态需为REVIEWED(2)
            - 防止重复收费：检查是否已支付挂号费/处方费
            - 幂等性保证：通过transactionNo防止重复支付
            """
    )
    @AuditLog(
        module = "收费管理",
        action = "创建收费单",
        description = "创建挂号费、处方费或合并收费单",
        auditType = AuditType.BUSINESS
    )
    @PostMapping
    public Result<ChargeVO> createCharge(@Valid @RequestBody CreateChargeDTO dto) {
        log.info("收到创建收费单请求: {}", dto);
        return Result.success("收费单创建成功", chargeService.createCharge(dto));
    }

    @Operation(
        summary = "查询收费单详情",
        description = "根据收费单ID查询详细信息，包括收费明细列表"
    )
    @GetMapping("/{id}")
    public Result<ChargeVO> getById(
            @Parameter(description = "收费单ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        return Result.success("查询成功", chargeService.getById(id));
    }

    @Operation(
        summary = "查询收费单列表",
        description = "支持分页查询和多条件筛选（收费单号、患者ID、状态、日期范围）"
    )
    @GetMapping
    public Result<Page<ChargeVO>> queryCharges(
            @Parameter(description = "收费单号（模糊查询）", example = "CHG20250101001")
            @RequestParam(value = "chargeNo", required = false) String chargeNo,
            @Parameter(description = "患者ID", example = "1")
            @RequestParam(value = "patientId", required = false) Long patientId,
            @Parameter(description = "状态（0=未支付, 1=已支付, 2=已退费）", example = "1")
            @RequestParam(value = "status", required = false) Integer status,
            @Parameter(description = "开始日期（yyyy-MM-dd）", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success("查询成功", chargeService.queryCharges(chargeNo, patientId, status, startDate, endDate, pageable));
    }

    @Operation(
        summary = "确认支付",
        description = """
            处理收费单支付，支持多种支付方式。
            **业务规则**：
            - 验证收费单状态（必须为未支付）
            - 验证支付金额（必须与应收金额一致）
            - 幂等性保证：通过transactionNo防止重复支付
            - 支付成功后更新收费单状态为PAID
            - 支付挂号费时更新挂号状态为PAID_REGISTRATION
            - 支付处方费时更新处方状态为PAID
            """
    )
    @AuditLog(
        module = "收费管理",
        action = "确认支付",
        description = "处理收费单支付，更新收费单和相关业务状态",
        auditType = AuditType.BUSINESS
    )
    @PostMapping("/{id}/pay")
    public Result<ChargeVO> processPayment(
            @Parameter(description = "收费单ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody PaymentDTO dto) {
        log.info("收到支付请求，ID: {}, 支付信息: {}", id, dto);
        return Result.success("支付成功", chargeService.processPayment(id, dto));
    }

    @Operation(
        summary = "处理退费",
        description = """
            处理收费单退费。
            **业务规则**：
            - 验证收费单状态（必须为已支付）
            - 更新收费单状态为REFUNDED
            - 根据处方状态决定是否恢复库存（已发药则不恢复）
            - 更新处方状态为REFUNDED
            """
    )
    @AuditLog(
        module = "收费管理",
        action = "处理退费",
        description = "处理收费单退费，更新状态并恢复库存",
        auditType = AuditType.SENSITIVE_OPERATION
    )
    @PostMapping("/{id}/refund")
    public Result<ChargeVO> processRefund(
            @Parameter(description = "收费单ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @RequestBody RefundRequest refundRequest) {
        log.info("收到退费请求，ID: {}, 原因: {}", id, refundRequest.getRefundReason());
        return Result.success("退费成功", chargeService.processRefund(id, refundRequest.getRefundReason()));
    }

    // ========== 分阶段收费新增API端点 ==========

    @Operation(
        summary = "创建挂号收费单（仅挂号费）",
        description = """
            为挂号单创建仅包含挂号费的收费单。
            **业务规则**：
            - 挂号单状态必须为 WAITING(0) 或 PAID_REGISTRATION(4)
            - 挂号费金额必须大于0
            - 未收取过挂号费（防止重复收费）
            **使用场景**：患者挂号时立即收取挂号费
            """
    )
    @AuditLog(
        module = "收费管理",
        action = "创建挂号收费单",
        description = "为挂号单创建仅包含挂号费的收费单",
        auditType = AuditType.BUSINESS
    )
    @PostMapping("/registration/{registrationId}")
    public Result<ChargeVO> createRegistrationCharge(
            @Parameter(
                description = "挂号单ID",
                required = true,
                example = "1"
            )
            @PathVariable("registrationId") Long registrationId) {
        log.info("收到创建挂号收费单请求，挂号ID: {}", registrationId);
        return Result.success("挂号收费单创建成功", chargeService.createRegistrationCharge(registrationId));
    }

    @Operation(
        summary = "创建处方收费单（仅处方费）",
        description = """
            为挂号单创建仅包含处方费的收费单。
            **业务规则**：
            - 挂号单状态必须为 COMPLETED(1)
            - 处方状态必须为 REVIEWED(2)
            - 未收取过该处方的费用（防止重复收费）
            **使用场景**：医生开处方并审核通过后，患者支付处方费
            """
    )
    @AuditLog(
        module = "收费管理",
        action = "创建处方收费单",
        description = "为已审核的处方创建收费单",
        auditType = AuditType.BUSINESS
    )
    @PostMapping("/prescription")
    public Result<ChargeVO> createPrescriptionCharge(@Valid @RequestBody CreateChargeDTO dto) {
        log.info("收到创建处方收费单请求: {}", dto);
        return Result.success("处方收费单创建成功",
            chargeService.createPrescriptionCharge(dto.getRegistrationId(), dto.getPrescriptionIds()));
    }

    @Operation(
        summary = "检查挂号费是否已支付",
        description = """
            查询指定挂号单的挂号费支付状态。
            **返回值**：
            - true: 挂号费已支付
            - false: 挂号费未支付
            **使用场景**：前端判断是否允许患者进入就诊流程
            """
    )
    @GetMapping("/registration/{registrationId}/payment-status")
    public Result<Boolean> checkRegistrationPaymentStatus(
            @Parameter(
                description = "挂号单ID",
                required = true,
                example = "1"
            )
            @PathVariable("registrationId") Long registrationId) {
        boolean isPaid = chargeService.isRegistrationFeePaid(registrationId);
        return Result.success("查询成功", isPaid);
    }

    @Operation(
        summary = "获取挂号单的所有收费记录（按类型分组）",
        description = """
            查询挂号单的所有收费记录，按类型分组返回。
            **返回结构**：
            - registration: 挂号费收费单列表
            - prescription: 处方费收费单列表
            - combined: 混合收费单列表（挂号费+处方费）
            **使用场景**：查看患者所有费用明细，支持对账和统计
            """
    )
    @GetMapping("/registration/{registrationId}/by-type")
    public Result<Map<String, List<ChargeVO>>> getChargesByType(
            @Parameter(
                description = "挂号单ID",
                required = true,
                example = "1"
            )
            @PathVariable("registrationId") Long registrationId) {
        return Result.success("查询成功", chargeService.getChargesByType(registrationId));
    }

    @Operation(
        summary = "每日结算报表",
        description = """
            生成每日收费结算报表，包括：
            - 总收费笔数和金额
            - 各支付方式的统计（现金、银行卡、微信、支付宝等）
            - 退费统计
            - 净收入统计

            **使用场景**：每日收费员对账、财务统计
            """
    )
    @GetMapping("/statistics/daily")
    public Result<DailySettlementVO> getDailySettlement(
            @Parameter(
                description = "目标日期（yyyy-MM-dd），不传则查询今日",
                example = "2025-01-01"
            )
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success("查询成功", chargeService.getDailySettlement(date));
    }

    @Data
    public static class RefundRequest {
        private String refundReason;
    }
}
