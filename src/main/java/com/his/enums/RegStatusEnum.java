package com.his.enums;

/**
 * 挂号状态枚举
 */
public enum RegStatusEnum {
    
    /**
     * 待就诊
     */
    WAITING((short) 0, "待就诊"),
    
    /**
     * 已就诊
     */
    COMPLETED((short) 1, "已就诊"),
    
    /**
     * 已取消
     */
    CANCELLED((short) 2, "已取消"),
    
    /**
     * 已退费
     */
    REFUNDED((short) 3, "已退费"),

    /**
     * 已缴挂号费（挂号费已支付，等待就诊）
     */
    PAID_REGISTRATION((short) 4, "已缴挂号费"),

    /**
     * 就诊中（正在就诊，尚未完成）
     */
    IN_CONSULTATION((short) 5, "就诊中");

    private final Short code;
    private final String description;

    RegStatusEnum(Short code, String description) {
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
    public static RegStatusEnum fromCode(Short code) {
        for (RegStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的挂号状态代码: " + code);
    }
}
