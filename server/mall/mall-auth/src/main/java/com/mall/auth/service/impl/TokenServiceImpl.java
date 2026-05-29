package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.config.MallSecurityConfigProperties;
import com.mall.auth.DTO.response.TokenResponse;
import com.mall.auth.service.ITokenService;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务实现
 *
 * <p>使用 HS512 签名生成 JWT（accessToken + refreshToken），通过 Redis 维护会话（session Key）
 * 和黑名单（blacklist Key）。refreshToken 一次性使用，刷新时旧 refreshToken 立即加入黑名单。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements ITokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MallAuthConfigProperties authProperties;
    private final MallSecurityConfigProperties securityProperties;

    /**
     * 签发 Token（同时生成 accessToken 和 refreshToken）
     *
     * @param userId 用户 ID
     * @return Token 响应
     */
    @Override
    public TokenResponse issue(String userId) {
        byte[] key = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        Date now = new Date();
        Date accessExp = new Date(now.getTime() + authProperties.getAccessTokenTtl() * 1000);
        Date refreshExp = new Date(now.getTime() + authProperties.getRefreshTokenTtl() * 1000);

        // 为 accessToken 和 refreshToken 分别生成唯一 jti
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        // 构建 accessToken JWT（HS512 签名）
        String accessToken = Jwts.builder()
                .setId(accessJti)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(accessExp)
                .claim("type", "access")
                .claim("userId", userId)
                .setIssuer("mall-auth")
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();

        // 构建 refreshToken JWT（有效期更长，用于续期 accessToken）
        String refreshToken = Jwts.builder()
                .setId(refreshJti)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(refreshExp)
                .claim("type", "refresh")
                .claim("userId", userId)
                .setIssuer("mall-auth")
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();

        // accessToken 会话缓存，用于快速校验 token 有效性
        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + accessJti;
        redisTemplate.opsForValue().set(sessionKey, "1", authProperties.getAccessTokenTtl(), TimeUnit.SECONDS);

        // refreshToken 映射缓存，刷新时校验 refreshToken 有效性
        String refreshKey = CacheConstants.Auth.REFRESH + refreshJti;
        redisTemplate.opsForValue().set(refreshKey, userId, authProperties.getRefreshTokenTtl(), TimeUnit.SECONDS);

        return new TokenResponse(accessToken, refreshToken, authProperties.getAccessTokenTtl());
    }

    /**
     * 校验 accessToken 并返回 userId
     *
     * @param accessToken 访问令牌
     * @return 用户 ID
     */
    @Override
    public String verify(String accessToken) {
        Claims claims = parseToken(accessToken);

        String jti = claims.getId();
        String userId = claims.getSubject();

        // 检查黑名单：已注销/已刷新的 token 直接拒绝
        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        // 检查会话缓存：无 session 说明 token 未签发或已被删除
        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        return userId;
    }

    /**
     * 使用 refreshToken 刷新 Token（旧 refreshToken 一次性使用后立即失效）
     *
     * @param refreshToken 刷新令牌
     * @return 新的 Token 响应
     */
    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseToken(refreshToken);

        // 校验 token 类型必须为 refresh
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        Date expiration = claims.getExpiration();

        // 检查该 refreshToken 是否已被加入黑名单（已被使用）
        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        // 检查 Redis 中是否存在 refresh 映射记录
        String refreshMappingKey = CacheConstants.Auth.REFRESH + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(refreshMappingKey))) {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        // 旧 refreshToken 一次性使用：加入黑名单 + 删除映射，剩余有效期作为黑名单 TTL
        long remainingSeconds = Math.max(0,
                (expiration.getTime() - System.currentTimeMillis()) / 1000);
        redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(refreshMappingKey);

        // 重新签发全套 Token
        return issue(userId);
    }

    /**
     * 吊销单条 accessToken（加入黑名单）
     *
     * @param accessToken 访问令牌
     */
    @Override
    public void revoke(String accessToken) {
        Claims claims = parseToken(accessToken);

        String jti = claims.getId();
        String userId = claims.getSubject();
        Date expiration = claims.getExpiration();

        // 计算 token 剩余有效期作为黑名单 TTL
        long remainingSeconds = Math.max(0,
                (expiration.getTime() - System.currentTimeMillis()) / 1000);

        // 加入黑名单防止后续使用，同时删除会话缓存
        String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);

        String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
        redisTemplate.delete(sessionKey);
    }

    /**
     * 吊销用户所有 Token（通过 pattern 匹配 session Key）
     *
     * @param userId 用户 ID
     */
    @Override
    public void revokeAll(String userId) {
        // pattern 匹配该用户所有 session Key：mall:auth:session:{userId}:*
        String pattern = CacheConstants.Auth.SESSION + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // 遍历每个 session，提取 jti 并加入黑名单
        for (String sessionKey : keys) {
            String jti = sessionKey.substring(sessionKey.lastIndexOf(':') + 1);
            String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", authProperties.getAccessTokenTtl(), TimeUnit.SECONDS);
            redisTemplate.delete(sessionKey);
        }
    }

    /**
     * 解析并验证 JWT Token
     *
     * @param token JWT Token 字符串
     * @return JWT Claims
     */
    private Claims parseToken(String token) {
        try {
            byte[] key = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
            // 用 HS512 密钥解析并校验 JWT 签名
            return Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // token 过期也视为无效，统一返 TOKEN_INVALID
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            // 签名篡改 / 格式非法 / 参数异常
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }
    }
}
