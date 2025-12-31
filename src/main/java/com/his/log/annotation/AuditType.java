package com.his.log.annotation;

/**
 * 审计类型枚举
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
public enum AuditType {

    /**
     * 业务操作
     * 例如：创建患者、开具处方
     */
    BUSINESS("业务操作"),

    /**
     * 数据访问
     * 例如：查询患者详情、导出数据
     */
    DATA_ACCESS("数据访问"),

    /**
     * 权限变更
     * 例如：分配角色、修改权限
     */
    PERMISSION_CHANGE("权限变更"),

    /**
     * 系统配置
     * 例如：修改系统参数、更新字典
     */
    SYSTEM_CONFIG("系统配置"),

    /**
     * 敏感操作
     * 例如：查看完整身份证号、导出敏感数据
     */
    SENSITIVE_OPERATION("敏感操作");

    private final String description;

    AuditType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
