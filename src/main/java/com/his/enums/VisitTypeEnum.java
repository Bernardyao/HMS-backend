package com.his.enums;

/**
 * 就诊类型枚举
 */
public enum VisitTypeEnum {
    
    /**
     * 初诊
     */
    FIRST((short) 1, "初诊"),
    
    /**
     * 复诊
     */
    FOLLOWUP((short) 2, "复诊"),
    
    /**
     * 急诊
     */
    EMERGENCY((short) 3, "急诊");

    private final Short code;
    private final String description;

    VisitTypeEnum(Short code, String description) {
        this.code = code;
        this.description = description;
    }

    public Short getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static VisitTypeEnum fromCode(Short code) {
        for (VisitTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的就诊类型代码: " + code);
    }
}
