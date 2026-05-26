package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenExceptionTest {

    @Test
    void shouldCreateWithErrorCode() {
        TokenException ex = new TokenException(ErrorCode.TOKEN_INVALID);
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.TOKEN_INVALID.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.TOKEN_INVALID.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldCreateWithExpiredToken() {
        TokenException ex = new TokenException(ErrorCode.TOKEN_INVALID);
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.TOKEN_INVALID.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.TOKEN_INVALID.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        TokenException ex = new TokenException(ErrorCode.TOKEN_INVALID);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
