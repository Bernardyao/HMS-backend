package com.his.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据脱敏注解
 *
 * <p>标注在实体类字段上，Jackson序列化时自动进行脱敏处理
 *
 * <h3>使用示例</h3>
 * <pre>
 * public class Patient {
 *     @SensitiveData(type = SensitiveType.PHONE)
 *     private String phone;
 *
 *     @SensitiveData(type = SensitiveType.ID_CARD)
 *     private String idCard;
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @see SensitiveType
 * @see com.his.config.SensitiveDataSerializer
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /**
     * 脱敏类型
     *
     * @return 脱敏类型枚举
     */
    SensitiveType type();
}
