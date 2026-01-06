package com.his.enums;

/**
 * 性别枚举
 */
public enum GenderEnum {

    /**
     * 女
     */
    FEMALE((short) 0, "女"),

    /**
     * 男
     */
    MALE((short) 1, "男"),

    /**
     * 未知
     */
    UNKNOWN((short) 2, "未知");

    private final Short code;
    private final String description;

    GenderEnum(Short code, String description) {
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
    public static GenderEnum fromCode(Short code) {
        for (GenderEnum gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("未知的性别代码: " + code);
    }
}
