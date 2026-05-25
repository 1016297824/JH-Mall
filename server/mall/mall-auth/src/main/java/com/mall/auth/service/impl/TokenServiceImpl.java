package com.mall.auth.service.impl;

import com.mall.auth.dto.response.TokenResponse;
import com.mall.auth.service.TokenService;
import com.mall.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    @Value("${mall.security.jwt-secret}")
    private String jwtSecret;

    @Value("${mall.auth.access-token-ttl:1800}")
    private long accessTtl;

    @Value("${mall.auth.refresh-token-ttl:604800}")
    private long refreshTtl;

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TokenResponse issue(String userId) {
        byte[] key = jwtSecret.getBytes(StandardCharsets.UTF_8);
        Date now = new Date();
        Date accessExp = new Date(now.getTime() + accessTtl * 1000);
        Date refreshExp = new Date(now.getTime() + refreshTtl * 1000);

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

        String sessionKey = "mall:auth:session:" + userId + ":" + accessJti;
        redisTemplate.opsForValue().set(sessionKey, "1", accessTtl, TimeUnit.SECONDS);

        String refreshKey = "mall:auth:refresh:" + refreshJti;
        redisTemplate.opsForValue().set(refreshKey, userId, refreshTtl, TimeUnit.SECONDS);

        return new TokenResponse(accessToken, refreshToken, accessTtl);
    }

    @Override
    public String verify(String accessToken) {
        Claims claims = parseToken(accessToken);

        String jti = claims.getId();
        String userId = claims.getSubject();

        String blacklistKey = "mall:auth:blacklist:" + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException("A0231", "token已被撤销");
        }

        String sessionKey = "mall:auth:session:" + userId + ":" + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
            throw new TokenException("A0231", "token会话不存在或已过期");
        }

        return userId;
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseToken(refreshToken);

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new TokenException("A0231", "refreshToken类型错误");
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        Date expiration = claims.getExpiration();

        String blacklistKey = "mall:auth:blacklist:" + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException("A0231", "refreshToken已被撤销");
        }

        String refreshMappingKey = "mall:auth:refresh:" + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(refreshMappingKey))) {
            throw new TokenException("A0231", "refreshToken已过期");
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

        String blacklistKey = "mall:auth:blacklist:" + jti;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);

        String sessionKey = "mall:auth:session:" + userId + ":" + jti;
        redisTemplate.delete(sessionKey);
    }

    @Override
    public void revokeAll(String userId) {
        String pattern = "mall:auth:session:" + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String sessionKey : keys) {
            String jti = sessionKey.substring(sessionKey.lastIndexOf(':') + 1);
            String blacklistKey = "mall:auth:blacklist:" + jti;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", accessTtl, TimeUnit.SECONDS);
            redisTemplate.delete(sessionKey);
        }
    }

    private Claims parseToken(String token) {
        try {
            byte[] key = jwtSecret.getBytes(StandardCharsets.UTF_8);
            return Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenException("A0231", "token无效或已过期");
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            throw new TokenException("A0231", "token无效或已过期");
        }
    }
}
