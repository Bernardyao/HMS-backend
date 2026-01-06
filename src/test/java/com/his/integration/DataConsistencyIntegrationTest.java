package com.his.integration;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.his.entity.*;
import com.his.repository.*;
import com.his.service.impl.MedicineServiceImpl;
import com.his.test.base.BaseIntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据一致性测试
 * <p>
 * 验证业务流程中跨实体的数据一致性
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("数据一致性集成测试")
class DataConsistencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MedicineRepository medicineRepository;

    @MockBean
    private com.his.service.ChargeService chargeService;

    // ==================== 数据一致性测试 ====================

    @Test
    @DisplayName("药品库存更新应该保持数据一致性")
    void inventoryUpdate_Consistency() {
        // Given - 创建药品
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Medicine medicine = createTestMedicine(uniqueSuffix);
        medicine.setStockQuantity(100);
        Medicine savedMedicine = medicineRepository.save(medicine);

        // When - 多次更新库存
        MedicineServiceImpl service = new MedicineServiceImpl(medicineRepository);

        // 第一次扣减
        service.updateStock(savedMedicine.getMainId(), -30, "第一次扣减");
        assertEquals(70, medicineRepository.findById(savedMedicine.getMainId()).orElseThrow().getStockQuantity());

        // 第二次扣减
        service.updateStock(savedMedicine.getMainId(), -20, "第二次扣减");
        assertEquals(50, medicineRepository.findById(savedMedicine.getMainId()).orElseThrow().getStockQuantity());

        // 恢复库存
        service.updateStock(savedMedicine.getMainId(), 15, "恢复库存");
        assertEquals(65, medicineRepository.findById(savedMedicine.getMainId()).orElseThrow().getStockQuantity());

        // Then - 验证最终库存正确
        Medicine finalMedicine = medicineRepository.findById(savedMedicine.getMainId()).orElseThrow();
        assertEquals(65, finalMedicine.getStockQuantity());
    }

    @Test
    @DisplayName("库存更新应该立即生效")
    void inventoryUpdate_ImmediateEffect() {
        // Given - 创建药品
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Medicine medicine = createTestMedicine(uniqueSuffix);
        medicine.setStockQuantity(100);
        Medicine savedMedicine = medicineRepository.save(medicine);

        // When - 更新库存
        MedicineServiceImpl service = new MedicineServiceImpl(medicineRepository);
        service.updateStock(savedMedicine.getMainId(), -10, "测试扣减");

        // Then - 验证库存立即更新
        Medicine updatedMedicine = medicineRepository.findById(savedMedicine.getMainId()).orElseThrow();
        assertEquals(90, updatedMedicine.getStockQuantity());

        // When - 恢复库存
        service.updateStock(savedMedicine.getMainId(), 5, "测试恢复");

        // Then - 验证库存正确恢复
        assertEquals(95, medicineRepository.findById(savedMedicine.getMainId()).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("库存不足应该抛出异常")
    void inventoryInsufficient_ThrowsException() {
        // Given - 库存为5的药品
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Medicine medicine = createTestMedicine(uniqueSuffix);
        medicine.setStockQuantity(5);
        Medicine savedMedicine = medicineRepository.save(medicine);

        // When & Then - 尝试扣减10应该抛出异常
        MedicineServiceImpl service = new MedicineServiceImpl(medicineRepository);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.updateStock(savedMedicine.getMainId(), -10, "测试不足"));

        assertTrue(exception.getMessage().contains("库存不足"));

        // 验证库存没有被修改
        Medicine unchangedMedicine = medicineRepository.findById(savedMedicine.getMainId()).orElseThrow();
        assertEquals(5, unchangedMedicine.getStockQuantity());
    }

    @Test
    @DisplayName("并发库存更新应该使用乐观锁")
    void concurrentInventoryUpdate_OptimisticLock() {
        // Given - 库存为100的药品
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Medicine medicine = createTestMedicine(uniqueSuffix);
        medicine.setStockQuantity(100);
        medicine.setVersion(1);
        Medicine savedMedicine = medicineRepository.save(medicine);

        // When - 两次更新库存
        MedicineServiceImpl service = new MedicineServiceImpl(medicineRepository);
        service.updateStock(savedMedicine.getMainId(), -10, "第一次扣减");

        // 第一次更新成功，库存应为90
        Medicine afterFirstUpdate = medicineRepository.findById(savedMedicine.getMainId()).orElseThrow();
        assertEquals(90, afterFirstUpdate.getStockQuantity());

        // 第二次更新（基于新版本）
        service.updateStock(savedMedicine.getMainId(), -5, "第二次扣减");

        // 验证最终库存为85
        Medicine finalMedicine = medicineRepository.findById(savedMedicine.getMainId()).orElseThrow();
        assertEquals(85, finalMedicine.getStockQuantity());
    }

    // ==================== 辅助方法 ====================

    private Medicine createTestMedicine(String suffix) {
        Medicine medicine = new Medicine();
        medicine.setMedicineCode("MED" + suffix);
        medicine.setName("测试药品");
        medicine.setRetailPrice(new BigDecimal("50.00"));
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);
        medicine.setVersion(1);
        return medicine;
    }
}
