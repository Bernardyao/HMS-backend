package com.his.controller;

import com.his.common.Result;
import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.service.ChargeService;
import com.his.vo.ChargeVO;
import com.his.vo.DailySettlementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;

/**
 * 收费管理控制器
 */
@Tag(name = "收费管理", description = "收费、支付、退费及结算相关接口")
@Slf4j
@RestController
@RequestMapping("/api/cashier/charges")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
public class ChargeController {

    private final ChargeService chargeService;

    @Operation(summary = "创建收费单")
    @PostMapping
    public Result<ChargeVO> createCharge(@Valid @RequestBody CreateChargeDTO dto) {
        log.info("收到创建收费单请求: {}", dto);
        return Result.success("收费单创建成功", chargeService.createCharge(dto));
    }

    @Operation(summary = "查询收费单详情")
    @GetMapping("/{id}")
    public Result<ChargeVO> getById(@PathVariable("id") Long id) {
        return Result.success("查询成功", chargeService.getById(id));
    }

    @Operation(summary = "查询收费单列表")
    @GetMapping
    public Result<Page<ChargeVO>> queryCharges(
            @RequestParam(value = "chargeNo", required = false) String chargeNo,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success("查询成功", chargeService.queryCharges(chargeNo, patientId, status, startDate, endDate, pageable));
    }

    @Operation(summary = "确认支付")
    @PostMapping("/{id}/pay")
    public Result<ChargeVO> processPayment(
            @PathVariable("id") Long id,
            @Valid @RequestBody PaymentDTO dto) {
        log.info("收到支付请求，ID: {}, 支付信息: {}", id, dto);
        return Result.success("支付成功", chargeService.processPayment(id, dto));
    }

    @Operation(summary = "处理退费")
    @PostMapping("/{id}/refund")
    public Result<ChargeVO> processRefund(
            @PathVariable("id") Long id,
            @RequestBody RefundRequest refundRequest) {
        log.info("收到退费请求，ID: {}, 原因: {}", id, refundRequest.getRefundReason());
        return Result.success("退费成功", chargeService.processRefund(id, refundRequest.getRefundReason()));
    }

    @Operation(summary = "每日结算报表")
    @GetMapping("/statistics/daily")
    public Result<DailySettlementVO> getDailySettlement(
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
