package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.dto.response.TokenResponse;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.TokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private static final String JWT_SECRET = "7COWPc0I1OG/8Cby86JRsZhk6+kR3tNbKXgxwr45O1mPSZm1SqfRmXyekGo1UojKSEnjVDUSSI7a0HEVKLZcoQ==";
    private static final String USER_ID = "1234567890";

    @Mock
    private MallAuthConfigProperties authProperties;

    @BeforeEach
    void setUp() {
        lenient().when(authProperties.getAccessTokenTtl()).thenReturn(1800L);
        lenient().when(authProperties.getRefreshTokenTtl()).thenReturn(604800L);
        ReflectionTestUtils.setField(tokenService, "jwtSecret", JWT_SECRET);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldIssueTokenWithCompleteFields() {
        TokenResponse response = tokenService.issue(USER_ID);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(1800L, response.getExpiresIn());
        assertNotEquals(response.getAccessToken(), response.getRefreshToken());
    }

    @Test
    void shouldVerifyValidTokenReturnUserId() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("session"))).thenReturn(true);

        TokenResponse response = tokenService.issue(USER_ID);
        String userId = tokenService.verify(response.getAccessToken());

        assertEquals(USER_ID, userId);
    }

    @Test
    void shouldVerifyThrowOnBlacklistedToken() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(true);

        TokenResponse response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.verify(response.getAccessToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldVerifyThrowOnExpiredToken() {
        byte[] key = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        Date pastDate = new Date(System.currentTimeMillis() - 3600000L);
        String expiredToken = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(USER_ID)
                .setIssuedAt(new Date(pastDate.getTime() - 3600000L))
                .setExpiration(pastDate)
                .claim("type", "access")
                .setIssuer("mall-auth")
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.verify(expiredToken));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldVerifyThrowWhenSessionMissing() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("session"))).thenReturn(false);

        TokenResponse response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.verify(response.getAccessToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshReturnNewTokenPair() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("refresh"))).thenReturn(true);

        TokenResponse response = tokenService.issue(USER_ID);

        when(redisTemplate.delete(anyString())).thenReturn(true);

        TokenResponse refreshed = tokenService.refresh(response.getRefreshToken());

        assertNotNull(refreshed.getAccessToken());
        assertNotNull(refreshed.getRefreshToken());
        assertEquals(1800L, refreshed.getExpiresIn());
        assertNotEquals(response.getAccessToken(), refreshed.getAccessToken());
        assertNotEquals(response.getRefreshToken(), refreshed.getRefreshToken());
    }

    @Test
    void shouldRefreshThrowOnAccessToken() {
        TokenResponse response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getAccessToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshThrowOnRevokedToken() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(true);

        TokenResponse response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getRefreshToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshThrowWhenRefreshMappingMissing() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("refresh"))).thenReturn(false);

        TokenResponse response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getRefreshToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRevokeSuccessfully() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        TokenResponse response = tokenService.issue(USER_ID);

        tokenService.revoke(response.getAccessToken());

        verify(valueOperations).set(
                contains("blacklist"), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete(contains("session"));
    }

    @Test
    void shouldRevokeAllDeleteSessions() {
        Set<String> sessionKeys = new HashSet<>();
        sessionKeys.add("mall:auth:session:" + USER_ID + ":jti-1");
        sessionKeys.add("mall:auth:session:" + USER_ID + ":jti-2");

        when(redisTemplate.keys(eq("mall:auth:session:" + USER_ID + ":*")))
                .thenReturn(sessionKeys);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        tokenService.revokeAll(USER_ID);

        verify(redisTemplate).keys(eq("mall:auth:session:" + USER_ID + ":*"));
        verify(redisTemplate, times(2)).delete(contains("mall:auth:session:"));
        verify(valueOperations, times(2)).set(
                contains("blacklist"), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }
}
