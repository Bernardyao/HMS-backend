package com.his.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据脱敏工具类
 *
 * <p>提供各种敏感数据的脱敏处理方法
 *
 * <h3>支持的数据类型</h3>
 * <ul>
 *   <li>手机号：保留前3位和后4位</li>
 *   <li>身份证号：保留前6位和后4位</li>
 *   <li>地址：保留省市区，详细地址用*替换</li>
 *   <li>银行卡号：保留前4位和后4位</li>
 *   <li>姓名：只保留姓氏</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 手机号脱敏
 * String masked = DataMaskingUtils.maskPhone("13800138000");
 * // 结果：138****8000
 *
 * // 身份证脱敏
 * String masked = DataMaskingUtils.maskIdCard("110101199001011234");
 * // 结果：110101********1234
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @see SensitiveData
 * @see SensitiveType
 */
@Slf4j
public class DataMaskingUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private DataMaskingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 手机号脱敏
     *
     * <p>规则：保留前3位和后4位，中间用*替换
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号，如果输入为null或格式不正确则返回原值
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    /**
     * 身份证号脱敏
     *
     * <p>规则：保留前6位和后4位，中间用*替换
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号，如果输入为null或长度不足15位则返回原值
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) {
            return idCard;
        }
        int length = idCard.length();
        int maskLength = length - 10;
        return idCard.substring(0, 6) + "*".repeat(maskLength) + idCard.substring(length - 4);
    }

    /**
     * 地址脱敏
     *
     * <p>规则：保留省市区，详细地址用***替换
     *
     * @param address 原始地址
     * @return 脱敏后的地址，如果输入为null或长度小于6则返回原值
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() < 6) {
            return address;
        }
        // 保留前6个字符（通常是省市区），其余替换为***
        int keepLength = Math.min(6, address.length());
        return address.substring(0, keepLength) + "***";
    }

    /**
     * 银行卡号脱敏
     *
     * <p>规则：保留前4位和后4位，中间用*替换
     *
     * @param bankCard 原始银行卡号
     * @return 脱敏后的银行卡号，如果输入为null或长度小于8则返回原值
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        int length = bankCard.length();
        int maskLength = length - 8;
        return bankCard.substring(0, 4) + "*".repeat(maskLength) + bankCard.substring(length - 4);
    }

    /**
     * 姓名脱敏
     *
     * <p>规则：只保留姓氏，其余用*替换
     * <ul>
     *   <li>单姓（张三）：张*</li>
     *   <li>复姓（欧阳娜娜）：欧阳**</li>
     *   <li>单字名（李明）：李*</li>
     *   <li>超过3个字：保留前1-2位，其余用*替换</li>
     * </ul>
     *
     * @param name 原始姓名
     * @return 脱敏后的姓名，如果输入为null或长度小于2则返回原值
     */
    public static String maskName(String name) {
        if (name == null || name.length() < 2) {
            return name;
        }

        int length = name.length();
        if (length == 2) {
            // 单姓单名：张三 → 张*
            return name.charAt(0) + "*";
        } else if (length == 3) {
            // 可能是复姓或双字名：张小三 → 张**
            // 也可能是复姓：欧阳娜 → 欧**
            return name.substring(0, 1) + "**";
        } else {
            // 长姓名：保留前2位，其余用*替换
            return name.substring(0, 2) + "*".repeat(length - 2);
        }
    }

    /**
     * 邮箱脱敏
     *
     * <p>规则：保留前2个字符和@后的域名，中间用*替换
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱，如果输入为null或格式不正确则返回原值
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf('@');
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);

        // 保留前2个字符，其余用*替换
        if (prefix.length() <= 2) {
            return prefix + "***" + suffix;
        }

        return prefix.substring(0, 2) + "*".repeat(prefix.length() - 2) + suffix;
    }

    /**
     * 根据敏感类型进行脱敏
     *
     * @param data 原始数据
     * @param type 脱敏类型
     * @return 脱敏后的数据
     */
    public static String mask(String data, SensitiveType type) {
        if (data == null) {
            return null;
        }

        switch (type) {
            case PHONE:
                return maskPhone(data);
            case ID_CARD:
                return maskIdCard(data);
            case ADDRESS:
                return maskAddress(data);
            case BANK_CARD:
                return maskBankCard(data);
            case NAME:
                return maskName(data);
            case EMAIL:
                return maskEmail(data);
            default:
                log.warn("未知的敏感数据类型: {}", type);
                return data;
        }
    }
}
