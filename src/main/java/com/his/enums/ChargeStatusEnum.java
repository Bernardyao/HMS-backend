package com.his.enums;

/**
 * 收费状态枚举
 */
public enum ChargeStatusEnum {

    /**
     * 未缴费
     */
    UNPAID((short) 0, "未缴费"),

    /**
     * 已缴费
     */
    PAID((short) 1, "已缴费"),

    /**
     * 已退费
     */
    REFUNDED((short) 2, "已退费");

    private final Short code;
    private final String description;

    ChargeStatusEnum(Short code, String description) {
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
    public static ChargeStatusEnum fromCode(Short code) {
        for (ChargeStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的收费状态代码: " + code);
    }
}
