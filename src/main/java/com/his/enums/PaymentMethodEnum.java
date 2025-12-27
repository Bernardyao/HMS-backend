package com.his.enums;

/**
 * 支付方式枚举
 */
public enum PaymentMethodEnum {
    
    /**
     * 现金
     */
    CASH((short) 1, "现金"),
    
    /**
     * 银行卡
     */
    CARD((short) 2, "银行卡"),
    
    /**
     * 微信
     */
    WECHAT((short) 3, "微信"),

    /**
     * 支付宝
     */
    ALIPAY((short) 4, "支付宝"),

    /**
     * 医保
     */
    INSURANCE((short) 5, "医保");

    private final Short code;
    private final String description;

    PaymentMethodEnum(Short code, String description) {
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
    public static PaymentMethodEnum fromCode(Short code) {
        for (PaymentMethodEnum method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未知的支付方式代码: " + code);
    }
}
