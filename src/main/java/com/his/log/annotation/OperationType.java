package com.his.log.annotation;

/**
 * 操作类型枚举
 *
 * 用于业务操作日志的分类和统计
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
public enum OperationType {

    /**
     * 查询操作
     */
    QUERY("查询"),

    /**
     * 创建操作
     */
    CREATE("创建"),

    /**
     * 更新操作
     */
    UPDATE("更新"),

    /**
     * 删除操作
     */
    DELETE("删除"),

    /**
     * 登录操作
     */
    LOGIN("登录"),

    /**
     * 登出操作
     */
    LOGOUT("登出"),

    /**
     * 导出操作
     */
    EXPORT("导出"),

    /**
     * 导入操作
     */
    IMPORT("导入"),

    /**
     * 审批操作
     */
    APPROVE("审批"),

    /**
     * 其他操作
     */
    OTHER("其他");

    private final String description;

    OperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
