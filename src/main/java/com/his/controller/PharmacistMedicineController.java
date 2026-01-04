package com.his.controller;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.dto.InventoryStatsVO;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.vo.MedicineVO;
import com.his.vo.PharmacistMedicineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 药师工作站-药品管理控制器
 *
 * <p>为药师工作站提供药品库存管理、发药、退药等核心功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品查询</b>：支持关键字搜索、低库存筛选</li>
 *   <li><b>发药管理</b>：根据处方发药，自动扣减库存</li>
 *   <li><b>退药管理</b>：处理退药申请，恢复库存</li>
 *   <li><b>库存监控</b>：查看低库存药品</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要PHARMACIST（药师）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>发药时验证库存是否充足</li>
 *   <li>发药后自动扣减库存</li>
 *   <li>退药时恢复库存</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicineService
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
                .map(VoConverter::toMedicineVO)
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
                .map(VoConverter::toMedicineVO)
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
            
            medicineService.updateStock(id, quantity, reason);
            
            return Result.success("库存更新成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("更新库存参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("更新库存业务错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新库存失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 药品高级查询（分页）
     * <p>
     * 支持多条件组合查询，为药师提供更灵活的药品筛选功能。
     * 相比医生查询，药师可以看到进货价、利润率等管理信息。
     * </p>
     *
     * <p><b>查询参数：</b></p>
     * <ul>
     *   <li><b>keyword</b>: 关键字搜索（支持名称、编码、通用名）</li>
     *   <li><b>category</b>: 药品分类</li>
     *   <li><b>isPrescription</b>: 是否处方药（0=否, 1=是）</li>
     *   <li><b>stockStatus</b>: 库存状态（"LOW"=低库存, "OUT"=缺货）</li>
     *   <li><b>manufacturer</b>: 生产厂家</li>
     *   <li><b>minPrice</b>: 最低零售价</li>
     *   <li><b>maxPrice</b>: 最高零售价</li>
     *   <li><b>page</b>: 页码（从0开始）</li>
     *   <li><b>size</b>: 每页大小（默认20）</li>
     *   <li><b>sort</b>: 排序字段和方向（默认name,asc）</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 查询所有药品（含进货价）
     * GET /api/pharmacist/medicines/search
     *
     * # 价格区间查询（10-50元）
     * GET /api/pharmacist/medicines/search?minPrice=10&maxPrice=50
     *
     * # 查询某某制药的处方药
     * GET /api/pharmacist/medicines/search?manufacturer=某某制药&isPrescription=1
     *
     * # 查询低库存药品
     * GET /api/pharmacist/medicines/search?stockStatus=LOW
     * }</pre>
     *
     * @param keyword        关键字（可选）
     * @param category       药品分类（可选）
     * @param isPrescription 是否处方药（可选）
     * @param stockStatus    库存状态（可选）
     * @param manufacturer   生产厂家（可选）
     * @param minPrice       最低价格（可选）
     * @param maxPrice       最高价格（可选）
     * @param pageable       分页和排序参数
     * @return 药品分页列表（含进货价、利润率等管理信息）
     */
    @GetMapping("/search")
    @Operation(
        summary = "药品高级查询",
        description = """
            支持多条件组合查询的分页接口，药师可以看到进货价、利润率等管理信息。

            **扩展字段（相比医生视图）：**
            - purchasePrice: 进货价
            - minStock/maxStock: 库存阈值
            - profitMargin: 利润率（%）
            - storageCondition: 储存条件
            - approvalNo: 批准文号

            **查询参数：**
            - keyword: 关键字（名称/编码/通用名）
            - category: 药品分类
            - isPrescription: 是否处方药
            - stockStatus: 库存状态（LOW=低库存, OUT=缺货）
            - manufacturer: 生产厂家
            - minPrice/maxPrice: 价格区间
            """
    )
    public Result<Page<PharmacistMedicineVO>> searchMedicinesAdvanced(
        @Parameter(description = "关键字（名称/编码/通用名）", example = "阿司匹林")
        @RequestParam(required = false) String keyword,

        @Parameter(description = "药品分类", example = "抗生素")
        @RequestParam(required = false) String category,

        @Parameter(description = "是否处方药（0=否, 1=是）", example = "1")
        @RequestParam(required = false) Short isPrescription,

        @Parameter(description = "库存状态（LOW=低库存, OUT=缺货）", example = "LOW")
        @RequestParam(required = false) String stockStatus,

        @Parameter(description = "生产厂家", example = "某某制药有限公司")
        @RequestParam(required = false) String manufacturer,

        @Parameter(description = "最低价格（元）", example = "10")
        @RequestParam(required = false) BigDecimal minPrice,

        @Parameter(description = "最高价格（元）", example = "50")
        @RequestParam(required = false) BigDecimal maxPrice,

        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
        @Parameter(description = "分页和排序参数")
        Pageable pageable
    ) {
        try {
            log.info("【药师】高级查询药品 - keyword: {}, category: {}, stockStatus: {}, manufacturer: {}, " +
                     "priceRange: {}-{}", keyword, category, stockStatus, manufacturer, minPrice, maxPrice);

            Page<Medicine> page = medicineService.searchMedicinesForPharmacist(
                keyword, category, isPrescription, stockStatus,
                manufacturer, minPrice, maxPrice, pageable
            );

            Page<PharmacistMedicineVO> voPage = page.map(VoConverter::toPharmacistMedicineVO);

            return Result.success(
                String.format("查询成功，共 %d 条记录", voPage.getTotalElements()),
                voPage
            );
        } catch (Exception e) {
            log.error("高级查询药品失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 低库存预警（分页）
     * <p>
     * 查询库存低于最低库存阈值的药品，支持分页。
     * 相比旧的 /low-stock 接口，这个接口支持分页，更适合数据量大的场景。
     * </p>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 查询低库存药品（默认按库存升序）
     * GET /api/pharmacist/medicines/low-stock-alert
     *
     * # 查询第二页，每页30条
     * GET /api/pharmacist/medicines/low-stock-alert?page=1&size=30
     * }</pre>
     *
     * @param pageable 分页和排序参数（默认按库存升序）
     * @return 低库存药品分页列表
     */
    @GetMapping("/low-stock-alert")
    @Operation(
        summary = "低库存预警（分页）",
        description = """
            查询库存低于最低库存阈值的药品，支持分页。

            **排序说明：**
            默认按库存数量升序排列，库存最少的排在前面，方便药师优先处理。

            **使用场景：**
            - 采购决策参考
            - 库存预警通知
            - 药品补货计划
            """
    )
    public Result<Page<PharmacistMedicineVO>> getLowStockAlert(
        @PageableDefault(size = 20, sort = "stockQuantity", direction = Sort.Direction.ASC)
        @Parameter(description = "分页和排序参数（默认按库存升序）")
        Pageable pageable
    ) {
        try {
            log.info("【药师】查询低库存预警");

            // 使用 stockStatus=LOW 查询低库存药品
            Page<Medicine> page = medicineService.searchMedicinesForPharmacist(
                null, null, null, "LOW", null, null, null, pageable
            );

            Page<PharmacistMedicineVO> voPage = page.map(VoConverter::toPharmacistMedicineVO);

            return Result.success(
                String.format("查询成功，共 %d 条低库存记录", voPage.getTotalElements()),
                voPage
            );
        } catch (Exception e) {
            log.error("查询低库存预警失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 库存统计
     * <p>
     * 提供药品库存的统计数据，用于药师工作站的库存概览和仪表盘展示。
     * </p>
     *
     * <p><b>统计维度：</b></p>
     * <ul>
     *   <li><b>totalMedicines</b>: 药品总数量（包含所有状态的药品）</li>
     *   <li><b>inStockCount</b>: 正常库存药品数量（库存 > 最低库存）</li>
     *   <li><b>lowStockCount</b>: 低库存药品数量（库存 <= 最低库存）</li>
     *   <li><b>outOfStockCount</b>: 缺货药品数量（库存 = 0）</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "message": "查询成功",
     *   "data": {
     *     "totalMedicines": 100,
     *     "inStockCount": 75,
     *     "lowStockCount": 20,
     *     "outOfStockCount": 5
     *   }
     * }
     * }</pre>
     *
     * @return 库存统计数据
     */
    @GetMapping("/inventory-stats")
    @Operation(
        summary = "库存统计",
        description = """
            获取药品库存统计数据，用于仪表盘展示。

            **统计维度：**
            - totalMedicines: 药品总数
            - inStockCount: 正常库存数
            - lowStockCount: 低库存数
            - outOfStockCount: 缺货数

            **使用场景：**
            - 药师工作站仪表盘
            - 库存预警通知
            - 采购决策参考
            - 管理报表
            """
    )
    public Result<InventoryStatsVO> getInventoryStats() {
        try {
            log.info("【药师】查询库存统计");

            InventoryStatsVO stats = medicineService.getInventoryStats();

            log.info("库存统计完成 - 总数: {}, 正常: {}, 低库存: {}, 缺货: {}",
                     stats.getTotalMedicines(), stats.getInStockCount(),
                     stats.getLowStockCount(), stats.getOutOfStockCount());

            return Result.success("查询成功", stats);
        } catch (Exception e) {
            log.error("查询库存统计失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
