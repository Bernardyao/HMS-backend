package com.his.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.his.dto.InventoryStatsVO;
import com.his.entity.Medicine;

/**
 * 药品服务接口
 *
 * <p>提供药品查询、库存管理等核心业务功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品查询</b>：支持模糊搜索、分页查询、分类查询</li>
 *   <li><b>库存管理</b>：库存检查、库存更新、库存统计</li>
 *   <li><b>角色视图</b>：为医生和药师提供不同的查询接口</li>
 * </ul>
 *
 * <h3>角色视图差异</h3>
 * <ul>
 *   <li><b>医生视图</b>：只能看到零售价，看不到进货价和利润率</li>
 *   <li><b>药师视图</b>：可以看到完整的药品信息（含进货价、利润率）</li>
 *   <li><b>库存状态</b>：医生关注是否有货，药师关注具体库存数量</li>
 * </ul>
 *
 * <h3>库存状态说明</h3>
 * <ul>
 *   <li><b>IN_STOCK</b>：正常库存（库存 > 最低库存）</li>
 *   <li><b>LOW_STOCK</b>：低库存（库存 <= 最低库存，但 > 0）</li>
 *   <li><b>OUT_OF_STOCK</b>：缺货（库存 = 0）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.entity.Medicine
 * @see com.his.repository.MedicineRepository
 */
public interface MedicineService {

    /**
     * 模糊搜索药品（根据名称或编码）
     *
     * <p>支持按药品名称、编码、通用名进行模糊搜索</p>
     * <p>不区分大小写，返回所有启用的药品</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>快速搜索：医生开处方时快速查找药品</li>
     *   <li>自动补全：前端输入框的自动补全功能</li>
     * </ul>
     *
     * @param keyword 关键字（药品名称、编码或通用名）
     * @return 药品列表（按名称排序）
     * @since 1.0
     */
    List<Medicine> searchMedicines(String keyword);

    /**
     * 根据ID查询药品
     *
     * <p>根据药品主键ID查询详细信息</p>
     * <p>药品不存在时抛出IllegalArgumentException</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>详情查看：查看药品完整信息</li>
     *   <li>库存查询：查看当前库存状态</li>
     *   <li>价格查询：查看零售价和进货价（药师视图）</li>
     * </ul>
     *
     * @param id 药品ID（主键）
     * @return 药品信息
     * @throws IllegalArgumentException 如果药品不存在
     * @since 1.0
     */
    Medicine getById(Long id);

    /**
     * 查询所有启用的药品
     *
     * <p>返回所有状态为启用(status=1)且未删除(isDeleted=0)的药品</p>
     * <p>用于下拉框选择、列表展示等场景</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>下拉框填充：药品选择下拉框的数据源</li>
     *   <li>列表展示：展示所有可用药品</li>
     * </ul>
     *
     * @return 药品列表（按名称排序）
     * @since 1.0
     */
    List<Medicine> getAllActive();

    /**
     * 检查库存是否充足
     *
     * <p>判断指定药品的库存是否满足需求量</p>
     * <p>库存充足：stockQuantity >= quantity</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>开方验证：医生开处方前验证库存</li>
     *   <li>发药验证：药师发药前验证库存</li>
     *   <li>库存预警：库存不足时提示</li>
     * </ul>
     *
     * @param medicineId 药品ID
     * @param quantity 需要的数量
     * @return true=库存充足, false=库存不足
     * @since 1.0
     */
    boolean checkStock(Long medicineId, Integer quantity);

    /**
     * 更新库存
     *
     * <p>增加或减少药品库存数量</p>
     * <p>自动记录库存变动日志和审计日志</p>
     *
     * <p><b>库存变动规则：</b></p>
     * <ul>
     *   <li><b>正数</b>：入库（采购入库、退货入库等）</li>
     *   <li><b>负数</b>：出库（发药、报损、盘亏等）</li>
     *   <li><b>零</b>：无效操作，不做处理</li>
     * </ul>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>库存不能为负数</li>
     *   <li>库存低于最低库存时记录日志</li>
     *   <li>库存为0时标记为缺货状态</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>发药扣减：药师发药时扣减库存</li>
     *   <li>采购入库：采购药品时增加库存</li>
     *   <li>退药入库：患者退药时恢复库存</li>
     * </ul>
     *
     * @param medicineId 药品ID
     * @param quantity 变动数量（正数=入库，负数=出库/消耗）
     * @param reason 变动原因（如："发药"、"采购入库"、"患者退药"）
     * @throws IllegalArgumentException 如果药品不存在或库存不足
     * @since 1.0
     */
    void updateStock(Long medicineId, Integer quantity, String reason);

    /**
     * 医生工作站 - 综合查询药品（分页）
     *
     * <p>为医生提供药品查询功能，用于开处方时选择药品</p>
     * <p>支持多条件组合查询，返回医生视图的数据（不含进货价）</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li><b>keyword</b>：关键字搜索（名称、编码、通用名）</li>
     *   <li><b>category</b>：药品分类筛选</li>
     *   <li><b>isPrescription</b>：处方药筛选（0=非处方药, 1=处方药）</li>
     *   <li><b>inStock</b>：库存状态筛选（true=只显示有货, false=显示全部）</li>
     * </ul>
     *
     * <p><b>医生视图特点：</b></p>
     * <ul>
     *   <li>只显示零售价，不显示进货价和利润率</li>
     *   <li>显示库存状态（IN_STOCK/LOW_STOCK/OUT_OF_STOCK）</li>
     *   <li>便于医生快速找到有货的药品开处方</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 查询所有抗生素类处方药
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));
     * Page&lt;Medicine&gt; page = medicineService.searchMedicinesForDoctor(
     *     null, "抗生素", (short) 1, true, pageable
     * );
     *
     * // 关键字搜索"阿莫西林"
     * Page&lt;Medicine&gt; page = medicineService.searchMedicinesForDoctor(
     *     "阿莫西林", null, null, null, pageable
     * );
     * </pre>
     *
     * @param keyword 关键字（名称/编码/通用名）
     * @param category 药品分类（如：抗生素、解热镇痛药）
     * @param isPrescription 是否处方药（0=否, 1=是, null=全部）
     * @param inStock 是否只显示有货药品（true=是, false=否, null=全部）
     * @param pageable 分页和排序参数
     * @return 药品分页数据（医生视图）
     * @since 1.0
     */
    Page<Medicine> searchMedicinesForDoctor(
        String keyword,
        String category,
        Short isPrescription,
        Boolean inStock,
        Pageable pageable
    );

    /**
     * 药师工作站 - 高级查询药品（分页）
     *
     * <p>为药师提供高级查询功能，用于药品管理和库存监控</p>
     * <p>支持更丰富的查询条件和更详细的数据</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li><b>keyword</b>：关键字搜索（名称、编码、通用名）</li>
     *   <li><b>category</b>：药品分类筛选</li>
     *   <li><b>isPrescription</b>：处方药筛选（0=非处方药, 1=处方药）</li>
     *   <li><b>stockStatus</b>：库存状态筛选（"LOW"=低库存, "OUT"=缺货, 其他=不过滤）</li>
     *   <li><b>manufacturer</b>：生产厂家筛选</li>
     *   <li><b>minPrice/maxPrice</b>：价格区间筛选</li>
     * </ul>
     *
     * <p><b>药师视图特点：</b></p>
     * <ul>
     *   <li>显示完整的药品信息（含进货价、利润率）</li>
     *   <li>显示具体的库存数量（而非简单的状态）</li>
     *   <li>支持按价格区间查询</li>
     *   <li>支持按生产厂家筛选</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 查询所有缺货药品
     * Pageable pageable = PageRequest.of(0, 50, Sort.by("stockQuantity"));
     * Page&lt;Medicine&gt; page = medicineService.searchMedicinesForPharmacist(
     *     null, null, null, "OUT", null, null, null, pageable
     * );
     *
     * // 查询价格在10-50元之间的抗生素
     * Page&lt;Medicine&gt; page = medicineService.searchMedicinesForPharmacist(
     *     null, "抗生素", null, null, null,
     *     new BigDecimal("10"), new BigDecimal("50"), pageable
     * );
     * </pre>
     *
     * @param keyword 关键字（名称/编码/通用名）
     * @param category 药品分类
     * @param isPrescription 是否处方药（0=否, 1=是）
     * @param stockStatus 库存状态（"LOW"=低库存, "OUT"=缺货, 其他=不过滤）
     * @param manufacturer 生产厂家
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页和排序参数
     * @return 药品分页数据（药师视图，含完整信息）
     * @since 1.0
     */
    Page<Medicine> searchMedicinesForPharmacist(
        String keyword,
        String category,
        Short isPrescription,
        String stockStatus,
        String manufacturer,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * 获取库存统计数据
     *
     * <p>统计当前库存的整体状况，用于库存监控和预警</p>
     * <p>帮助药师了解库存分布，及时补货</p>
     *
     * <p><b>统计维度：</b></p>
     * <ul>
     *   <li><b>total</b>：药品总数量</li>
     *   <li><b>inStock</b>：正常库存数量（库存 > 最低库存）</li>
     *   <li><b>lowStock</b>：低库存数量（库存 <= 最低库存，但 > 0）</li>
     *   <li><b>outOfStock</b>：缺货数量（库存 = 0）</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>库存仪表盘：在药师工作台首页展示库存概况</li>
     *   <li>库存预警：低库存和缺货药品需要及时补货</li>
     *   <li>采购计划：根据统计数据制定采购计划</li>
     * </ul>
     *
     * @return 库存统计数据
     * @since 1.0
     */
    InventoryStatsVO getInventoryStats();
}
