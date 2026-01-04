package com.his.service.impl;

import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.test.base.BaseServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * MedicineServiceImpl 库存管理测试
 * <p>
 * 专注于测试药品服务的库存扣减、并发控制、边界条件等复杂场景
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("MedicineServiceImpl 库存管理测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicineServiceImplInventoryTest extends BaseServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    // ==================== 库存扣减测试 ====================

    @Test
    @DisplayName("扣减库存：扣减至零应该成功")
    void deductInventory_ToZero() {
        // Given - 库存为10，扣减10
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", 10);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - 扣减全部库存
        medicineService.updateStock(medicineId, -10, "发药用完");

        // Then - 库存应该为0
        assertEquals(0, medicine.getStockQuantity());
        verify(medicineRepository).save(medicine);
    }

    @Test
    @DisplayName("扣减库存：并发扣减应该使用乐观锁")
    void deductInventory_Concurrent() {
        // Given - 两个线程同时扣减库存
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", 100);
        medicine.setVersion(5); // 初始版本号

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class)))
                .thenAnswer(invocation -> {
                    Medicine m = invocation.getArgument(0);
                    // 模拟版本号冲突
                    if (m.getVersion() == 5) {
                        throw new ObjectOptimisticLockingFailureException(Medicine.class, medicineId);
                    }
                    return m;
                });

        // When & Then - 并发冲突应该抛出乐观锁异常
        ObjectOptimisticLockingFailureException exception = assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> medicineService.updateStock(medicineId, -30, "并发发药")
        );

        assertNotNull(exception);
        verify(medicineRepository).save(any(Medicine.class));
    }

    @Test
    @DisplayName("扣减库存：库存不足应该抛出异常")
    void deductInventory_Insufficient() {
        // Given - 库存为5，尝试扣减10
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", 5);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When & Then - 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> medicineService.updateStock(medicineId, -10, "发药不足"));

        assertTrue(exception.getMessage().contains("库存不足"));
        assertTrue(exception.getMessage().contains("当前库存: 5"));
        assertTrue(exception.getMessage().contains("尝试扣减: 10"));
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    @DisplayName("恢复库存：退费后增加库存应该成功")
    void restoreInventory_AfterRefund() {
        // Given - 当前库存为20，退药增加5
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", 20);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - 增加库存（正数）
        medicineService.updateStock(medicineId, 5, "退药恢复库存");

        // Then - 库存应该为25
        assertEquals(25, medicine.getStockQuantity());
        verify(medicineRepository).save(medicine);
    }

    // ==================== 库存检查测试 ====================

    @ParameterizedTest
    @DisplayName("库存检查：边界值测试")
    @CsvSource({
        "100, 100, true",   // 库存恰好等于需求
        "100, 99, true",    // 库存大于需求
        "100, 101, false",  // 库存小于需求
        "1, 1, true",       // 最小库存边界
        "0, 1, false"       // 零库存
    })
    void checkStock_BoundaryValues(int stock, int required, boolean expected) {
        // Given
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", stock);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When
        boolean result = medicineService.checkStock(medicineId, required);

        // Then
        assertEquals(expected, result);
    }

    // ==================== 参数验证测试 ====================

    @ParameterizedTest
    @DisplayName("更新库存：参数验证（medicineId为null或quantity为0）")
    @ValueSource(ints = {0, 10, -5})
    void updateStock_QuantityZero(int quantity) {
        // Given
        Long medicineId = 1L;

        if (quantity == 0) {
            // When & Then - quantity为0应该抛出异常
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> medicineService.updateStock(medicineId, quantity, "测试"));

            assertTrue(exception.getMessage().contains("变动数量不能为空且不能为0"));
        }
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    @DisplayName("更新库存：medicineId为null应该抛出异常")
    void updateStock_NullMedicineId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> medicineService.updateStock(null, 10, "测试"));

        assertEquals("药品ID不能为空", exception.getMessage());
        verify(medicineRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("更新库存：quantity为null应该抛出异常")
    void updateStock_NullQuantity() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> medicineService.updateStock(1L, null, "测试"));

        assertTrue(exception.getMessage().contains("变动数量不能为空且不能为0"));
        verify(medicineRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("库存检查：参数验证测试")
    void checkStock_ParameterValidation() {
        // Given
        Long medicineId = 1L;
        Medicine medicine = createMockMedicine(medicineId, "测试药品", 100);
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When & Then - quantity为null
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> medicineService.checkStock(medicineId, null));
        assertTrue(exception1.getMessage().contains("参数错误"));

        // When & Then - quantity为0
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> medicineService.checkStock(medicineId, 0));
        assertTrue(exception2.getMessage().contains("参数错误"));

        // When & Then - quantity为负数
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
                () -> medicineService.checkStock(medicineId, -1));
        assertTrue(exception3.getMessage().contains("参数错误"));
    }

    // ==================== 辅助方法 ====================

    private Medicine createMockMedicine(Long id, String name, int stock) {
        Medicine medicine = new Medicine();
        medicine.setMainId(id);
        medicine.setMedicineCode("MED" + String.format("%03d", id));
        medicine.setName(name);
        medicine.setStockQuantity(stock);
        medicine.setStatus((short) 1); // 启用
        medicine.setIsDeleted((short) 0);
        medicine.setVersion(1);
        return medicine;
    }
}
