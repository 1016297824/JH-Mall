package com.mall.user.infrastructure.feign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RemoteAuthAdapter 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
class RemoteAuthAdapterTest {

    private final RemoteAuthAdapter adapter = new RemoteAuthAdapter();

    @Test
    void maskPhoneShouldMaskMiddleFourDigits() {
        String result = adapter.maskPhone("13800138000");
        assertEquals("138****8000", result);
    }

    @Test
    void maskPhoneShouldReturnNullWhenInputIsNull() {
        String result = adapter.maskPhone(null);
        assertNull(result);
    }

    @Test
    void maskPhoneShouldReturnOriginalWhenLengthLessThanSeven() {
        assertEquals("12345", adapter.maskPhone("12345"));
    }

    @Test
    void maskPhoneShouldMaskCorrectlyForSevenDigitPhone() {
        assertEquals("138****3456", adapter.maskPhone("1383456"));
    }

    @Test
    void decryptPhoneShouldReturnOriginalValue() {
        assertEquals("encrypted123", adapter.decryptPhone("encrypted123"));
    }
}
