package com.his.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.his.common.DataMaskingContext;
import com.his.common.DataMaskingUtils;
import com.his.common.SensitiveData;
import com.his.common.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 敏感数据脱敏序列化器
 *
 * <p>实现 Jackson 的 ContextualSerializer 接口，支持通过注解动态配置脱敏类型
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>在实体字段上标注 @SensitiveData 注解并指定类型</li>
 *   <li>Jackson 序列化时自动创建序列化器上下文</li>
 *   <li>根据注解的类型创建对应的序列化器实例</li>
 *   <li>序列化时检查 {@link DataMaskingContext} 状态，决定是否脱敏</li>
 *   <li>如果脱敏被禁用（管理员查看），则显示明文；否则调用脱敏工具类处理数据</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>
 * public class Patient {
 *     @SensitiveData(type = SensitiveType.PHONE)
 *     @JsonSerialize(using = SensitiveDataSerializer.class)
 *     private String phone;
 * }
 *
 * // Controller 中为管理员禁用脱敏
 * {@code @GetMapping("/api/admin/patients/{id}")}
 * public Result{@code <Patient>} getPatientDetail(@PathVariable Long id) {
 *     try (DataMaskingContext.Scope scope = DataMaskingContext.disable()) {
 *         // 返回的 JSON 中包含明文数据
 *         return Result.success(patientService.getById(id));
 *     }
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @see com.his.common.SensitiveData
 * @see com.his.common.DataMaskingUtils
 * @see com.his.common.DataMaskingContext
 */
@Slf4j
public class SensitiveDataSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type;

    /**
     * 默认构造函数（Jackson 需要无参构造函数）
     */
    public SensitiveDataSerializer() {
    }

    /**
     * 带类型的构造函数（用于创建指定类型的序列化器）
     *
     * @param type 脱敏类型
     */
    public SensitiveDataSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 检查是否禁用脱敏（管理员查看明文）
        if (DataMaskingContext.isMaskingDisabled()) {
            log.debug("脱敏已禁用，返回明文数据 - 类型: {}", type);
            gen.writeString(value);
            return;
        }

        // 根据类型进行脱敏
        String maskedValue = DataMaskingUtils.mask(value, type);
        gen.writeString(maskedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 获取字段上的 @SensitiveData 注解
        SensitiveData annotation = property.getAnnotation(SensitiveData.class);

        if (annotation != null) {
            // 如果有注解，创建带类型的序列化器
            return new SensitiveDataSerializer(annotation.type());
        }

        // 如果没有注解，返回默认序列化器
        return this;
    }
}
