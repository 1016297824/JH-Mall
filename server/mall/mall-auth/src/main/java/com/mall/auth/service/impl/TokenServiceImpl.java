package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.config.MallSecurityConfigProperties;
import com.mall.auth.dto.response.TokenResponse;
import com.mall.auth.service.TokenService;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MallAuthConfigProperties authProperties;
    private final MallSecurityConfigProperties securityProperties;

    public TokenServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            MallAuthConfigProperties authProperties,
                            MallSecurityConfigProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
        this.securityProperties = securityProperties;
    }

    @Override
    public TokenResponse issue(String userId) {
        byte[] key = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        Date now = new Date();
        Date accessExp = new Date(now.getTime() + authProperties.getAccessTokenTtl() * 1000);
        Date refreshExp = new Date(now.getTime() + authProperties.getRefreshTokenTtl() * 1000);

        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = Jwts.builder()
                .setId(accessJti)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(accessExp)
                .claim("type", "access")
                .setIssuer("mall-auth")
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();

        String refreshToken = Jwts.builder()
                .setId(refreshJti)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(refreshExp)
                .claim("type", "refresh")
                .setIssuer("mall-auth")
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();

        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + accessJti;
        redisTemplate.opsForValue().set(sessionKey, "1", authProperties.getAccessTokenTtl(), TimeUnit.SECONDS);

        String refreshKey = CacheConstants.Auth.REFRESH + refreshJti;
        redisTemplate.opsForValue().set(refreshKey, userId, authProperties.getRefreshTokenTtl(), TimeUnit.SECONDS);

        return new TokenResponse(accessToken, refreshToken, authProperties.getAccessTokenTtl());
    }

    @Override
    public String verify(String accessToken) {
        Claims claims = parseToken(accessToken);

        String jti = claims.getId();
        String userId = claims.getSubject();

        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        return userId;
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseToken(refreshToken);

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        Date expiration = claims.getExpiration();

        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        String refreshMappingKey = CacheConstants.Auth.REFRESH + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(refreshMappingKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        long remainingSeconds = Math.max(0,
                (expiration.getTime() - System.currentTimeMillis()) / 1000);
        redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(refreshMappingKey);

        return issue(userId);
    }

    @Override
    public void revoke(String accessToken) {
        Claims claims = parseToken(accessToken);

        String jti = claims.getId();
        String userId = claims.getSubject();
        Date expiration = claims.getExpiration();

        long remainingSeconds = Math.max(0,
                (expiration.getTime() - System.currentTimeMillis()) / 1000);

        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);

        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
        redisTemplate.delete(sessionKey);
    }

    @Override
    public void revokeAll(String userId) {
        String pattern = CacheConstants.Auth.SESSION + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String sessionKey : keys) {
            String jti = sessionKey.substring(sessionKey.lastIndexOf(':') + 1);
            String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", authProperties.getAccessTokenTtl(), TimeUnit.SECONDS);
            redisTemplate.delete(sessionKey);
        }
    }

    private Claims parseToken(String token) {
        try {
            byte[] key = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
            return Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }
    }
}
