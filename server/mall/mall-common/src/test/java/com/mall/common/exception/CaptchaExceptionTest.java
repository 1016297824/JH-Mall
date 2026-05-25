package com.mall.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptchaExceptionTest {

    @Test
    void shouldCreateWithTwoArgConstructor() {
        CaptchaException ex = new CaptchaException("A0001", "验证码错误");
        assertEquals("A0001", ex.getErrorCode());
        assertEquals("验证码错误", ex.getMessage());
        assertEquals("验证码错误", ex.getUserTip());
    }

    @Test
    void shouldCreateWithThreeArgConstructor() {
        CaptchaException ex = new CaptchaException("A0001", "验证码已过期", "请重新获取验证码");
        assertEquals("A0001", ex.getErrorCode());
        assertEquals("验证码已过期", ex.getMessage());
        assertEquals("请重新获取验证码", ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        CaptchaException ex = new CaptchaException("A0001", "验证码错误");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
