package com.his.enums;

/**
 * 处方状态枚举
 */
public enum PrescriptionStatusEnum {

    /**
     * 草稿
     */
    DRAFT((short) 0, "草稿"),

    /**
     * 已开方
     */
    ISSUED((short) 1, "已开方"),

    /**
     * 已审核
     */
    REVIEWED((short) 2, "已审核"),

    /**
     * 已发药
     */
    DISPENSED((short) 3, "已发药"),

    /**
     * 已退费
     */
    REFUNDED((short) 4, "已退费"),

    /**
     * 已缴费
     */
    PAID((short) 5, "已缴费");

    private final Short code;
    private final String description;

    PrescriptionStatusEnum(Short code, String description) {
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
    public static PrescriptionStatusEnum fromCode(Short code) {
        for (PrescriptionStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的处方状态代码: " + code);
    }
}
