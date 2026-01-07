package com.his.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 枚举测试类
 *
 * <p>验证所有枚举类的正确性，包括新增的枚举类。</p>
 */
class EnumTest {

    @Test
    void testPrescriptionStatusEnum() {
        assertEquals("草稿", PrescriptionStatusEnum.DRAFT.getDescription());
        assertEquals((short) 5, PrescriptionStatusEnum.PAID.getCode());
        assertEquals(PrescriptionStatusEnum.PAID, PrescriptionStatusEnum.fromCode((short) 5));

        assertThrows(IllegalArgumentException.class, () -> PrescriptionStatusEnum.fromCode((short) 99));
    }

    @Test
    void testChargeStatusEnum() {
        assertEquals("已缴费", ChargeStatusEnum.PAID.getDescription());
        assertEquals((short) 1, ChargeStatusEnum.PAID.getCode());
        assertEquals(ChargeStatusEnum.PAID, ChargeStatusEnum.fromCode((short) 1));
    }

    @Test
    void testPaymentMethodEnum() {
        assertEquals("微信", PaymentMethodEnum.WECHAT.getDescription());
        assertEquals((short) 3, PaymentMethodEnum.WECHAT.getCode());
        assertEquals(PaymentMethodEnum.WECHAT, PaymentMethodEnum.fromCode((short) 3));
        assertEquals(PaymentMethodEnum.ALIPAY, PaymentMethodEnum.fromCode((short) 4));
        assertEquals(PaymentMethodEnum.INSURANCE, PaymentMethodEnum.fromCode((short) 5));
    }

    /**
     * 测试新增的ChargeTypeEnum枚举
     *
     * <p>新增时间：2026-01-07</p>
     * <p>用于支持灵活的收费模式（仅挂号费、仅处方费、混合收费）</p>
     */
    @Test
    void testChargeTypeEnum() {
        // 测试枚举值
        assertEquals("仅挂号费", ChargeTypeEnum.REGISTRATION_ONLY.getDescription());
        assertEquals("仅处方费", ChargeTypeEnum.PRESCRIPTION_ONLY.getDescription());
        assertEquals("混合收费", ChargeTypeEnum.MIXED.getDescription());

        // 测试代码值
        assertEquals((short) 1, ChargeTypeEnum.REGISTRATION_ONLY.getCode());
        assertEquals((short) 2, ChargeTypeEnum.PRESCRIPTION_ONLY.getCode());
        assertEquals((short) 3, ChargeTypeEnum.MIXED.getCode());

        // 测试fromCode方法
        assertEquals(ChargeTypeEnum.REGISTRATION_ONLY, ChargeTypeEnum.fromCode((short) 1));
        assertEquals(ChargeTypeEnum.PRESCRIPTION_ONLY, ChargeTypeEnum.fromCode((short) 2));
        assertEquals(ChargeTypeEnum.MIXED, ChargeTypeEnum.fromCode((short) 3));

        // 测试无效代码
        assertThrows(IllegalArgumentException.class, () -> ChargeTypeEnum.fromCode((short) 99));
    }

    /**
     * 测试新增的MedicalRecordStatusEnum枚举
     *
     * <p>新增时间：2026-01-07</p>
     * <p>用于控制病历的编辑权限和工作流（草稿、已提交、已审核）</p>
     */
    @Test
    void testMedicalRecordStatusEnum() {
        // 测试枚举值
        assertEquals("草稿", MedicalRecordStatusEnum.DRAFT.getDescription());
        assertEquals("已提交", MedicalRecordStatusEnum.SUBMITTED.getDescription());
        assertEquals("已审核", MedicalRecordStatusEnum.AUDITED.getDescription());

        // 测试代码值
        assertEquals((short) 0, MedicalRecordStatusEnum.DRAFT.getCode());
        assertEquals((short) 1, MedicalRecordStatusEnum.SUBMITTED.getCode());
        assertEquals((short) 2, MedicalRecordStatusEnum.AUDITED.getCode());

        // 测试fromCode方法
        assertEquals(MedicalRecordStatusEnum.DRAFT, MedicalRecordStatusEnum.fromCode((short) 0));
        assertEquals(MedicalRecordStatusEnum.SUBMITTED, MedicalRecordStatusEnum.fromCode((short) 1));
        assertEquals(MedicalRecordStatusEnum.AUDITED, MedicalRecordStatusEnum.fromCode((short) 2));

        // 测试无效代码
        assertThrows(IllegalArgumentException.class, () -> MedicalRecordStatusEnum.fromCode((short) 99));
    }

    /**
     * 测试新增的PrescriptionTypeEnum枚举
     *
     * <p>新增时间：2026-01-07</p>
     * <p>用于区分西药和中药处方（不同的管理系统）</p>
     */
    @Test
    void testPrescriptionTypeEnum() {
        // 测试枚举值
        assertEquals("西药处方", PrescriptionTypeEnum.WESTERN_MEDICINE.getDescription());
        assertEquals("中药处方", PrescriptionTypeEnum.CHINESE_MEDICINE.getDescription());

        // 测试代码值
        assertEquals((short) 1, PrescriptionTypeEnum.WESTERN_MEDICINE.getCode());
        assertEquals((short) 2, PrescriptionTypeEnum.CHINESE_MEDICINE.getCode());

        // 测试fromCode方法
        assertEquals(PrescriptionTypeEnum.WESTERN_MEDICINE, PrescriptionTypeEnum.fromCode((short) 1));
        assertEquals(PrescriptionTypeEnum.CHINESE_MEDICINE, PrescriptionTypeEnum.fromCode((short) 2));

        // 测试无效代码
        assertThrows(IllegalArgumentException.class, () -> PrescriptionTypeEnum.fromCode((short) 99));
    }
}
