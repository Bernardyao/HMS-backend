package com.his.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.his.common.SensitiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感数据序列化测试
 *
 * <p>验证 @SensitiveData 注解和序列化器的协同工作
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("敏感数据序列化测试")
class SensitiveDataSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ==================== 序列化器单元测试 ====================

    @Test
    @DisplayName("测试序列化器 - 手机号脱敏")
    void testSerializer_Phone() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PHONE);

        String result = serializeWithSerializer(serializer, "13800138000");

        assertThat(result).isEqualTo("\"138****8000\"");
    }

    @Test
    @DisplayName("测试序列化器 - 身份证脱敏")
    void testSerializer_IdCard() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.ID_CARD);

        String result = serializeWithSerializer(serializer, "110101199001011234");

        assertThat(result).isEqualTo("\"110101********1234\"");
    }

    @Test
    @DisplayName("测试序列化器 - 地址脱敏")
    void testSerializer_Address() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.ADDRESS);

        String result = serializeWithSerializer(serializer, "北京市朝阳区建国路88号");

        // 保留前6个字符：北京市朝阳区
        assertThat(result).isEqualTo("\"北京市朝阳区***\"");
    }

    @Test
    @DisplayName("测试序列化器 - 银行卡脱敏")
    void testSerializer_BankCard() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.BANK_CARD);

        String result = serializeWithSerializer(serializer, "6222021234567890");

        assertThat(result).isEqualTo("\"6222********7890\"");
    }

    @Test
    @DisplayName("测试序列化器 - 姓名脱敏")
    void testSerializer_Name() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.NAME);

        String result = serializeWithSerializer(serializer, "张三");

        assertThat(result).isEqualTo("\"张*\"");
    }

    @Test
    @DisplayName("测试序列化器 - 邮箱脱敏")
    void testSerializer_Email() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.EMAIL);

        String result = serializeWithSerializer(serializer, "zhangsan@example.com");

        assertThat(result).isEqualTo("\"zh******@example.com\"");
    }

    @Test
    @DisplayName("测试序列化器 - null值处理")
    void testSerializer_NullValue() throws IOException {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PHONE);

        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(writer);
        SerializerProvider provider = objectMapper.getSerializerProvider();

        serializer.serialize(null, jsonGenerator, provider);
        jsonGenerator.flush();

        String result = writer.toString();
        assertThat(result).isEqualTo("null");
    }

    // ==================== 辅助方法 ====================

    /**
     * 使用指定序列化器序列化对象
     */
    private String serializeWithSerializer(SensitiveDataSerializer serializer, String value) throws IOException {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(writer);
        SerializerProvider provider = objectMapper.getSerializerProvider();

        serializer.serialize(value, jsonGenerator, provider);
        jsonGenerator.flush();

        return writer.toString();
    }
}
