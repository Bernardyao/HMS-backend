package com.his.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据脱敏工具类测试
 *
 * <p>验证各种脱敏方法的正确性
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("数据脱敏工具类测试")
class DataMaskingUtilsTest {

    // ==================== 手机号脱敏测试 ====================

    @Test
    @DisplayName("测试手机号脱敏 - 正常11位手机号")
    void testMaskPhone_Normal() {
        String phone = "13800138000";
        String masked = DataMaskingUtils.maskPhone(phone);
        assertThat(masked).isEqualTo("138****8000");
    }

    @Test
    @DisplayName("测试手机号脱敏 - 不同号段")
    void testMaskPhone_DifferentPrefix() {
        assertThat(DataMaskingUtils.maskPhone("15912345678")).isEqualTo("159****5678");
        assertThat(DataMaskingUtils.maskPhone("18698765432")).isEqualTo("186****5432");
        assertThat(DataMaskingUtils.maskPhone("17766668888")).isEqualTo("177****8888");
    }

    @ParameterizedTest
    @CsvSource({
        "null, null",
        "'', ''",
        "'123', '123'",
        "'123456', '123456'"
    })
    @DisplayName("测试手机号脱敏 - 边界情况")
    void testMaskPhone_Boundary(String input, String expected) {
        String result = DataMaskingUtils.maskPhone(input);
        assertThat(result).isEqualTo(expected);
    }

    // ==================== 身份证号脱敏测试 ====================

    @Test
    @DisplayName("测试身份证号脱敏 - 18位身份证")
    void testMaskIdCard_18Digits() {
        String idCard = "110101199001011234";
        String masked = DataMaskingUtils.maskIdCard(idCard);
        assertThat(masked).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("测试身份证号脱敏 - 15位身份证")
    void testMaskIdCard_15Digits() {
        String idCard = "110101900101123";
        String masked = DataMaskingUtils.maskIdCard(idCard);
        // 110101 + 5个* + 1123
        assertThat(masked).isEqualTo("110101*****1123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "12345678901234"})
    @DisplayName("测试身份证号脱敏 - 边界情况")
    void testMaskIdCard_Boundary(String input) {
        String result = DataMaskingUtils.maskIdCard(input);
        assertThat(result).isEqualTo(input);
    }

    // ==================== 地址脱敏测试 ====================

    @Test
    @DisplayName("测试地址脱敏 - 完整地址")
    void testMaskAddress_Full() {
        String address = "北京市朝阳区建国路88号";
        String masked = DataMaskingUtils.maskAddress(address);
        // 保留前6个字符：北京市朝阳区
        assertThat(masked).isEqualTo("北京市朝阳区***");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "12345"})
    @DisplayName("测试地址脱敏 - 边界情况")
    void testMaskAddress_Boundary(String input) {
        String result = DataMaskingUtils.maskAddress(input);
        assertThat(result).isEqualTo(input);
    }

    // ==================== 银行卡号脱敏测试 ====================

    @Test
    @DisplayName("测试银行卡号脱敏 - 16位卡号")
    void testMaskBankCard_16Digits() {
        String bankCard = "6222021234567890";
        String masked = DataMaskingUtils.maskBankCard(bankCard);
        assertThat(masked).isEqualTo("6222********7890");
    }

    @Test
    @DisplayName("测试银行卡号脱敏 - 19位卡号")
    void testMaskBankCard_19Digits() {
        String bankCard = "6222021234567890123";
        String masked = DataMaskingUtils.maskBankCard(bankCard);
        assertThat(masked).isEqualTo("6222***********0123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "1234567"})
    @DisplayName("测试银行卡号脱敏 - 边界情况")
    void testMaskBankCard_Boundary(String input) {
        String result = DataMaskingUtils.maskBankCard(input);
        assertThat(result).isEqualTo(input);
    }

    // ==================== 姓名脱敏测试 ====================

    @Test
    @DisplayName("测试姓名脱敏 - 单姓单名")
    void testMaskName_TwoCharacters() {
        String name = "张三";
        String masked = DataMaskingUtils.maskName(name);
        assertThat(masked).isEqualTo("张*");
    }

    @Test
    @DisplayName("测试姓名脱敏 - 单姓双名")
    void testMaskName_ThreeCharacters() {
        String name = "张小三";
        String masked = DataMaskingUtils.maskName(name);
        assertThat(masked).isEqualTo("张**");
    }

    @Test
    @DisplayName("测试姓名脱敏 - 复姓")
    void testMaskName_CompoundSurname() {
        String name = "欧阳娜娜";
        String masked = DataMaskingUtils.maskName(name);
        assertThat(masked).isEqualTo("欧阳**");
    }

    @Test
    @DisplayName("测试姓名脱敏 - 长姓名")
    void testMaskName_Long() {
        String name = "司马玉兰明珠";
        String masked = DataMaskingUtils.maskName(name);
        assertThat(masked).isEqualTo("司马****");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "李"})
    @DisplayName("测试姓名脱敏 - 边界情况")
    void testMaskName_Boundary(String input) {
        String result = DataMaskingUtils.maskName(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("测试姓名脱敏 - null值")
    void testMaskName_Null() {
        String result = DataMaskingUtils.maskName(null);
        assertThat(result).isNull();
    }

    // ==================== 邮箱脱敏测试 ====================

    @Test
    @DisplayName("测试邮箱脱敏 - 标准邮箱")
    void testMaskEmail_Standard() {
        String email = "zhangsan@example.com";
        String masked = DataMaskingUtils.maskEmail(email);
        // zhangsan = 8个字符，保留前2个，剩下6个用*替换
        assertThat(masked).isEqualTo("zh******@example.com");
    }

    @Test
    @DisplayName("测试邮箱脱敏 - 长邮箱名")
    void testMaskEmail_Long() {
        String email = "wangxiaoming@hospital.com";
        String masked = DataMaskingUtils.maskEmail(email);
        // wangxiaoming = 12个字符，保留前2个，剩下10个用*替换
        assertThat(masked).isEqualTo("wa**********@hospital.com");
    }

    @Test
    @DisplayName("测试邮箱脱敏 - 短邮箱名")
    void testMaskEmail_Short() {
        String email = "ab@test.com";
        String masked = DataMaskingUtils.maskEmail(email);
        assertThat(masked).isEqualTo("ab***@test.com");
    }

    @Test
    @DisplayName("测试邮箱脱敏 - 单字符邮箱名")
    void testMaskEmail_SingleChar() {
        String email = "a@test.com";
        String masked = DataMaskingUtils.maskEmail(email);
        assertThat(masked).isEqualTo("a***@test.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "invalid-email", "test.com"})
    @DisplayName("测试邮箱脱敏 - 边界情况")
    void testMaskEmail_Boundary(String input) {
        String result = DataMaskingUtils.maskEmail(input);
        assertThat(result).isEqualTo(input);
    }

    // ==================== 统一脱敏方法测试 ====================

    @Test
    @DisplayName("测试统一脱敏方法 - 手机号")
    void testMask_Phone() {
        String result = DataMaskingUtils.mask("13800138000", SensitiveType.PHONE);
        assertThat(result).isEqualTo("138****8000");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - 身份证")
    void testMask_IdCard() {
        String result = DataMaskingUtils.mask("110101199001011234", SensitiveType.ID_CARD);
        assertThat(result).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - 地址")
    void testMask_Address() {
        String result = DataMaskingUtils.mask("北京市朝阳区建国路88号", SensitiveType.ADDRESS);
        // 保留前6个字符
        assertThat(result).isEqualTo("北京市朝阳区***");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - 银行卡")
    void testMask_BankCard() {
        String result = DataMaskingUtils.mask("6222021234567890", SensitiveType.BANK_CARD);
        assertThat(result).isEqualTo("6222********7890");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - 姓名")
    void testMask_Name() {
        String result = DataMaskingUtils.mask("张三", SensitiveType.NAME);
        assertThat(result).isEqualTo("张*");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - 邮箱")
    void testMask_Email() {
        String result = DataMaskingUtils.mask("zhangsan@example.com", SensitiveType.EMAIL);
        assertThat(result).isEqualTo("zh******@example.com");
    }

    @Test
    @DisplayName("测试统一脱敏方法 - null值")
    void testMask_Null() {
        String result = DataMaskingUtils.mask(null, SensitiveType.PHONE);
        assertThat(result).isNull();
    }
}
