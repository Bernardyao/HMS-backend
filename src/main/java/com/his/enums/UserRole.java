package com.his.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {

    /**
     * 管理员 - 可以访问所有科室数据
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 医生 - 只能访问自己所在科室的数据
     */
    DOCTOR("DOCTOR", "医生"),

    /**
     * 护士 - 只能访问自己所在科室的数据
     */
    NURSE("NURSE", "护士"),

    /**
     * 药师 - 负责发药、退药、库存管理
     */
    PHARMACIST("PHARMACIST", "药师"),

    /**
     * 收费员 - 负责收费、退费
     */
    CASHIER("CASHIER", "收费员");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
