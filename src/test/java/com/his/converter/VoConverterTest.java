package com.his.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.his.entity.Medicine;
import com.his.vo.MedicineVO;
import com.his.vo.views.MedicineViews;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoConverter 单元测试
 * <p>
 * 测试Entity到VO的转换逻辑，重点关注：
 * <ul>
 *   <li>字段映射的正确性</li>
 *   <li>JsonView支持的正确性</li>
 *   <li>库存状态计算的准确性</li>
 *   <li>利润率计算的准确性</li>
 *   <li>Null值处理</li>
 * </ul>
 * </p>
 *
 * @author HIS 开发团队
 * @version 2.0
 */
@DisplayName("VO转换工具测试")
class VoConverterTest {

    /**
     * 测试转换：正常药品 → MedicineVO (Public视图)
     */
    @Test
    @DisplayName("应该正确转换Medicine到MedicineVO(Public视图)")
    void testToMedicineVO_PublicView() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();

        // 执行转换 - Public视图
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Public.class);

        // 验证基本字段（Public视图字段）
        assertNotNull(vo);
        assertEquals(medicine.getMainId(), vo.getMainId());
        assertEquals(medicine.getMedicineCode(), vo.getMedicineCode());
        assertEquals(medicine.getName(), vo.getName());
        assertEquals(medicine.getGenericName(), vo.getGenericName());
        assertEquals(medicine.getRetailPrice(), vo.getRetailPrice());
        assertEquals(medicine.getStockQuantity(), vo.getStockQuantity());
        assertEquals(medicine.getCategory(), vo.getCategory());
        // 注意：Medicine实体使用Short，但MedicineVO使用Integer，需要用intValue()比较
        assertEquals(medicine.getIsPrescription().intValue(), vo.getIsPrescription());
        assertEquals(medicine.getStatus().intValue(), vo.getStatus());

        // Public视图不应该包含这些字段
        assertNull(vo.getSpecification(), "Public视图不应该包含specification");
        assertNull(vo.getUnit(), "Public视图不应该包含unit");
        assertNull(vo.getDosageForm(), "Public视图不应该包含dosageForm");
        assertNull(vo.getManufacturer(), "Public视图不应该包含manufacturer");
        assertNull(vo.getStockStatus(), "Public视图不应该包含stockStatus");
        assertNull(vo.getPurchasePrice(), "Public视图不应该包含purchasePrice");
        assertNull(vo.getProfitMargin(), "Public视图不应该包含profitMargin");
    }

    /**
     * 测试转换：正常药品 → MedicineVO (Doctor视图)
     */
    @Test
    @DisplayName("应该正确转换Medicine到MedicineVO(Doctor视图)")
    void testToMedicineVO_DoctorView() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();

        // 执行转换 - Doctor视图
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);

        // 验证基本字段
        assertNotNull(vo);
        assertEquals(medicine.getMainId(), vo.getMainId());
        assertEquals(medicine.getMedicineCode(), vo.getMedicineCode());
        assertEquals(medicine.getName(), vo.getName());

        // Doctor视图应该包含这些字段
        assertEquals(medicine.getSpecification(), vo.getSpecification());
        assertEquals(medicine.getUnit(), vo.getUnit());
        assertEquals(medicine.getDosageForm(), vo.getDosageForm());
        assertEquals(medicine.getManufacturer(), vo.getManufacturer());
        assertEquals("IN_STOCK", vo.getStockStatus());

        // Doctor视图不应该包含药师专属字段
        assertNull(vo.getPurchasePrice(), "Doctor视图不应该包含purchasePrice");
        assertNull(vo.getProfitMargin(), "Doctor视图不应该包含profitMargin");
        assertNull(vo.getMinStock(), "Doctor视图不应该包含minStock");
    }

    /**
     * 测试转换：正常药品 → MedicineVO (Pharmacist视图)
     */
    @Test
    @DisplayName("应该正确转换Medicine到MedicineVO(Pharmacist视图)")
    void testToMedicineVO_PharmacistView() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();

        // 执行转换 - Pharmacist视图
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);

        // 验证基本字段
        assertNotNull(vo);
        assertEquals(medicine.getMainId(), vo.getMainId());
        assertEquals(medicine.getMedicineCode(), vo.getMedicineCode());
        assertEquals(medicine.getName(), vo.getName());

        // Pharmacist视图应该包含所有字段
        assertEquals(medicine.getSpecification(), vo.getSpecification());
        assertEquals(medicine.getPurchasePrice(), vo.getPurchasePrice());
        assertEquals(medicine.getMinStock(), vo.getMinStock());
        assertEquals(medicine.getMaxStock(), vo.getMaxStock());
        assertEquals("IN_STOCK", vo.getStockStatus());
        assertNotNull(vo.getProfitMargin());

        // 验证利润率计算
        // 利润率 = ((25.80 - 18.50) / 18.50) × 100% = 39.46%
        assertEquals(new BigDecimal("39.46"), vo.getProfitMargin().setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 测试转换：null值处理
     */
    @Test
    @DisplayName("应该正确处理null值")
    void testToMedicineVO_Null() {
        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(null, MedicineViews.Public.class);

        // 验证结果
        assertNull(vo);
    }

    /**
     * 测试转换：库存为0 → OUT_OF_STOCK
     */
    @Test
    @DisplayName("应该正确计算库存状态-缺货")
    void testToMedicineVO_OutOfStock() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(0);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);

        // 验证库存状态
        assertEquals("OUT_OF_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：库存低于最低库存 → LOW_STOCK
     */
    @Test
    @DisplayName("应该正确计算库存状态-低库存")
    void testToMedicineVO_LowStock() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(30);  // 低于minStock (50)
        medicine.setMinStock(50);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);

        // 验证库存状态
        assertEquals("LOW_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：库存高于最低库存 → IN_STOCK
     */
    @Test
    @DisplayName("应该正确计算库存状态-正常")
    void testToMedicineVO_InStock() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(100);  // 高于minStock (50)
        medicine.setMinStock(50);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);

        // 验证库存状态
        assertEquals("IN_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：库存为null → OUT_OF_STOCK
     */
    @Test
    @DisplayName("应该正确处理库存为null的情况")
    void testToMedicineVO_NullStockQuantity() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(null);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Doctor.class);

        // 验证库存状态
        assertEquals("OUT_OF_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：进货价为0 → 利润率为0
     */
    @Test
    @DisplayName("应该正确处理进货价为0的情况")
    void testToMedicineVO_ZeroPurchasePrice() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setPurchasePrice(BigDecimal.ZERO);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);

        // 验证利润率
        assertEquals(BigDecimal.ZERO, vo.getProfitMargin());
    }

    /**
     * 测试转换：进货价为null → 利润率为0
     */
    @Test
    @DisplayName("应该正确处理进货价为null的情况")
    void testToMedicineVO_NullPurchasePrice() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setPurchasePrice(null);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);

        // 验证利润率
        assertEquals(BigDecimal.ZERO, vo.getProfitMargin());
    }

    /**
     * 测试转换：零售价和进货价相同 → 利润率为100%
     */
    @Test
    @DisplayName("应该正确计算利润率-相同价格")
    void testToMedicineVO_SamePrice() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        BigDecimal price = new BigDecimal("20.00");
        medicine.setRetailPrice(price);
        medicine.setPurchasePrice(price);

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);

        // 验证利润率 ((20 - 20) / 20) × 100% = 0%
        assertEquals(new BigDecimal("0.00"), vo.getProfitMargin().setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 测试转换：验证所有Pharmacist视图字段
     */
    @Test
    @DisplayName("应该正确转换所有Pharmacist视图字段")
    void testToMedicineVO_AllPharmacistFields() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStorageCondition("密闭，在阴凉干燥处保存");
        medicine.setApprovalNo("国药准字H12345678");
        medicine.setExpiryWarningDays(90);
        medicine.setCreatedAt(LocalDateTime.of(2023, 1, 1, 10, 0, 0));
        medicine.setUpdatedAt(LocalDateTime.of(2023, 12, 1, 15, 30, 0));

        // 执行转换
        MedicineVO vo = VoConverter.toMedicineVO(medicine, MedicineViews.Pharmacist.class);

        // 验证所有字段
        assertEquals(medicine.getStorageCondition(), vo.getStorageCondition());
        assertEquals(medicine.getApprovalNo(), vo.getApprovalNo());
        assertEquals(medicine.getExpiryWarningDays(), vo.getExpiryWarningDays());
        assertEquals(medicine.getCreatedAt(), vo.getCreatedAt());
        assertEquals(medicine.getUpdatedAt(), vo.getUpdatedAt());
    }

    /**
     * 创建测试用的Medicine实体
     */
    private Medicine createTestMedicine() {
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setMedicineCode("MED001");
        medicine.setName("阿莫西林胶囊");
        medicine.setGenericName("阿莫西林");
        medicine.setRetailPrice(new BigDecimal("25.80"));
        medicine.setPurchasePrice(new BigDecimal("18.50"));
        medicine.setStockQuantity(100);
        medicine.setMinStock(50);
        medicine.setMaxStock(500);
        medicine.setCategory("抗生素");
        medicine.setIsPrescription((short) 1);
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setDosageForm("胶囊");
        medicine.setManufacturer("某某制药有限公司");
        medicine.setStorageCondition("密闭，在阴凉干燥处保存");
        medicine.setApprovalNo("国药准字H12345678");
        medicine.setExpiryWarningDays(90);
        medicine.setStatus((short) 1);
        medicine.setCreatedAt(LocalDateTime.of(2023, 1, 1, 10, 0, 0));
        medicine.setUpdatedAt(LocalDateTime.of(2023, 12, 1, 15, 30, 0));

        return medicine;
    }
}
