package com.his.controller;

import com.his.common.Result;
import com.his.dto.InventoryStatsVO;
import com.his.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 药师工作站-库存操作控制器
 *
 * <p>为药师工作站提供药品库存管理的操作接口（不含查询）</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>库存更新</b>：手动调整药品库存（入库/出库）</li>
 *   <li><b>库存统计</b>：获取库存统计数据，用于仪表盘展示</li>
 * </ul>
 *
 * <h3>重要说明</h3>
 * <p>本控制器只包含操作类接口，所有查询接口已迁移至 {@link com.his.controller.CommonMedicineController}</p>
 * <p>查询药品请使用：<code>GET /api/common/medicines</code></p>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要PHARMACIST（药师）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>更新库存时记录操作原因，用于审计</li>
 *   <li>库存统计数据用于库存预警和采购决策</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 * @see com.his.service.MedicineService
 * @see com.his.controller.CommonMedicineController
 */
@Tag(name = "药师工作站-库存操作", description = "药师工作站的库存管理操作接口（查询功能已迁移至/common）")
@Slf4j
@RestController
@RequestMapping(value = "/api/pharmacist/medicines", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
public class PharmacistMedicineController {

    private final MedicineService medicineService;

    /**
     * 更新药品库存
     * <p>
     * 手动调整药品库存数量，用于入库、出库、盘点等操作。
     * </p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>quantity为正数表示入库（增加库存）</li>
     *   <li>quantity为负数表示出库（减少库存）</li>
     *   <li>必须提供操作原因，用于审计跟踪</li>
     *   <li>出库时验证库存是否充足</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 入库100盒
     * PUT /api/pharmacist/medicines/1/stock?quantity=100&reason=采购入库
     *
     * # 出库5盒
     * PUT /api/pharmacist/medicines/1/stock?quantity=-5&reason=发药消耗
     * }</pre>
     *
     * @param id 药品ID
     * @param quantity 更新数量（正数=入库，负数=出库）
     * @param reason 操作原因
     * @return 操作结果
     */
    @Operation(
        summary = "更新药品库存",
        description = """
            手动调整药品库存数量（入库/出库）。

            **业务规则：**
            - quantity为正数：入库（增加库存）
            - quantity为负数：出库（减少库存）
            - 必须提供操作原因用于审计
            - 出库时验证库存是否充足

            **使用场景：**
            - 采购入库
            - 盘点调整
            - 损耗报损
            - 发药退药
            """
    )
    @PutMapping("/{id}/stock")
    public Result<String> updateStock(
        @Parameter(description = "药品ID", required = true, example = "1")
        @PathVariable Long id,

        @Parameter(description = "更新数量（正数=入库，负数=出库）", required = true, example = "100")
        @RequestParam Integer quantity,

        @Parameter(description = "操作原因", required = true, example = "采购入库")
        @RequestParam String reason) {

        log.info("【药师】更新药品库存 - 药品ID: {}, 数量: {}, 原因: {}", id, quantity, reason);

        medicineService.updateStock(id, quantity, reason);

        return Result.success("库存更新成功", null);
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
    @GetMapping("/inventory-stats")
    public Result<InventoryStatsVO> getInventoryStats() {
        log.info("【药师】查询库存统计");

        InventoryStatsVO stats = medicineService.getInventoryStats();

        log.info("库存统计完成 - 总数: {}, 正常: {}, 低库存: {}, 缺货: {}",
                 stats.getTotalMedicines(), stats.getInStockCount(),
                 stats.getLowStockCount(), stats.getOutOfStockCount());

        return Result.success("查询成功", stats);
    }
}
