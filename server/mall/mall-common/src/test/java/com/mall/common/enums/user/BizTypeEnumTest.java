package com.mall.common.enums.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizTypeEnum 单元测试
 *
 * @author system
 * @date 2026/05/28
 */
class BizTypeEnumTest {

    @Test
    void shouldHaveSixEnumValues() {
        BizTypeEnum[] values = BizTypeEnum.values();
        assertEquals(6, values.length);
    }

    @Test
    void orderEnumShouldHaveCorrectCodeAndName() {
        assertEquals("order", BizTypeEnum.ORDER.getCode());
        assertEquals("下单赠送", BizTypeEnum.ORDER.getName());
    }

    @Test
    void signInEnumShouldHaveCorrectCodeAndName() {
        assertEquals("signin", BizTypeEnum.SIGN_IN.getCode());
        assertEquals("签到", BizTypeEnum.SIGN_IN.getName());
    }

    @Test
    void reviewEnumShouldHaveCorrectCodeAndName() {
        assertEquals("review", BizTypeEnum.REVIEW.getCode());
        assertEquals("评价", BizTypeEnum.REVIEW.getName());
    }

    @Test
    void refundEnumShouldHaveCorrectCodeAndName() {
        assertEquals("refund", BizTypeEnum.REFUND.getCode());
        assertEquals("退款扣除", BizTypeEnum.REFUND.getName());
    }

    @Test
    void adminEnumShouldHaveCorrectCodeAndName() {
        assertEquals("admin", BizTypeEnum.ADMIN.getCode());
        assertEquals("管理员调整", BizTypeEnum.ADMIN.getName());
    }

    @Test
    void expireEnumShouldHaveCorrectCodeAndName() {
        assertEquals("expire", BizTypeEnum.EXPIRE.getCode());
        assertEquals("积分过期", BizTypeEnum.EXPIRE.getName());
    }

    @Test
    void fromCodeShouldReturnOrderWhenCodeIsOrder() {
        BizTypeEnum result = BizTypeEnum.fromCode("order");
        assertEquals(BizTypeEnum.ORDER, result);
    }

    @Test
    void fromCodeShouldReturnNullWhenCodeIsUnknown() {
        BizTypeEnum result = BizTypeEnum.fromCode("unknown");
        assertNull(result);
    }
}
