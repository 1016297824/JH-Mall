package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptchaExceptionTest {

    @Test
    void shouldCreateWithErrorCode() {
        CaptchaException ex = new CaptchaException(ErrorCode.CAPTCHA_VERIFY_ERROR);
        assertEquals(ErrorCode.CAPTCHA_VERIFY_ERROR.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.CAPTCHA_VERIFY_ERROR.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.CAPTCHA_VERIFY_ERROR.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldCreateWithExpiredCode() {
        CaptchaException ex = new CaptchaException(ErrorCode.CAPTCHA_EXPIRED);
        assertEquals(ErrorCode.CAPTCHA_EXPIRED.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.CAPTCHA_EXPIRED.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.CAPTCHA_EXPIRED.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        CaptchaException ex = new CaptchaException(ErrorCode.CAPTCHA_VERIFY_ERROR);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
