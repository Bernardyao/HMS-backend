package com.his.specification;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.his.entity.Medicine;

/**
 * 药品动态查询规格类
 * <p>
 * 使用Spring Data JPA Specification实现动态查询条件组合，
 * 支持多条件灵活组合查询，无需编写复杂的JPQL。
 * </p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>关键字模糊搜索（名称、编码、通用名）</li>
 *   <li>按分类、处方药标识筛选</li>
 *   <li>库存状态筛选（有货/缺货/低库存）</li>
 *   <li>价格区间查询</li>
 *   <li>生产厂家筛选</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 单条件查询
 * Specification<Medicine> spec = MedicineSpecification.hasKeyword("阿莫西林");
 * List<Medicine> results = repository.findAll(spec);
 *
 * // 多条件组合查询
 * Specification<Medicine> spec = Specification
 *     .where(MedicineSpecification.isActive())
 *     .and(MedicineSpecification.hasKeyword("阿司匹林"))
 *     .and(MedicineSpecification.isPrescription((short) 1))
 *     .and(MedicineSpecification.hasStock(true));
 * Page<Medicine> page = repository.findAll(spec, pageable);
 * }</pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see Medicine
 * @see Specification
 */
public class MedicineSpecification {

    private MedicineSpecification() {
        // 工具类，禁止实例化
    }

    /**
     * 基础条件：未删除且启用的药品
     * <p>
     * 所有查询都应该包含此条件，确保只查询有效的药品数据。
     * </p>
     *
     * @return 未删除且启用条件
     */
    public static Specification<Medicine> isActive() {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("isDeleted"), 0),
            cb.equal(root.get("status"), 1)
        );
    }

    /**
     * 关键字模糊搜索
     * <p>
     * 支持在以下字段中模糊匹配：
     * <ul>
     *   <li>药品名称（name）</li>
     *   <li>药品编码（medicineCode）</li>
     *   <li>通用名称（genericName）</li>
     * </ul>
     * 搜索不区分大小写，使用LIKE进行模糊匹配。
     * </p>
     *
     * @param keyword 关键字，如果为null或空字符串则不添加此条件
     * @return 关键字搜索条件
     */
    public static Specification<Medicine> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("medicineCode")), pattern),
                cb.like(cb.lower(root.get("genericName")), pattern)
            );
        };
    }

    /**
     * 按药品分类筛选
     * <p>
     * 精确匹配药品分类字段。
     * </p>
     *
     * @param category 药品分类，如果为null则不添加此条件
     * @return 分类筛选条件
     */
    public static Specification<Medicine> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category"), category);
        };
    }

    /**
     * 按是否处方药筛选
     * <p>
     * <ul>
     *   <li>0 = 非处方药</li>
     *   <li>1 = 处方药</li>
     * </ul>
     * </p>
     *
     * @param isPrescription 是否处方药，如果为null则不添加此条件
     * @return 处方药筛选条件
     */
    public static Specification<Medicine> isPrescription(Short isPrescription) {
        return (root, query, cb) -> {
            if (isPrescription == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrescription"), isPrescription);
        };
    }

    /**
     * 按库存状态筛选
     * <p>
     * <ul>
     *   <li>inStock = true: 只显示有货的药品（stockQuantity > 0）</li>
     *   <li>inStock = false: 只显示缺货的药品（stockQuantity = 0）</li>
     *   <li>inStock = null: 不过滤库存状态</li>
     * </ul>
     * </p>
     *
     * @param inStock 是否只显示有货药品，null表示不过滤
     * @return 库存状态筛选条件
     */
    public static Specification<Medicine> hasStock(Boolean inStock) {
        return (root, query, cb) -> {
            if (inStock == null) {
                return cb.conjunction();
            }
            if (inStock) {
                return cb.greaterThan(root.get("stockQuantity"), 0);
            } else {
                return cb.equal(root.get("stockQuantity"), 0);
            }
        };
    }

    /**
     * 筛选低库存药品
     * <p>
     * 库存数量 <= 最低库存阈值的药品。
     * </p>
     *
     * @return 低库存条件
     */
    public static Specification<Medicine> isLowStock() {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
            root.get("stockQuantity"),
            root.get("minStock")
        );
    }

    /**
     * 筛选缺货药品
     * <p>
     * 库存数量 = 0 的药品。
     * </p>
     *
     * @return 缺货条件
     */
    public static Specification<Medicine> isOutOfStock() {
        return (root, query, cb) -> cb.equal(root.get("stockQuantity"), 0);
    }

    /**
     * 按价格区间筛选
     * <p>
     * <ul>
     *   <li>只指定minPrice: 零售价 >= minPrice</li>
     *   <li>只指定maxPrice: 零售价 <= maxPrice</li>
     *   <li>同时指定: minPrice <= 零售价 <= maxPrice</li>
     *   <li>都不指定: 不过滤价格</li>
     * </ul>
     * </p>
     *
     * @param minPrice 最低价格（包含），可以为null
     * @param maxPrice 最高价格（包含），可以为null
     * @return 价格区间筛选条件
     */
    public static Specification<Medicine> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction();
            }
            if (minPrice == null) {
                return cb.lessThanOrEqualTo(root.get("retailPrice"), maxPrice);
            }
            if (maxPrice == null) {
                return cb.greaterThanOrEqualTo(root.get("retailPrice"), minPrice);
            }
            return cb.between(root.get("retailPrice"), minPrice, maxPrice);
        };
    }

    /**
     * 按生产厂家筛选
     * <p>
     * 精确匹配生产厂家字段。
     * </p>
     *
     * @param manufacturer 生产厂家，如果为null则不添加此条件
     * @return 生产厂家筛选条件
     */
    public static Specification<Medicine> hasManufacturer(String manufacturer) {
        return (root, query, cb) -> {
            if (manufacturer == null || manufacturer.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("manufacturer"), manufacturer);
        };
    }

    /**
     * 构建综合查询条件（医生工作站）
     * <p>
     * 组合医生工作站常用的查询条件：
     * <ul>
     *   <li>基础条件：未删除且启用</li>
     *   <li>关键字搜索（名称、编码、通用名）</li>
     *   <li>分类筛选</li>
     *   <li>处方药筛选</li>
     *   <li>库存状态筛选</li>
     * </ul>
     * </p>
     *
     * @param keyword        关键字
     * @param category       药品分类
     * @param isPrescription 是否处方药
     * @param inStock        是否只显示有货药品
     * @return 组合查询条件
     */
    public static Specification<Medicine> buildDoctorQuery(
        String keyword,
        String category,
        Short isPrescription,
        Boolean inStock
    ) {
        return Specification
            .where(isActive())
            .and(hasKeyword(keyword))
            .and(hasCategory(category))
            .and(isPrescription(isPrescription))
            .and(hasStock(inStock));
    }

    /**
     * 构建综合查询条件（药师工作站）
     * <p>
     * 组合药师工作站的高级查询条件：
     * <ul>
     *   <li>基础条件：未删除且启用</li>
     *   <li>关键字搜索（名称、编码、通用名）</li>
     *   <li>分类筛选</li>
     *   <li>处方药筛选</li>
     *   <li>库存状态筛选（LOW/OUT）</li>
     *   <li>生产厂家筛选</li>
     *   <li>价格区间筛选</li>
     * </ul>
     * </p>
     *
     * @param keyword        关键字
     * @param category       药品分类
     * @param isPrescription 是否处方药
     * @param stockStatus    库存状态（"LOW"=低库存, "OUT"=缺货, 其他=不过滤）
     * @param manufacturer   生产厂家
     * @param minPrice       最低价格
     * @param maxPrice       最高价格
     * @return 组合查询条件
     */
    public static Specification<Medicine> buildPharmacistQuery(
        String keyword,
        String category,
        Short isPrescription,
        String stockStatus,
        String manufacturer,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        Specification<Medicine> spec = Specification
            .where(isActive())
            .and(hasKeyword(keyword))
            .and(hasCategory(category))
            .and(isPrescription(isPrescription))
            .and(hasManufacturer(manufacturer))
            .and(priceBetween(minPrice, maxPrice));

        // 根据stockStatus参数添加额外的库存条件
        if ("LOW".equalsIgnoreCase(stockStatus)) {
            spec = spec.and(isLowStock());
        } else if ("OUT".equalsIgnoreCase(stockStatus)) {
            spec = spec.and(isOutOfStock());
        }

        return spec;
    }
}
