package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void shouldCreateWithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.PRIVACY_NOT_AGREED);
        assertEquals(ErrorCode.PRIVACY_NOT_AGREED.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.PRIVACY_NOT_AGREED.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.PRIVACY_NOT_AGREED.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldCreateWithThreeDifferentParams() {
        BusinessException ex = new BusinessException(ErrorCode.ACCOUNT_FROZEN);
        assertEquals(ErrorCode.ACCOUNT_FROZEN.getCode(), ex.getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_FROZEN.getMessage(), ex.getMessage());
        assertEquals(ErrorCode.ACCOUNT_FROZEN.getUserTip(), ex.getUserTip());
    }

    @Test
    void shouldBeRuntimeException() {
        BusinessException ex = new BusinessException(ErrorCode.PRIVACY_NOT_AGREED);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
