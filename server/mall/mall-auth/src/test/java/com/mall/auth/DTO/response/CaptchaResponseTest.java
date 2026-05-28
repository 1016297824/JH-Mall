package com.mall.auth.DTO.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptchaResponseTest {

    @Test
    void shouldCreateWithNoArgConstructor() {
        CaptchaResponse response = new CaptchaResponse();
        assertNull(response.getCaptchaKey());
        assertNull(response.getCaptchaImage());
    }

    @Test
    void shouldCreateWithAllArgConstructor() {
        CaptchaResponse response = new CaptchaResponse("key-123", "image-base64-data");
        assertEquals("key-123", response.getCaptchaKey());
        assertEquals("image-base64-data", response.getCaptchaImage());
    }

    @Test
    void shouldSupportSetters() {
        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey("key-456");
        response.setCaptchaImage("image-data");
        assertEquals("key-456", response.getCaptchaKey());
        assertEquals("image-data", response.getCaptchaImage());
    }
}
