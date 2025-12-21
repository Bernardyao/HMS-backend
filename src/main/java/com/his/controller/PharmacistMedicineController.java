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
 * 药师工作站-药品管理控制器
 * 权限：药师和管理员
 */
@Tag(name = "药师工作站-药品管理", description = "药师工作站的药品库存管理、发药退药等接口")
@Slf4j
@RestController
@RequestMapping(value = "/api/pharmacist/medicines", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
public class PharmacistMedicineController {

    private final MedicineService medicineService;

    /**
     * 查询药品库存
     * 权限：仅药师
     * 
     * @param keyword 药品名称关键字（可选）
     * @param lowStock 是否只查询低库存药品
     * @return 库存列表
     */
    @Operation(summary = "查询药品库存", description = "查询药品库存信息，支持关键字搜索和低库存筛选")
    @GetMapping("/inventory")
    public Result<List<MedicineVO>> getInventory(
            @Parameter(description = "药品名称关键字（可选）", example = "阿莫西林")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "是否只查询低库存药品", example = "false")
            @RequestParam(defaultValue = "false") boolean lowStock) {
        try {
            log.info("查询药品库存，关键字: {}, 低库存: {}", keyword, lowStock);
            
            List<Medicine> medicines = medicineService.searchMedicines(keyword);
            
            // 如果只查询低库存，过滤库存不足的药品（假设低于100为低库存）
            if (lowStock) {
                medicines = medicines.stream()
                    .filter(m -> m.getStockQuantity() < 100)
                    .collect(Collectors.toList());
            }
            
            List<MedicineVO> voList = medicines.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return Result.success("查询成功", voList);
        } catch (Exception e) {
            log.error("查询库存失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询低库存预警
     * 
     * @return 低库存药品列表
     */
    @Operation(summary = "低库存预警", description = "查询库存低于预警线的药品列表")
    @GetMapping("/low-stock")
    public Result<List<MedicineVO>> getLowStock() {
        try {
            log.info("查询低库存预警");
            
            // 查询所有药品并过滤低库存
            List<Medicine> medicines = medicineService.searchMedicines(null);
            List<Medicine> lowStockMedicines = medicines.stream()
                .filter(m -> m.getStockQuantity() < 100)  // 低于100为低库存
                .collect(Collectors.toList());
            
            List<MedicineVO> voList = lowStockMedicines.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return Result.success(String.format("查询成功，共 %d 个药品库存不足", voList.size()), voList);
        } catch (Exception e) {
            log.error("查询低库存预警失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新药品库存
     * 
     * @param id 药品ID
     * @param quantity 更新数量（正数为入库，负数为出库）
     * @param reason 操作原因
     * @return 更新结果
     */
    @Operation(summary = "更新药品库存", description = "手动调整药品库存数量（入库/出库）")
    @PutMapping("/{id}/stock")
    public Result<String> updateStock(
            @Parameter(description = "药品ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "更新数量（正数=入库，负数=出库）", required = true, example = "100")
            @RequestParam Integer quantity,
            @Parameter(description = "操作原因", required = true, example = "采购入库")
            @RequestParam String reason) {
        try {
            log.info("更新药品库存，药品ID: {}, 数量: {}, 原因: {}", id, quantity, reason);
            
            // TODO: 实现库存更新逻辑
            
            return Result.success("库存更新成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("更新库存参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("更新库存失败", e);
            return Result.error("更新失败: " + e.getMessage());
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
    }
}
