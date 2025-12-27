package com.his.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
}
