package com.mall.auth.DTO.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenResponseTest {

    @Test
    void shouldCreateWithNoArgConstructor() {
        TokenRespDTO response = new TokenRespDTO();
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
        assertEquals(0, response.getExpiresIn());
    }

    @Test
    void shouldCreateWithAllArgConstructor() {
        TokenRespDTO response = new TokenRespDTO("access-token", "refresh-token", 3600L);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(3600L, response.getExpiresIn());
    }

    @Test
    void shouldSupportSetters() {
        TokenRespDTO response = new TokenRespDTO();
        response.setAccessToken("new-access");
        response.setRefreshToken("new-refresh");
        response.setExpiresIn(7200L);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals(7200L, response.getExpiresIn());
    }
}
