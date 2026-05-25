package com.mall.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void shouldCreateWithTwoArgConstructor() {
        BusinessException ex = new BusinessException("A0101", "未同意隐私协议");
        assertEquals("A0101", ex.getErrorCode());
        assertEquals("未同意隐私协议", ex.getMessage());
        assertEquals("未同意隐私协议", ex.getUserTip());
    }

    @Test
    void shouldCreateWithThreeArgConstructor() {
        BusinessException ex = new BusinessException("A0202", "账户已被冻结", "您的账户已被冻结，请联系客服");
        assertEquals("A0202", ex.getErrorCode());
        assertEquals("账户已被冻结", ex.getMessage());
        assertEquals("您的账户已被冻结，请联系客服", ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        BusinessException ex = new BusinessException("A0101", "未同意隐私协议");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
