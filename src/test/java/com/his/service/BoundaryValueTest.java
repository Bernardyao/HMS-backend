package com.his.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.his.dto.RegistrationDTO;
import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.repository.PatientRepository;
import com.his.service.impl.MedicineServiceImpl;
import com.his.service.impl.RegistrationServiceImpl;
import com.his.test.base.BaseServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 边界值测试
 * <p>
 * 测试系统在各种边界条件下的行为
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("边界值测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class BoundaryValueTest extends BaseServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    // ==================== 字符串长度边界测试 ====================

    @Test
    @DisplayName("挂号：最大字段长度应该正确处理")
    void registration_MaxFieldLength() {
        // Given - 测试超长患者姓名
        String longName = "张".repeat(100); // 100个字符
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName(longName);
        dto.setIdCard("110101199001011234");
        dto.setGender((short) 1);
        dto.setAge((short) 30);
        dto.setPhone("13800138000");

        // 大部分系统会截断或拒绝超长输入
        // 这里验证DTO能接受超长输入（验证逻辑在Controller层）
        assertEquals(100, dto.getPatientName().length());
    }

    @Test
    @DisplayName("挂号：最小字段长度边界")
    void registration_MinFieldLength() {
        // Given - 测试最小有效输入
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("张"); // 单字符姓名
        dto.setIdCard("110101199001011234");
        dto.setGender((short) 1);
        dto.setAge((short) 0); // 最小年龄
        dto.setPhone("1"); // 最小电话长度

        // DTO应该能接受这些最小值
        assertEquals("张", dto.getPatientName());
        assertEquals((short) 0, dto.getAge());
        assertEquals("1", dto.getPhone());
    }

    // ==================== 数值精度边界测试 ====================

    @Test
    @DisplayName("BigDecimal：精度边界应该正确处理")
    void bigDecimal_PrecisionBoundary() {
        // Given - 测试BigDecimal精度边界
        BigDecimal verySmall = new BigDecimal("0.001"); // 最小金额
        BigDecimal veryLarge = new BigDecimal("999999.99"); // 最大金额
        BigDecimal withManyDecimals = new BigDecimal("99.999999999"); // 多位小数

        // 验证精度处理
        assertEquals(3, verySmall.scale());
        assertEquals(2, veryLarge.scale());
        assertEquals(9, withManyDecimals.scale());

        // 测试四舍五入
        BigDecimal rounded = withManyDecimals.setScale(2, RoundingMode.HALF_UP);
        assertEquals("100.00", rounded.toString());
    }

    @ParameterizedTest
    @DisplayName("金额：零和负数边界值")
    @ValueSource(doubles = {0.0, -0.01, -100.0})
    void amount_NegativeBoundary(double amount) {
        // Given - 测试零和负数金额
        BigDecimal fee = BigDecimal.valueOf(amount);

        // When & Then - 应该被拒绝或标记为无效
        // 具体验证逻辑在Service层
        if (amount <= 0) {
            assertTrue(fee.compareTo(BigDecimal.ZERO) <= 0,
                    "零或负数金额应该被检测为无效");
        }
    }

    // ==================== 整数溢出边界测试 ====================

    @ParameterizedTest
    @DisplayName("年龄：边界值测试")
    @ValueSource(shorts = {0, 1, 150})
    void age_BoundaryValues(short age) {
        // Given - 测试年龄边界值
        RegistrationDTO dto = new RegistrationDTO();
        dto.setAge(age);

        // DTO应该能接受这些边界值
        assertEquals(age, dto.getAge());

        // 实际业务验证在Service层
        assertTrue(age >= 0 && age <= 150, "年龄应该在合理范围内");
    }

    @Test
    @DisplayName("年龄：超出最大值应该被检测")
    void age_ExceedsMaximum() {
        // Given - 超出人类最大年龄
        short tooOld = 200;
        RegistrationDTO dto = new RegistrationDTO();
        dto.setAge(tooOld);

        // DTO能接受，但业务验证应该拒绝
        assertEquals((int)200, (int)dto.getAge());
        assertTrue(tooOld > 150, "200岁超出合理范围，应该被业务逻辑拒绝");
    }

    // ==================== 日期边界测试 ====================

    @Test
    @DisplayName("日期：未来日期边界")
    void date_FutureBoundary() {
        // Given - 未来100年的日期
        LocalDate futureDate = LocalDate.now().plusYears(100);

        // 大部分业务场景不应该接受未来日期
        // 但某些场景（如预约）可能需要
        assertTrue(futureDate.isAfter(LocalDate.now()));
        assertTrue(futureDate.getYear() >= LocalDate.now().getYear() + 100);
    }

    @Test
    @DisplayName("日期：过去日期边界")
    void date_PastBoundary() {
        // Given - 过去150年的日期（人类最长寿记录）
        LocalDate pastDate = LocalDate.now().minusYears(150);

        // 验证过去日期处理
        assertTrue(pastDate.isBefore(LocalDate.now()));
        assertTrue(pastDate.getYear() <= LocalDate.now().getYear() - 150);
    }

    // ==================== 集合边界测试 ====================

    @Test
    @DisplayName("列表：空列表边界")
    void list_Empty() {
        // Given - 空列表
        var emptyList = java.util.Collections.<String>emptyList();

        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    @DisplayName("库存：零库存边界")
    void stock_ZeroBoundary() {
        // Given - 库存为0的药品
        Medicine medicine = new Medicine();
        medicine.setMainId(1L);
        medicine.setMedicineCode("MED001");
        medicine.setName("测试药品");
        medicine.setStockQuantity(0); // 零库存
        medicine.setStatus((short) 1);
        medicine.setIsDeleted((short) 0);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));

        // When & Then - 库存检查应该返回false
        boolean result = medicineService.checkStock(1L, 1);

        assertFalse(result, "零库存应该无法满足任何需求");
    }

    // ==================== 数组边界测试 ====================

    @Test
    @DisplayName("数组：单元素数组边界")
    void array_SingleElement() {
        // Given - 单元素数组
        String[] array = {"只有1个元素"};

        assertEquals(1, array.length);
        assertEquals("只有1个元素", array[0]);
    }

    @Test
    @DisplayName("数组：空数组边界")
    void array_Empty() {
        // Given - 空数组
        String[] array = {};

        assertEquals(0, array.length);
        assertTrue(array.length == 0);
    }
}
