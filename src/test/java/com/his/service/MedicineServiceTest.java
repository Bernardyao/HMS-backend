package com.his.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.his.dto.InventoryStatsVO;
import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.service.impl.MedicineServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MedicineService 单元测试
 * <p>
 * 使用Mockito测试MedicineService的业务逻辑，重点测试：
 * <ul>
 *   <li>医生查询药品功能</li>
 *   <li>药师高级查询功能</li>
 *   <li>库存统计功能</li>
 *   <li>参数验证和异常处理</li>
 * </ul>
 * </p>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("药品服务测试")
@SuppressWarnings("unchecked")
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    private Medicine testMedicine;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testMedicine = createTestMedicine();
    }

    // ==================== searchMedicinesForDoctor 测试 ====================

    @Test
    @DisplayName("医生查询药品 - 成功查询")
    void testSearchMedicinesForDoctor_Success() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(testMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForDoctor(
            "阿莫西林", "抗生素", (short) 1, true, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("医生查询药品 - 空结果")
    void testSearchMedicinesForDoctor_EmptyResult() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForDoctor(
            "不存在的药品", "未知分类", (short) 0, false, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("医生查询药品 - 所有参数为null")
    void testSearchMedicinesForDoctor_AllNullParams() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(testMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询（所有参数为null）
        Page<Medicine> result = medicineService.searchMedicinesForDoctor(
            null, null, null, null, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("医生查询药品 - 只查询有货药品")
    void testSearchMedicinesForDoctor_OnlyInStock() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Medicine inStockMedicine = createTestMedicine();
        inStockMedicine.setStockQuantity(100);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(inStockMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForDoctor(
            null, null, null, true, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        result.getContent().forEach(m -> assertTrue(m.getStockQuantity() > 0));
    }

    // ==================== searchMedicinesForPharmacist 测试 ====================

    @Test
    @DisplayName("药师查询药品 - 高级查询成功")
    void testSearchMedicinesForPharmacist_Success() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(testMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行高级查询
        Page<Medicine> result = medicineService.searchMedicinesForPharmacist(
            "阿莫西林", "抗生素", (short) 1, "LOW",
            "某某制药", new BigDecimal("10"), new BigDecimal("50"), pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("药师查询药品 - 价格区间查询")
    void testSearchMedicinesForPharmacist_PriceRange() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(testMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行价格区间查询
        Page<Medicine> result = medicineService.searchMedicinesForPharmacist(
            null, null, null, null, null,
            new BigDecimal("10"), new BigDecimal("50"), pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("药师查询药品 - 按生产厂家查询")
    void testSearchMedicinesForPharmacist_Manufacturer() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(testMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForPharmacist(
            null, null, null, null, "某某制药", null, null, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("药师查询药品 - 查询低库存药品")
    void testSearchMedicinesForPharmacist_LowStock() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Medicine lowStockMedicine = createTestMedicine();
        lowStockMedicine.setStockQuantity(30);
        lowStockMedicine.setMinStock(50);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(lowStockMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForPharmacist(
            null, null, null, "LOW", null, null, null, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("药师查询药品 - 查询缺货药品")
    void testSearchMedicinesForPharmacist_OutOfStock() {
        // Given - 准备测试数据
        Pageable pageable = PageRequest.of(0, 20);
        Medicine outOfStockMedicine = createTestMedicine();
        outOfStockMedicine.setStockQuantity(0);
        Page<Medicine> mockPage = new PageImpl<>(Arrays.asList(outOfStockMedicine), pageable, 1);

        when(medicineRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(mockPage);

        // When - 执行查询
        Page<Medicine> result = medicineService.searchMedicinesForPharmacist(
            null, null, null, "OUT", null, null, null, pageable
        );

        // Then - 验证结果
        assertNotNull(result);
        verify(medicineRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    // ==================== getInventoryStats 测试 ====================

    @Test
    @DisplayName("库存统计 - 正常情况")
    void testGetInventoryStats_Success() {
        // Given - 准备测试数据
        Medicine inStock = createTestMedicine(); // 库存100 > 最低库存50
        Medicine lowStock = createTestMedicine();
        lowStock.setStockQuantity(30);
        lowStock.setMinStock(50);
        Medicine outOfStock = createTestMedicine();
        outOfStock.setStockQuantity(0);

        List<Medicine> medicines = Arrays.asList(inStock, lowStock, outOfStock);

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When - 执行查询
        InventoryStatsVO stats = medicineService.getInventoryStats();

        // Then - 验证结果
        assertNotNull(stats);
        assertEquals(3L, stats.getTotalMedicines());
        assertEquals(1L, stats.getInStockCount());    // 只有inStock
        assertEquals(1L, stats.getLowStockCount());   // 只有lowStock
        assertEquals(1L, stats.getOutOfStockCount()); // 只有outOfStock

        // 验证占比计算
        assertEquals(33.33, stats.getInStockRate(), 0.01);
        assertEquals(33.33, stats.getLowStockRate(), 0.01);
        assertEquals(33.33, stats.getOutOfStockRate(), 0.01);
    }

    @Test
    @DisplayName("库存统计 - 空列表")
    void testGetInventoryStats_EmptyList() {
        // Given - 准备测试数据
        when(medicineRepository.findAllActive()).thenReturn(Collections.emptyList());

        // When - 执行查询
        InventoryStatsVO stats = medicineService.getInventoryStats();

        // Then - 验证结果
        assertNotNull(stats);
        assertEquals(0L, stats.getTotalMedicines());
        assertEquals(0L, stats.getInStockCount());
        assertEquals(0L, stats.getLowStockCount());
        assertEquals(0L, stats.getOutOfStockCount());
        assertEquals(0.0, stats.getInStockRate());
        assertEquals(0.0, stats.getLowStockRate());
        assertEquals(0.0, stats.getOutOfStockRate());
    }

    @Test
    @DisplayName("库存统计 - 全部正常库存")
    void testGetInventoryStats_AllInStock() {
        // Given - 准备测试数据
        List<Medicine> medicines = Arrays.asList(
            createTestMedicine(),
            createTestMedicine(),
            createTestMedicine()
        );

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When - 执行查询
        InventoryStatsVO stats = medicineService.getInventoryStats();

        // Then - 验证结果
        assertNotNull(stats);
        assertEquals(3L, stats.getTotalMedicines());
        assertEquals(3L, stats.getInStockCount());
        assertEquals(0L, stats.getLowStockCount());
        assertEquals(0L, stats.getOutOfStockCount());
    }

    @Test
    @DisplayName("库存统计 - 全部缺货")
    void testGetInventoryStats_AllOutOfStock() {
        // Given - 准备测试数据
        Medicine outOfStock = createTestMedicine();
        outOfStock.setStockQuantity(0);
        List<Medicine> medicines = Arrays.asList(outOfStock, outOfStock);

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When - 执行查询
        InventoryStatsVO stats = medicineService.getInventoryStats();

        // Then - 验证结果
        assertNotNull(stats);
        assertEquals(2L, stats.getTotalMedicines());
        assertEquals(0L, stats.getInStockCount());
        assertEquals(0L, stats.getLowStockCount());
        assertEquals(2L, stats.getOutOfStockCount());
    }

    // ==================== 辅助方法 ====================

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

        return medicine;
    }
}
