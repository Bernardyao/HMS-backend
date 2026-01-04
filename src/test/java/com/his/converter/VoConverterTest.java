package com.his.converter;

import com.his.entity.Medicine;
import com.his.vo.DoctorMedicineVO;
import com.his.vo.PharmacistMedicineVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoConverter 单元测试
 * <p>
 * 测试Entity到VO的转换逻辑，重点关注：
 * <ul>
 *   <li>字段映射的正确性</li>
 *   <li>库存状态计算的准确性</li>
 *   <li>利润率计算的准确性</li>
 *   <li>Null值处理</li>
 * </ul>
 * </p>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("VO转换工具测试")
class VoConverterTest {

    /**
     * 测试转换：正常药品 → DoctorMedicineVO
     */
    @Test
    @DisplayName("应该正确转换Medicine到DoctorMedicineVO")
    void testToDoctorMedicineVO_Success() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证基本字段
        assertNotNull(vo);
        assertEquals(medicine.getMainId(), vo.getMainId());
        assertEquals(medicine.getMedicineCode(), vo.getMedicineCode());
        assertEquals(medicine.getName(), vo.getName());
        assertEquals(medicine.getGenericName(), vo.getGenericName());
        assertEquals(medicine.getRetailPrice(), vo.getRetailPrice());
        assertEquals(medicine.getStockQuantity(), vo.getStockQuantity());
        assertEquals(medicine.getSpecification(), vo.getSpecification());
        assertEquals(medicine.getUnit(), vo.getUnit());
        assertEquals(medicine.getDosageForm(), vo.getDosageForm());
        assertEquals(medicine.getCategory(), vo.getCategory());
        assertEquals(medicine.getIsPrescription(), vo.getIsPrescription());
        assertEquals(medicine.getManufacturer(), vo.getManufacturer());
        assertEquals(medicine.getStatus(), vo.getStatus());

        // 验证计算字段：库存状态
        assertEquals("IN_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：null药品 → DoctorMedicineVO
     */
    @Test
    @DisplayName("null药品应该转换为null")
    void testToDoctorMedicineVO_Null() {
        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(null);

        // 验证结果
        assertNull(vo);
    }

    /**
     * 测试库存状态计算：正常库存
     */
    @Test
    @DisplayName("正常库存应该返回IN_STOCK")
    void testStockStatus_InStock() {
        // 准备测试数据：库存100，最低库存50
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(100);
        medicine.setMinStock(50);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertEquals("IN_STOCK", vo.getStockStatus());
    }

    /**
     * 测试库存状态计算：低库存
     */
    @Test
    @DisplayName("低库存应该返回LOW_STOCK")
    void testStockStatus_LowStock() {
        // 准备测试数据：库存30，最低库存50
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(30);
        medicine.setMinStock(50);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertEquals("LOW_STOCK", vo.getStockStatus());
    }

    /**
     * 测试库存状态计算：缺货
     */
    @Test
    @DisplayName("缺货应该返回OUT_OF_STOCK")
    void testStockStatus_OutOfStock() {
        // 准备测试数据：库存0
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(0);
        medicine.setMinStock(50);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertEquals("OUT_OF_STOCK", vo.getStockStatus());
    }

    /**
     * 测试库存状态计算：库存为null
     */
    @Test
    @DisplayName("库存为null应该返回OUT_OF_STOCK")
    void testStockStatus_NullQuantity() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(null);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertEquals("OUT_OF_STOCK", vo.getStockStatus());
    }

    /**
     * 测试库存状态计算：最低库存为null
     */
    @Test
    @DisplayName("最低库存为null且库存>0应该返回IN_STOCK")
    void testStockStatus_NullMinStock() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setStockQuantity(100);
        medicine.setMinStock(null);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertEquals("IN_STOCK", vo.getStockStatus());
    }

    /**
     * 测试转换：正常药品 → PharmacistMedicineVO
     */
    @Test
    @DisplayName("应该正确转换Medicine到PharmacistMedicineVO")
    void testToPharmacistMedicineVO_Success() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证基本字段（与DoctorMedicineVO相同的字段）
        assertNotNull(vo);
        assertEquals(medicine.getMainId(), vo.getMainId());
        assertEquals(medicine.getName(), vo.getName());
        assertEquals(medicine.getStockQuantity(), vo.getStockQuantity());
        assertEquals("IN_STOCK", vo.getStockStatus());

        // 验证药师专属字段
        assertEquals(medicine.getPurchasePrice(), vo.getPurchasePrice());
        assertEquals(medicine.getMinStock(), vo.getMinStock());
        assertEquals(medicine.getMaxStock(), vo.getMaxStock());
        assertEquals(medicine.getStorageCondition(), vo.getStorageCondition());
        assertEquals(medicine.getApprovalNo(), vo.getApprovalNo());
        assertEquals(medicine.getExpiryWarningDays(), vo.getExpiryWarningDays());
        assertEquals(medicine.getCreatedAt(), vo.getCreatedAt());
        assertEquals(medicine.getUpdatedAt(), vo.getUpdatedAt());

        // 验证计算字段：利润率
        assertNotNull(vo.getProfitMargin());
        // 利润率 = ((25.80 - 18.50) / 18.50) * 100 = 39.459... ≈ 39.46
        assertTrue(vo.getProfitMargin().compareTo(new BigDecimal("39.46")) >= 0);
        assertTrue(vo.getProfitMargin().compareTo(new BigDecimal("39.47")) < 0);
    }

    /**
     * 测试转换：null药品 → PharmacistMedicineVO
     */
    @Test
    @DisplayName("null药品应该转换为null（药师视图）")
    void testToPharmacistMedicineVO_Null() {
        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(null);

        // 验证结果
        assertNull(vo);
    }

    /**
     * 测试利润率计算：正常情况
     */
    @Test
    @DisplayName("利润率计算应该正确")
    void testProfitMargin_Calculation() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setRetailPrice(new BigDecimal("25.80"));
        medicine.setPurchasePrice(new BigDecimal("18.50"));

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证结果：(25.80 - 18.50) / 18.50 * 100 = 39.459...
        BigDecimal expected = new BigDecimal("39.46");
        assertTrue(vo.getProfitMargin().subtract(expected).abs().compareTo(new BigDecimal("0.01")) < 0,
            "利润率应该约为39.46%");
    }

    /**
     * 测试利润率计算：进货价为null
     */
    @Test
    @DisplayName("进货价为null时利润率应该为0")
    void testProfitMargin_NullPurchasePrice() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setPurchasePrice(null);

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证结果
        assertEquals(BigDecimal.ZERO, vo.getProfitMargin());
    }

    /**
     * 测试利润率计算：进货价为0
     */
    @Test
    @DisplayName("进货价为0时利润率应该为0")
    void testProfitMargin_ZeroPurchasePrice() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setPurchasePrice(BigDecimal.ZERO);
        medicine.setRetailPrice(new BigDecimal("25.80"));

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证结果
        assertEquals(BigDecimal.ZERO, vo.getProfitMargin());
    }

    /**
     * 测试利润率计算：零售价等于进货价（0利润）
     */
    @Test
    @DisplayName("零售价等于进货价时利润率应该为0")
    void testProfitMargin_ZeroProfit() {
        // 准备测试数据
        Medicine medicine = createTestMedicine();
        medicine.setRetailPrice(new BigDecimal("20.00"));
        medicine.setPurchasePrice(new BigDecimal("20.00"));

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证结果 - 使用compareTo避免BigDecimal精度问题
        assertEquals(0, vo.getProfitMargin().compareTo(BigDecimal.ZERO));
    }

    /**
     * 测试利润率计算：高利润率
     */
    @Test
    @DisplayName("高利润率计算应该正确")
    void testProfitMargin_HighProfit() {
        // 准备测试数据：进货10元，售价50元，利润率 = ((50-10)/10)*100 = 400%
        Medicine medicine = createTestMedicine();
        medicine.setRetailPrice(new BigDecimal("50.00"));
        medicine.setPurchasePrice(new BigDecimal("10.00"));

        // 执行转换
        PharmacistMedicineVO vo = VoConverter.toPharmacistMedicineVO(medicine);

        // 验证结果
        assertEquals(new BigDecimal("400.00"), vo.getProfitMargin());
    }

    /**
     * 测试边界情况：所有字段都是null
     */
    @Test
    @DisplayName("应该正确处理所有字段为null的情况")
    void testToDoctorMedicineVO_AllNullFields() {
        // 准备测试数据
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setMedicineCode("MED001");
        medicine.setName("测试药品");
        medicine.setRetailPrice(BigDecimal.ZERO);
        medicine.setStockQuantity(0);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);

        // 执行转换
        DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

        // 验证结果
        assertNotNull(vo);
        assertEquals("OUT_OF_STOCK", vo.getStockStatus());
        assertNull(vo.getGenericName());
        assertNull(vo.getSpecification());
        assertNull(vo.getCategory());
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
        medicine.setSpecification("0.25g*24粒");
        medicine.setUnit("盒");
        medicine.setDosageForm("胶囊");
        medicine.setCategory("抗生素");
        medicine.setIsPrescription((short) 1);
        medicine.setManufacturer("某某制药有限公司");
        medicine.setStorageCondition("密闭，在阴凉干燥处保存");
        medicine.setApprovalNo("国药准字H12345678");
        medicine.setExpiryWarningDays(90);
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);
        medicine.setCreatedAt(LocalDateTime.now());
        medicine.setUpdatedAt(LocalDateTime.now());

        return medicine;
    }
}
