package com.his.exception;

import lombok.Getter;

/**
 * 业务异常枚举
 * 统一管理业务错误码和错误信息
 *
 * @author HIS Team
 * @since 2025-12-27
 */
@Getter
public enum BusinessException {

    // 通用错误 1000-1999
    COMMON_INVALID_PARAM(1000, "参数错误"),
    COMMON_NOT_FOUND(1001, "资源不存在"),
    COMMON_ALREADY_EXISTS(1002, "资源已存在"),
    COMMON_OPERATION_NOT_ALLOWED(1003, "不允许的操作"),

    // 患者相关错误 2000-2999
    PATIENT_NOT_FOUND(2000, "患者信息不存在"),
    PATIENT_ALREADY_EXISTS(2001, "患者已存在"),
    PATIENT_ID_INVALID(2002, "患者ID必须大于0"),

    // 挂号相关错误 3000-3999
    REGISTRATION_NOT_FOUND(3000, "挂号记录不存在"),
    REGISTRATION_ALREADY_CANCELLED(3001, "该挂号已取消"),
    REGISTRATION_ALREADY_COMPLETED(3002, "该挂号已完成就诊"),
    REGISTRATION_STATUS_ERROR(3003, "挂号状态错误，无法执行此操作"),
    REGISTRATION_DOCTOR_MISMATCH(3004, "无权操作其他医生的挂号"),

    // 病历相关错误 4000-4999
    MEDICAL_RECORD_NOT_FOUND(4000, "病历记录不存在"),
    MEDICAL_RECORD_ALREADY_SUBMITTED(4001, "病历已提交，无法修改"),
    MEDICAL_RECORD_STATUS_ERROR(4002, "病历状态错误，无法执行此操作"),
    MEDICAL_RECORD_ALREADY_EXISTS(4003, "该挂号单已存在病历"),

    // 处方相关错误 5000-5999
    PRESCRIPTION_NOT_FOUND(5000, "处方记录不存在"),
    PRESCRIPTION_STATUS_ERROR(5001, "处方状态错误"),
    PRESCRIPTION_ALREADY_DISPENSED(5002, "处方已发药，无法退药"),
    PRESCRIPTION_NOT_PAID(5003, "处方未缴费，无法发药"),
    PRESCRIPTION_INVALID_STATUS(5004, "处方状态不正确"),

    // 发药相关错误 6000-6999
    DISPENSE_PRESCRIPTION_NOT_APPROVED(6000, "只能对已审核状态的处方进行发药操作"),
    DISPENSE_INSUFFICIENT_STOCK(6001, "药品库存不足，无法发药"),
    DISPENSE_ALREADY_COMPLETED(6002, "该处方已发药"),

    // 退药相关错误 7000-7999
    RETURN_PRESCRIPTION_NOT_DISPENSED(7000, "只能对已发药状态的处方进行退药操作"),
    RETURN_ALREADY_RETURNED(7001, "该处方已退药"),

    // 药品相关错误 8000-8999
    MEDICINE_NOT_FOUND(8000, "药品信息不存在"),
    MEDICINE_ALREADY_EXISTS(8001, "药品编码已存在"),
    MEDICINE_STOCK_INSUFFICIENT(8002, "药品库存不足"),
    MEDICINE_ID_INVALID(8003, "药品ID必须大于0"),

    // 医生相关错误 9000-9999
    DOCTOR_NOT_FOUND(9000, "医生信息不存在"),
    DOCTOR_ALREADY_EXISTS(9001, "医生编码已存在"),

    // 科室相关错误 10000-10999
    DEPARTMENT_NOT_FOUND(10000, "科室信息不存在"),
    DEPARTMENT_ALREADY_EXISTS(10001, "科室编码已存在"),

    // 认证授权错误 11000-11999
    AUTH_TOKEN_INVALID(11000, "认证失败，请重新登录"),
    AUTH_TOKEN_EXPIRED(11001, "登录已过期，请重新登录"),
    AUTH_UNAUTHORIZED(11002, "未授权访问"),
    AUTH_FORBIDDEN(11003, "无权访问"),

    // 收费相关错误 12000-12999
    CHARGE_NOT_FOUND(12000, "收费记录不存在"),
    CHARGE_ALREADY_PAID(12001, "收费单已支付，请勿重复支付"),
    CHARGE_ALREADY_REFUNDED(12002, "收费单已退费"),
    CHARGE_STATUS_ERROR(12003, "收费单状态错误，无法执行此操作"),
    CHARGE_AMOUNT_MISMATCH(12004, "支付金额与应收金额不一致"),
    CHARGE_DUPLICATE_TRANSACTION(12005, "交易流水号已存在，请勿重复支付"),

    // 挂号收费错误 12100-12199
    CHARGE_REGISTRATION_NOT_FOUND(12100, "挂号记录不存在"),
    CHARGE_REGISTRATION_STATUS_ERROR(12101, "挂号状态错误，无法收费"),
    CHARGE_REGISTRATION_ALREADY_PAID(12102, "挂号费已支付，请勿重复收费"),
    CHARGE_REGISTRATION_FEE_ZERO(12103, "挂号费金额必须大于0"),

    // 处方收费错误 12200-12299
    CHARGE_PRESCRIPTION_NOT_FOUND(12200, "处方记录不存在"),
    CHARGE_PRESCRIPTION_STATUS_ERROR(12201, "处方状态错误，只有已审核的处方才能收费"),
    CHARGE_PRESCRIPTION_ALREADY_PAID(12202, "处方费已支付，请勿重复收费"),
    CHARGE_PRESCRIPTION_REGISTRATION_NOT_COMPLETED(12203, "挂号未完成就诊，无法收取处方费"),

    // 退费错误 12300-12399
    REFUND_CHARGE_NOT_PAID(12300, "只有已支付的收费单才能退费"),
    REFUND_PRESCRIPTION_ALREADY_DISPENSED(12301, "处方已发药，无法退费"),
    REFUND_AMOUNT_ERROR(12302, "退费金额错误"),

    // 数据库约束违例错误 12400-12499
    CONSTRAINT_VIOLATION(12400, "数据完整性约束违例"),
    DUPLICATE_REGISTRATION(12401, "患者当天已挂该医生号，请勿重复挂号"),
    DUPLICATE_TRANSACTION_NO(12402, "交易号已存在，请勿重复提交"),
    FOREIGN_KEY_VIOLATION(12403, "关联数据不存在"),
    NOT_NULL_VIOLATION(12404, "必填字段不能为空");

    private final int code;
    private final String message;

    BusinessException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 格式化错误信息
     *
     * @param args 格式化参数
     * @return 格式化后的错误信息
     */
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
