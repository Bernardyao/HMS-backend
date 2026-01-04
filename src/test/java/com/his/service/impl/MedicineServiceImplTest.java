package com.his.service.impl;

import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.test.base.BaseServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("药品服务测试")
class MedicineServiceImplTest extends BaseServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    @Test
    @DisplayName("更新库存 - 增加库存成功")
    void updateStock_increase_success() {
        Long medicineId = 1L;
        Integer quantity = 10; // Increase by 10
        String reason = "Replenishment";

        Medicine medicine = new Medicine();
        medicine.setMainId(medicineId);
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(inv -> inv.getArgument(0));

        medicineService.updateStock(medicineId, quantity, reason);

        assertThat(medicine.getStockQuantity()).isEqualTo(60);
        verify(medicineRepository).save(medicine);
    }

    @Test
    @DisplayName("更新库存 - 扣减库存成功")
    void updateStock_decrease_success() {
        Long medicineId = 1L;
        Integer quantity = -10; // Decrease by 10
        String reason = "Breakage";

        Medicine medicine = new Medicine();
        medicine.setMainId(medicineId);
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(inv -> inv.getArgument(0));

        medicineService.updateStock(medicineId, quantity, reason);

        assertThat(medicine.getStockQuantity()).isEqualTo(40);
        verify(medicineRepository).save(medicine);
    }

    @Test
    @DisplayName("更新库存 - 扣减时库存不足抛出异常")
    void updateStock_decrease_insufficient_throwsException() {
        Long medicineId = 1L;
        Integer quantity = -60; // Decrease by 60 (Stock is 50)
        String reason = "Error";

        Medicine medicine = new Medicine();
        medicine.setMainId(medicineId);
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        assertThatThrownBy(() -> medicineService.updateStock(medicineId, quantity, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("库存不足");

        verify(medicineRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新库存 - 药品不存在抛出异常")
    void updateStock_medicineNotFound_throwsException() {
        Long medicineId = 999L;

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicineService.updateStock(medicineId, 10, "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("药品不存在");
    }

    // ==================== searchMedicines Tests ====================

    @Test
    @DisplayName("搜索药品 - 有关键字返回结果")
    void testSearchMedicines_WithKeyword_ReturnsResults() {
        // Given
        String keyword = "阿";
        List<Medicine> medicines = Arrays.asList(
            createMedicine(1L, "阿莫西林", "ABC001"),
            createMedicine(2L, "阿司匹林", "ABC002")
        );

        when(medicineRepository.searchByKeyword(keyword, (short) 0)).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.searchMedicines(keyword);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("阿莫西林");
        assertThat(result.get(1).getName()).isEqualTo("阿司匹林");
        verify(medicineRepository).searchByKeyword(keyword, (short) 0);
    }

    @Test
    @DisplayName("搜索药品 - 空关键字返回所有启用药品")
    void testSearchMedicines_EmptyKeyword_ReturnsAllActive() {
        // Given
        List<Medicine> medicines = Arrays.asList(
            createMedicine(1L, "阿莫西林", "ABC001"),
            createMedicine(2L, "青霉素", "DEF001")
        );

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.searchMedicines("");

        // Then
        assertThat(result).hasSize(2);
        verify(medicineRepository).findAllActive();
        verify(medicineRepository, never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("搜索药品 - null关键字返回所有启用药品")
    void testSearchMedicines_NullKeyword_ReturnsAllActive() {
        // Given
        List<Medicine> medicines = Collections.singletonList(
            createMedicine(1L, "阿莫西林", "ABC001")
        );

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.searchMedicines(null);

        // Then
        assertThat(result).hasSize(1);
        verify(medicineRepository).findAllActive();
    }

    @Test
    @DisplayName("搜索药品 - 无结果返回空列表")
    void testSearchMedicines_NoResults_ReturnsEmptyList() {
        // Given
        String keyword = "不存在的药品";
        when(medicineRepository.searchByKeyword(keyword, (short) 0)).thenReturn(Collections.emptyList());

        // When
        List<Medicine> result = medicineService.searchMedicines(keyword);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== getById Tests ====================

    @Test
    @DisplayName("根据ID查询药品 - 成功")
    void testGetById_Success() {
        // Given
        Long medicineId = 1L;
        Medicine medicine = createMedicine(medicineId, "阿莫西林", "ABC001");
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When
        Medicine result = medicineService.getById(medicineId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMainId()).isEqualTo(medicineId);
        assertThat(result.getName()).isEqualTo("阿莫西林");
    }

    @Test
    @DisplayName("根据ID查询药品 - 药品不存在")
    void testGetById_NotFound_ThrowsException() {
        // Given
        Long medicineId = 999L;
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> medicineService.getById(medicineId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("药品不存在");
    }

    @Test
    @DisplayName("根据ID查询药品 - 药品已删除")
    void testGetById_Deleted_ThrowsException() {
        // Given
        Long medicineId = 1L;
        Medicine medicine = createMedicine(medicineId, "阿莫西林", "ABC001");
        medicine.setIsDeleted((short) 1); // 已删除

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When & Then
        assertThatThrownBy(() -> medicineService.getById(medicineId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("药品已被删除");
    }

    @Test
    @DisplayName("根据ID查询药品 - ID为null")
    void testGetById_NullId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> medicineService.getById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("药品ID不能为空");
    }

    // ==================== getAllActive Tests ====================

    @Test
    @DisplayName("查询所有启用药品 - 返回列表")
    void testGetAllActive_ReturnsList() {
        // Given
        List<Medicine> medicines = Arrays.asList(
            createMedicine(1L, "阿莫西林", "ABC001"),
            createMedicine(2L, "青霉素", "DEF001"),
            createMedicine(3L, "头孢", "GHI001")
        );

        when(medicineRepository.findAllActive()).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.getAllActive();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting("name").containsExactly("阿莫西林", "青霉素", "头孢");
    }

    @Test
    @DisplayName("查询所有启用药品 - 空列表")
    void testGetAllActive_ReturnsEmpty() {
        // Given
        when(medicineRepository.findAllActive()).thenReturn(Collections.emptyList());

        // When
        List<Medicine> result = medicineService.getAllActive();

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== checkStock Tests ====================

    @Test
    @DisplayName("检查库存 - 库存充足")
    void testCheckStock_Sufficient_ReturnsTrue() {
        // Given
        Long medicineId = 1L;
        Integer quantity = 10;
        Medicine medicine = createMedicine(medicineId, "阿莫西林", "ABC001");
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When
        boolean result = medicineService.checkStock(medicineId, quantity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("检查库存 - 库存不足")
    void testCheckStock_Insufficient_ReturnsFalse() {
        // Given
        Long medicineId = 1L;
        Integer quantity = 60;
        Medicine medicine = createMedicine(medicineId, "阿莫西林", "ABC001");
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When
        boolean result = medicineService.checkStock(medicineId, quantity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("检查库存 - 刚好足够")
    void testCheckStock_ExactlyEnough_ReturnsTrue() {
        // Given
        Long medicineId = 1L;
        Integer quantity = 50;
        Medicine medicine = createMedicine(medicineId, "阿莫西林", "ABC001");
        medicine.setStockQuantity(50);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        // When
        boolean result = medicineService.checkStock(medicineId, quantity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("检查库存 - medicineId为null")
    void testCheckStock_NullMedicineId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> medicineService.checkStock(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数错误");
    }

    @Test
    @DisplayName("检查库存 - quantity为null")
    void testCheckStock_NullQuantity_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> medicineService.checkStock(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数错误");
    }

    @Test
    @DisplayName("检查库存 - quantity为0")
    void testCheckStock_ZeroQuantity_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> medicineService.checkStock(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数错误");
    }

    @Test
    @DisplayName("检查库存 - quantity为负数")
    void testCheckStock_NegativeQuantity_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> medicineService.checkStock(1L, -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数错误");
    }

    // ==================== Helper Methods ====================

    private Medicine createMedicine(Long id, String name, String code) {
        Medicine medicine = new Medicine();
        medicine.setMainId(id);
        medicine.setName(name);
        medicine.setMedicineCode(code);
        medicine.setStockQuantity(100);
        medicine.setIsDeleted((short) 0);
        return medicine;
    }
}
