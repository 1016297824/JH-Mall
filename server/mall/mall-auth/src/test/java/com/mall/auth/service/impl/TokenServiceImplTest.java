package com.mall.auth.service.impl;

import com.mall.api.feign.RemoteUserService;
import com.mall.auth.DTO.response.TokenRespDTO;
import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.config.MallSecurityConfigProperties;
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

import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    @Mock
    private RemoteUserService remoteUserService;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private static final String JWT_SECRET = "7COWPc0I1OG/8Cby86JRsZhk6+kR3tNbKXgxwr45O1mPSZm1SqfRmXyekGo1UojKSEnjVDUSSI7a0HEVKLZcoQ==";
    private static final String USER_ID = "1234567890";

    @Mock
    private MallAuthConfigProperties authProperties;

    @Mock
    private MallSecurityConfigProperties securityProperties;

    @BeforeEach
    void setUp() {
        lenient().when(authProperties.getAccessTokenTtl()).thenReturn(1800L);
        lenient().when(authProperties.getRefreshTokenTtl()).thenReturn(604800L);
        lenient().when(authProperties.getTokenVersionCacheTtl()).thenReturn(2592000L);
        lenient().when(securityProperties.getJwtSecret()).thenReturn(JWT_SECRET);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // token_version 默认返回 1
        lenient().when(remoteUserService.getTokenVersion(anyString())).thenReturn(1);
    }

    @Test
    void shouldIssueTokenWithVerClaim() {
        TokenRespDTO response = tokenService.issue(USER_ID);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(1800L, response.getExpiresIn());
        assertNotEquals(response.getAccessToken(), response.getRefreshToken());
    }

    @Test
    void shouldVerifyValidTokenReturnUserId() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        // Redis 缓存命中，version=1
        when(valueOperations.get(contains("user_version:"))).thenReturn(1);

        TokenRespDTO response = tokenService.issue(USER_ID);
        String userId = tokenService.verify(response.getAccessToken());

        assertEquals(USER_ID, userId);
    }

    @Test
    void shouldVerifyThrowOnBlacklistedToken() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(true);

        TokenRespDTO response = tokenService.issue(USER_ID);

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
    void shouldVerifyThrowWhenVersionMismatch() {
        // 先签发 JWT（此时 Redis 缓存 miss，走 Feign getTokenVersion 返回 1，JWT 签 ver=1）
        TokenRespDTO response = tokenService.issue(USER_ID);

        // 再 mock Redis 缓存返回 version=99，JWT 中 ver=1，不匹配 → 应抛异常
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(valueOperations.get(contains("user_version:"))).thenReturn(99);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.verify(response.getAccessToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshReturnNewTokenPair() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("refresh"))).thenReturn(true);

        TokenRespDTO response = tokenService.issue(USER_ID);

        when(redisTemplate.delete(anyString())).thenReturn(true);

        TokenRespDTO refreshed = tokenService.refresh(response.getRefreshToken());

        assertNotNull(refreshed.getAccessToken());
        assertNotNull(refreshed.getRefreshToken());
        assertEquals(1800L, refreshed.getExpiresIn());
        assertNotEquals(response.getAccessToken(), refreshed.getAccessToken());
        assertNotEquals(response.getRefreshToken(), refreshed.getRefreshToken());
    }

    @Test
    void shouldRefreshThrowOnAccessToken() {
        TokenRespDTO response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getAccessToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshThrowOnRevokedToken() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(true);

        TokenRespDTO response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getRefreshToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRefreshThrowWhenRefreshMappingMissing() {
        when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(false);
        when(redisTemplate.hasKey(contains("refresh"))).thenReturn(false);

        TokenRespDTO response = tokenService.issue(USER_ID);

        TokenException exception = assertThrows(TokenException.class,
                () -> tokenService.refresh(response.getRefreshToken()));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRevokeSuccessfully() {
        TokenRespDTO response = tokenService.issue(USER_ID);

        tokenService.revoke(response.getAccessToken());

        // revoke 只写黑名单，不删 session
        verify(valueOperations).set(
                contains("blacklist"), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRevokeAllCallIncrementTokenVersion() {
        tokenService.revokeAll(USER_ID);

        // revokeAll 改为调 Feign incrementTokenVersion
        verify(remoteUserService).incrementTokenVersion(USER_ID);
    }
}
