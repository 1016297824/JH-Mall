package com.mall.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenExceptionTest {

    @Test
    void shouldCreateWithTwoArgConstructor() {
        TokenException ex = new TokenException("A0201", "Token无效");
        assertEquals("A0201", ex.getErrorCode());
        assertEquals("Token无效", ex.getMessage());
        assertEquals("Token无效", ex.getUserTip());
    }

    @Test
    void shouldCreateWithThreeArgConstructor() {
        TokenException ex = new TokenException("A0201", "Token已过期", "请重新登录");
        assertEquals("A0201", ex.getErrorCode());
        assertEquals("Token已过期", ex.getMessage());
        assertEquals("请重新登录", ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        TokenException ex = new TokenException("A0201", "Token无效");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
