package com.his.controller;

import com.his.common.Result;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.vo.MedicineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 药品管理控制器
 * 权限：医生和管理员可以查询药品，药师可以管理库存
 */
@Tag(name = "药品管理", description = "药品相关接口，包括搜索、库存查询等操作")
@Slf4j
@RestController
@RequestMapping(value = "/api/medicine", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    /**
     * 搜索药品（根据名称或编码）
     * 权限：医生、药师、管理员
     *
     * @param keyword 关键字
     * @return 药品列表
     */
    @Operation(summary = "搜索药品", description = "根据药品名称或编码模糊搜索药品信息")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public Result<List<MedicineVO>> search(
            @Parameter(description = "关键字（药品名称或编码）", example = "阿莫西林")
            @RequestParam(required = false) String keyword) {
        
        log.info("搜索药品，关键字: {}", keyword);
        
        List<Medicine> medicines = medicineService.searchMedicines(keyword);
        List<MedicineVO> voList = medicines.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return Result.success("查询成功", voList);
    }

    /**
     * 根据ID查询药品
     * 权限：医生、药师、管理员
     *
     * @param id 药品ID
     * @return 药品信息
     */
    @Operation(summary = "查询药品详情", description = "根据药品ID查询详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public Result<MedicineVO> getById(
            @Parameter(description = "药品ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        
        log.info("查询药品详情，ID: {}", id);
        
        Medicine medicine = medicineService.getById(id);
        MedicineVO vo = convertToVO(medicine);
        
        return Result.success("查询成功", vo);
    }

    /**
     * 发药
     * 权限：仅药师
     * 
     * @param prescriptionId 处方ID
     * @return 发药结果
     */
    @Operation(summary = "发药", description = "根据处方ID进行发药操作，自动扣减库存")
    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public Result<String> dispenseMedicine(
            @Parameter(description = "处方ID", required = true, example = "1")
            @RequestParam Long prescriptionId) {
        try {
            log.info("发药请求，处方ID: {}", prescriptionId);
            
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
     * 权限：仅药师
     * 
     * @param dispenseId 发药记录ID
     * @param reason 退药原因
     * @return 退药结果
     */
    @Operation(summary = "退药", description = "为已发药记录进行退药操作，自动归还库存")
    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public Result<String> returnMedicine(
            @Parameter(description = "发药记录ID", required = true, example = "1")
            @RequestParam Long dispenseId,
            @Parameter(description = "退药原因", required = true, example = "患者要求退药")
            @RequestParam String reason) {
        try {
            log.info("退药请求，发药记录ID: {}, 原因: {}", dispenseId, reason);
            
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
     * 查询药品库存
     * 权限：仅药师
     * 
     * @param keyword 药品名称关键字（可选）
     * @param lowStock 是否只查询低库存药品
     * @return 库存列表
     */
    @Operation(summary = "查询药品库存", description = "查询药品库存信息，支持关键字搜索和低库存筛选")
    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public Result<String> getStock(
            @Parameter(description = "药品名称关键字（可选）", example = "阿莫西林")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "是否只查询低库存药品", example = "false")
            @RequestParam(defaultValue = "false") boolean lowStock) {
        try {
            log.info("查询药品库存，关键字: {}, 低库存: {}", keyword, lowStock);
            
            // TODO: 实现库存查询逻辑
            
            return Result.success("查询成功", "库存列表（待实现）");
        } catch (Exception e) {
            log.error("查询库存失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 待发药列表
     * 
     * @return 待发药的处方列表
     */
    @Operation(summary = "待发药列表", description = "查询所有已缴费但未发药的处方列表")
    @GetMapping("/pending")
    public Result<String> getPendingDispenseList() {
        try {
            log.info("查询待发药列表");
            
            // TODO: 实现待发药列表查询
            
            return Result.success("查询成功", "待发药列表（待实现）");
        } catch (Exception e) {
            log.error("查询待发药列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
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
    private MedicineVO convertToVO(Medicine medicine) {
        return MedicineVO.builder()
            .mainId(medicine.getMainId())
            .medicineCode(medicine.getMedicineCode())
            .name(medicine.getName())
            .genericName(medicine.getGenericName())
            .retailPrice(medicine.getRetailPrice())
            .stockQuantity(medicine.getStockQuantity())
            .status(medicine.getStatus())
            .specification(medicine.getSpecification())
            .unit(medicine.getUnit())
            .dosageForm(medicine.getDosageForm())
            .manufacturer(medicine.getManufacturer())
            .category(medicine.getCategory())
            .isPrescription(medicine.getIsPrescription())
            .createdAt(medicine.getCreatedAt())
            .updatedAt(medicine.getUpdatedAt())
            .build();
    }}
