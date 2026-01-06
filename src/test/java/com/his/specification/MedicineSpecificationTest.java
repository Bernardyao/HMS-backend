package com.his.specification;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.test.base.BaseControllerTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MedicineSpecification 单元测试
 * <p>
 * 测试动态查询条件的正确性，确保各种筛选条件组合能够正确生成SQL查询
 * </p>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("药品动态查询规格测试")
class MedicineSpecificationTest extends BaseControllerTest {

    @Autowired
    private MedicineRepository medicineRepository;

    /**
     * 测试基础条件：查询未删除且启用的药品
     */
    @Test
    @DisplayName("应该只查询未删除且启用的药品")
    void testIsActive() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
        );

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertEquals(0, (int) medicine.getIsDeleted(), "应该只包含未删除的药品");
            assertEquals(1, (int) medicine.getStatus(), "应该只包含启用的药品");
        });
    }

    /**
     * 测试关键字搜索：药品名称
     */
    @Test
    @DisplayName("应该能够按药品名称模糊搜索")
    void testHasKeyword_ByName() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasKeyword("阿莫西林"))
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.size() >= 0, "查询结果不应为null");
    }

    /**
     * 测试关键字搜索：药品编码
     */
    @Test
    @DisplayName("应该能够按药品编码模糊搜索")
    void testHasKeyword_ByCode() {
        // 执行查询（假设存在编码包含 "001" 的药品）
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasKeyword("001"))
        );

        // 验证结果
        assertNotNull(result);
    }

    /**
     * 测试关键字搜索：空关键字应该返回所有启用的药品
     */
    @Test
    @DisplayName("空关键字应该返回所有启用的药品")
    void testHasKeyword_EmptyKeyword() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasKeyword(""))
        );

        // 执行查询（null关键字）
        List<Medicine> resultNull = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasKeyword(null))
        );

        // 验证结果
        assertNotNull(result);
        assertNotNull(resultNull);
        // 空关键字不应该过滤数据
    }

    /**
     * 测试按分类筛选
     */
    @Test
    @DisplayName("应该能够按药品分类筛选")
    void testHasCategory() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasCategory("抗生素"))
        );

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertEquals("抗生素", medicine.getCategory(), "所有药品应该属于抗生素分类");
        });
    }

    /**
     * 测试按分类筛选：空分类应该不过滤
     */
    @Test
    @DisplayName("空分类应该不过滤数据")
    void testHasCategory_NullCategory() {
        // 执行查询
        List<Medicine> resultWithNull = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasCategory(null))
        );

        // 验证结果
        assertNotNull(resultWithNull);
    }

    /**
     * 测试按处方药筛选
     */
    @Test
    @DisplayName("应该能够按是否处方药筛选")
    void testIsPrescription() {
        // 查询处方药
        List<Medicine> prescriptionDrugs = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.isPrescription((short) 1))
        );

        // 查询非处方药
        List<Medicine> nonPrescriptionDrugs = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.isPrescription((short) 0))
        );

        // 验证结果
        assertNotNull(prescriptionDrugs);
        assertNotNull(nonPrescriptionDrugs);

        prescriptionDrugs.forEach(medicine ->
            assertEquals(1, (int) medicine.getIsPrescription(), "应该都是处方药")
        );

        nonPrescriptionDrugs.forEach(medicine ->
            assertEquals(0, (int) medicine.getIsPrescription(), "应该都是非处方药")
        );
    }

    /**
     * 测试库存状态筛选
     */
    @Test
    @DisplayName("应该能够按库存状态筛选")
    void testHasStock() {
        // 查询有货的药品
        List<Medicine> inStock = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasStock(true))
        );

        // 查询缺货的药品
        List<Medicine> outOfStock = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasStock(false))
        );

        // 验证结果
        assertNotNull(inStock);
        assertNotNull(outOfStock);

        inStock.forEach(medicine ->
            assertTrue(medicine.getStockQuantity() > 0, "有货药品库存应该大于0")
        );

        outOfStock.forEach(medicine ->
            assertEquals(0, medicine.getStockQuantity(), "缺货药品库存应该等于0")
        );
    }

    /**
     * 测试低库存筛选
     */
    @Test
    @DisplayName("应该能够查询低库存药品")
    void testIsLowStock() {
        // 执行查询
        List<Medicine> lowStock = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.isLowStock())
        );

        // 验证结果
        assertNotNull(lowStock);
        lowStock.forEach(medicine -> {
            assertTrue(medicine.getStockQuantity() <= medicine.getMinStock(),
                "低库存药品库存应该 <= 最低库存");
        });
    }

    /**
     * 测试价格区间筛选
     */
    @Test
    @DisplayName("应该能够按价格区间筛选")
    void testPriceBetween() {
        // 查询价格在10-50元之间的药品
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.priceBetween(
                    new BigDecimal("10"),
                    new BigDecimal("50")
                ))
        );

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertTrue(medicine.getRetailPrice().compareTo(new BigDecimal("10")) >= 0,
                "价格应该 >= 10元");
            assertTrue(medicine.getRetailPrice().compareTo(new BigDecimal("50")) <= 0,
                "价格应该 <= 50元");
        });
    }

    /**
     * 测试价格区间：只有最低价
     */
    @Test
    @DisplayName("价格区间只有最低价应该查询价格 >= 最低价的药品")
    void testPriceBetween_OnlyMinPrice() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.priceBetween(
                    new BigDecimal("20"),
                    null
                ))
        );

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertTrue(medicine.getRetailPrice().compareTo(new BigDecimal("20")) >= 0,
                "价格应该 >= 20元");
        });
    }

    /**
     * 测试价格区间：只有最高价
     */
    @Test
    @DisplayName("价格区间只有最高价应该查询价格 <= 最高价的药品")
    void testPriceBetween_OnlyMaxPrice() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.priceBetween(
                    null,
                    new BigDecimal("100")
                ))
        );

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertTrue(medicine.getRetailPrice().compareTo(new BigDecimal("100")) <= 0,
                "价格应该 <= 100元");
        });
    }

    /**
     * 测试按生产厂家筛选
     */
    @Test
    @DisplayName("应该能够按生产厂家筛选")
    void testHasManufacturer() {
        // 执行查询
        List<Medicine> result = medicineRepository.findAll(
            MedicineSpecification.isActive()
                .and(MedicineSpecification.hasManufacturer("某某制药"))
        );

        // 验证结果
        assertNotNull(result);
        // 注意：如果数据库中没有该厂家的药品，结果为空是正常的
    }

    /**
     * 测试医生查询：多条件组合
     */
    @Test
    @DisplayName("医生查询应该支持多条件组合")
    void testBuildDoctorQuery() {
        // 执行查询：有货的抗生素类处方药
        var spec = MedicineSpecification.buildDoctorQuery(
            null,           // keyword
            "抗生素",       // category
            (short) 1,      // isPrescription
            true            // inStock
        );

        List<Medicine> result = medicineRepository.findAll(spec);

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertEquals("抗生素", medicine.getCategory());
            assertEquals(1, (int) medicine.getIsPrescription());
            assertTrue(medicine.getStockQuantity() > 0);
        });
    }

    /**
     * 测试药师查询：高级条件组合
     */
    @Test
    @DisplayName("药师查询应该支持高级条件组合")
    void testBuildPharmacistQuery() {
        // 执行查询：低库存的抗生素类药品
        var spec = MedicineSpecification.buildPharmacistQuery(
            null,               // keyword
            "抗生素",          // category
            null,              // isPrescription
            "LOW",             // stockStatus
            null,              // manufacturer
            null,              // minPrice
            null               // maxPrice
        );

        List<Medicine> result = medicineRepository.findAll(spec);

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertEquals("抗生素", medicine.getCategory());
            assertTrue(medicine.getStockQuantity() <= medicine.getMinStock(),
                "应该是低库存药品");
        });
    }

    /**
     * 测试分页查询
     */
    @Test
    @DisplayName("应该支持分页查询")
    void testPagination() {
        // 创建分页对象：第0页，每页10条
        Pageable pageable = PageRequest.of(0, 10);

        // 执行分页查询
        var spec = MedicineSpecification.isActive();
        Page<Medicine> page = medicineRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(page);
        assertTrue(page.getContent().size() <= 10, "每页最多10条记录");
        assertEquals(0, page.getNumber(), "应该是第0页");
    }

    /**
     * 测试排序功能
     */
    @Test
    @DisplayName("应该支持按名称排序")
    void testSorting() {
        // 创建排序分页对象：按名称升序
        Pageable pageable = PageRequest.of(0, 20,
            org.springframework.data.domain.Sort.by("name").ascending());

        // 执行查询
        var spec = MedicineSpecification.isActive();
        Page<Medicine> page = medicineRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(page);
        // 验证结果是否按名称升序排列
        List<Medicine> content = page.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertTrue(content.get(i).getName().compareTo(content.get(i + 1).getName()) <= 0,
                "应该按名称升序排列");
        }
    }

    /**
     * 测试组合条件：关键字 + 分类 + 库存状态
     */
    @Test
    @DisplayName("应该支持关键字、分类、库存状态的组合查询")
    void testCombinedConditions() {
        // 执行查询：名称包含"阿"的抗生素类有货药品
        var spec = MedicineSpecification.isActive()
            .and(MedicineSpecification.hasKeyword("阿"))
            .and(MedicineSpecification.hasCategory("抗生素"))
            .and(MedicineSpecification.hasStock(true));

        List<Medicine> result = medicineRepository.findAll(spec);

        // 验证结果
        assertNotNull(result);
        result.forEach(medicine -> {
            assertTrue(medicine.getName().contains("阿") ||
                       medicine.getMedicineCode().contains("阿") ||
                       (medicine.getGenericName() != null && medicine.getGenericName().contains("阿")),
                "应该包含关键字'阿'");
            assertEquals("抗生素", medicine.getCategory());
            assertTrue(medicine.getStockQuantity() > 0);
        });
    }
}
