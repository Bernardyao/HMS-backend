package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存统计视图对象
 * <p>
 * 提供药品库存的统计数据，用于药师工作站的库存概览和预警提示。
 * </p>
 *
 * <h3>统计维度</h3>
 * <ul>
 *   <li><b>totalMedicines</b>: 药品总数量（包含所有状态的药品）</li>
 *   <li><b>inStockCount</b>: 正常库存药品数量（库存 > 最低库存）</li>
 *   <li><b>lowStockCount</b>: 低库存药品数量（库存 <= 最低库存）</li>
 *   <li><b>outOfStockCount</b>: 缺货药品数量（库存 = 0）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>药师工作站仪表盘</li>
 *   <li>库存预警通知</li>
 *   <li>采购决策参考</li>
 *   <li>管理报表</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "库存统计视图对象")
public class InventoryStatsVO {

    @Schema(description = "药品总数量", example = "100")
    private Long totalMedicines;

    @Schema(description = "正常库存药品数量", example = "75")
    private Long inStockCount;

    @Schema(description = "低库存药品数量", example = "20")
    private Long lowStockCount;

    @Schema(description = "缺货药品数量", example = "5")
    private Long outOfStockCount;

    /**
     * 计算正常库存占比
     * <p>
     * 正常库存占比 = (正常库存数量 / 总数量) × 100%
     * </p>
     *
     * @return 正常库存占比（百分比），如果总数量为0则返回0
     */
    @Schema(description = "正常库存占比（%）", example = "75.0")
    public double getInStockRate() {
        if (totalMedicines == null || totalMedicines == 0) {
            return 0.0;
        }
        return (inStockCount * 100.0) / totalMedicines;
    }

    /**
     * 计算低库存占比
     * <p>
     * 低库存占比 = (低库存数量 / 总数量) × 100%
     * </p>
     *
     * @return 低库存占比（百分比），如果总数量为0则返回0
     */
    @Schema(description = "低库存占比（%）", example = "20.0")
    public double getLowStockRate() {
        if (totalMedicines == null || totalMedicines == 0) {
            return 0.0;
        }
        return (lowStockCount * 100.0) / totalMedicines;
    }

    /**
     * 计算缺货占比
     * <p>
     * 缺货占比 = (缺货数量 / 总数量) × 100%
     * </p>
     *
     * @return 缺货占比（百分比），如果总数量为0则返回0
     */
    @Schema(description = "缺货占比（%）", example = "5.0")
    public double getOutOfStockRate() {
        if (totalMedicines == null || totalMedicines == 0) {
            return 0.0;
        }
        return (outOfStockCount * 100.0) / totalMedicines;
    }
}
