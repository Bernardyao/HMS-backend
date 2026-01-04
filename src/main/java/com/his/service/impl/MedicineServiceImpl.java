package com.his.service.impl;

import com.his.dto.InventoryStatsVO;
import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.service.MedicineService;
import com.his.specification.MedicineSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 药品服务实现类
 *
 * <p>负责药品信息的管理，包括药品查询、库存管理等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>药品查询：支持模糊搜索、按ID查询、查询所有启用药品</li>
 *   <li>库存检查：检查指定数量的库存是否充足</li>
 *   <li>库存管理：支持库存增加和扣减操作</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>仅查询未删除的药品（isDeleted=0）</li>
 *   <li>模糊搜索支持药品名称和编码（medicineCode）</li>
 *   <li>库存变动数量不能为0，且扣减后库存不能为负</li>
 *   <li>正数表示增加库存，负数表示扣减库存</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.Medicine} - 药品信息</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicineService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    /**
     * 模糊搜索药品
     *
     * <p>根据关键字模糊查询药品名称或编码</p>
     *
     * <p><b>搜索规则：</b></p>
     * <ul>
     *   <li>支持药品名称（name）模糊匹配</li>
     *   <li>支持药品编码（medicineCode）模糊匹配</li>
     *   <li>仅返回未删除的药品</li>
     *   <li>关键字为空时返回所有启用的药品</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>医生开具处方时搜索药品</li>
     *   <li>药品库存查询</li>
     * </ul>
     *
     * @param keyword 搜索关键字（药品名称或编码），为空则返回所有启用的药品
     * @return 匹配的药品列表
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String keyword) {
        log.info("搜索药品，关键字: {}", keyword);
        
        if (!StringUtils.hasText(keyword)) {
            log.warn("搜索关键字为空，返回所有启用的药品");
            return getAllActive();
        }

        // 使用 @Query 自定义查询，支持名称或编码模糊匹配
        List<Medicine> medicines = medicineRepository.searchByKeyword(keyword, (short) 0);
        log.info("搜索到 {} 个药品", medicines.size());
        
        return medicines;
    }

    /**
     * 根据ID查询药品
     *
     * <p>查询指定ID的药品详细信息</p>
     *
     * @param id 药品ID
     * @return 药品实体
     * @throws IllegalArgumentException 如果药品ID为空
     * @throws IllegalArgumentException 如果药品不存在
     * @throws IllegalArgumentException 如果药品已删除
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public Medicine getById(Long id) {
        log.info("查询药品，ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("药品ID不能为空");
        }
        
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + id));
        
        if (medicine.getIsDeleted() == 1) {
            throw new IllegalArgumentException("药品已被删除，ID: " + id);
        }
        
        return medicine;
    }

    /**
     * 查询所有启用的药品
     *
     * <p>查询所有状态为启用且未删除的药品</p>
     *
     * <p><b>查询条件：</b></p>
     * <ul>
     *   <li>药品未删除（isDeleted=0）</li>
     *   <li>药品状态为启用（status=1）</li>
     * </ul>
     *
     * @return 启用的药品列表
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getAllActive() {
        log.info("查询所有启用的药品");
        return medicineRepository.findAllActive();
    }

    /**
     * 检查库存是否充足
     *
     * <p>检查指定药品的当前库存是否满足需求量</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>比较当前库存与需求量</li>
     *   <li>当前库存 >= 需求量时返回true</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>发药前检查库存</li>
     *   <li>处方审核时检查库存</li>
     * </ul>
     *
     * @param medicineId 药品ID
     * @param quantity 需要的数量（必须大于0）
     * @return true表示库存充足，false表示库存不足
     * @throws IllegalArgumentException 如果参数为空或数量不大于0
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkStock(Long medicineId, Integer quantity) {
        log.info("检查库存，药品ID: {}, 需要数量: {}", medicineId, quantity);
        
        if (medicineId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数错误");
        }
        
        Medicine medicine = getById(medicineId);
        boolean sufficient = medicine.getStockQuantity() >= quantity;
        
        log.info("库存检查结果: 当前库存={}, 需要数量={}, 是否充足={}", 
                medicine.getStockQuantity(), quantity, sufficient);
        
        return sufficient;
    }

    /**
     * 更新药品库存
     *
     * <p>增加或扣减药品库存，支持库存变动记录</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>正数：增加库存（如：采购入库、退药恢复）</li>
     *   <li>负数：扣减库存（如：发药、报损）</li>
     *   <li>扣减后库存不能为负数，否则抛出异常</li>
     *   <li>变动数量不能为0</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>发药时扣减库存</li>
     *   <li>退药时恢复库存</li>
     *   <li>采购时增加库存</li>
     * </ul>
     *
     * @param medicineId 药品ID
     * @param quantity 变动数量（正数=增加，负数=扣减，不能为0）
     * @param reason 变动原因（如：发药、退药、采购等）
     * @throws IllegalArgumentException 如果药品ID为空
     * @throws IllegalArgumentException 如果变动数量为空或为0
     * @throws IllegalStateException 如果扣减后库存为负
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long medicineId, Integer quantity, String reason) {
        log.info("更新药品库存，药品ID: {}, 变动数量: {}, 原因: {}", medicineId, quantity, reason);

        if (medicineId == null) {
            throw new IllegalArgumentException("药品ID不能为空");
        }
        if (quantity == null || quantity == 0) {
            throw new IllegalArgumentException("变动数量不能为空且不能为0");
        }

        // 使用 getById 查询药品，已包含判空和逻辑删除检查
        Medicine medicine = getById(medicineId);

        long newStock = (long) medicine.getStockQuantity() + quantity;

        if (newStock < 0) {
            throw new IllegalStateException("库存不足，当前库存: " + medicine.getStockQuantity() + ", 尝试扣减: " + Math.abs(quantity));
        }

        // 更新库存
        medicine.setStockQuantity((int) newStock);
        medicine.setUpdatedAt(java.time.LocalDateTime.now());
        
        // 实际保存
        medicineRepository.save(medicine);

        log.info("库存更新成功：药品ID={}, 原库存={}, 新库存={}, 原因={}",
                medicine.getMainId(), newStock - quantity, newStock, reason);
    }

    /**
     * 医生工作站 - 综合查询药品（分页）
     * <p>
     * 使用Specification动态查询，支持多条件组合。
     * </p>
     *
     * @param keyword        关键字（名称/编码/通用名）
     * @param category       药品分类
     * @param isPrescription 是否处方药
     * @param inStock        是否只显示有货药品
     * @param pageable       分页和排序参数
     * @return 药品分页数据
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Medicine> searchMedicinesForDoctor(
        String keyword,
        String category,
        Short isPrescription,
        Boolean inStock,
        Pageable pageable
    ) {
        log.info("医生查询药品 - keyword: {}, category: {}, isPrescription: {}, inStock: {}, pageable: {}",
                 keyword, category, isPrescription, inStock, pageable);

        // 使用Specification构建动态查询条件
        var spec = MedicineSpecification.buildDoctorQuery(
            keyword, category, isPrescription, inStock
        );

        Page<Medicine> result = medicineRepository.findAll(spec, pageable);

        log.info("查询成功，共 {} 条记录", result.getTotalElements());
        return result;
    }

    /**
     * 药师工作站 - 高级查询药品（分页）
     * <p>
     * 支持更高级的查询条件，包括库存状态、生产厂家、价格区间等。
     * </p>
     *
     * @param keyword        关键字（名称/编码/通用名）
     * @param category       药品分类
     * @param isPrescription 是否处方药
     * @param stockStatus    库存状态（"LOW"=低库存, "OUT"=缺货）
     * @param manufacturer   生产厂家
     * @param minPrice       最低价格
     * @param maxPrice       最高价格
     * @param pageable       分页和排序参数
     * @return 药品分页数据
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Medicine> searchMedicinesForPharmacist(
        String keyword,
        String category,
        Short isPrescription,
        String stockStatus,
        String manufacturer,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    ) {
        log.info("药师查询药品 - keyword: {}, category: {}, stockStatus: {}, manufacturer: {}, " +
                 "priceRange: {}-{}, pageable: {}",
                 keyword, category, stockStatus, manufacturer, minPrice, maxPrice, pageable);

        // 使用Specification构建高级查询条件
        var spec = MedicineSpecification.buildPharmacistQuery(
            keyword, category, isPrescription, stockStatus,
            manufacturer, minPrice, maxPrice
        );

        Page<Medicine> result = medicineRepository.findAll(spec, pageable);

        log.info("查询成功，共 {} 条记录", result.getTotalElements());
        return result;
    }

    /**
     * 获取库存统计数据
     * <p>
     * 统计所有药品的库存情况，包括总数量、正常库存、低库存、缺货数量。
     * </p>
     *
     * @return 库存统计数据
     */
    @Override
    @Transactional(readOnly = true)
    public InventoryStatsVO getInventoryStats() {
        log.info("查询库存统计");

        // 查询所有启用的药品
        List<Medicine> allMedicines = getAllActive();

        long total = allMedicines.size();
        long inStock = allMedicines.stream()
            .filter(m -> m.getStockQuantity() != null &&
                       m.getStockQuantity() > 0 &&
                       (m.getMinStock() == null || m.getStockQuantity() > m.getMinStock()))
            .count();
        long lowStock = allMedicines.stream()
            .filter(m -> m.getStockQuantity() != null &&
                       m.getStockQuantity() > 0 &&
                       m.getMinStock() != null &&
                       m.getStockQuantity() <= m.getMinStock())
            .count();
        long outOfStock = allMedicines.stream()
            .filter(m -> m.getStockQuantity() != null &&
                       m.getStockQuantity() == 0)
            .count();

        InventoryStatsVO stats = InventoryStatsVO.builder()
            .totalMedicines(total)
            .inStockCount(inStock)
            .lowStockCount(lowStock)
            .outOfStockCount(outOfStock)
            .build();

        log.info("库存统计完成 - 总数: {}, 正常: {}, 低库存: {}, 缺货: {}",
                 total, inStock, lowStock, outOfStock);

        return stats;
    }
}
