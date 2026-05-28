package com.mall.api.infrastructure.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.HeaderConstants;
import com.mall.common.constant.SecurityConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 内部请求签名校验过滤器
 *
 * <p>拦截 /inner/** 路径，校验 {@code X-Internal-*} 签名头。</p>
 * <p>{@code @Component} 自动注册，各模块无需额外配置。</p>
 *
 * @author AI
 * @date 2026/05/28
 */
@Component
public class InnerSignatureFilter extends OncePerRequestFilter {

    @Value("${mall.security.internal-secret}")
    private String secret;

    @Value("${mall.security.internal-timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;

    private final StringRedisTemplate redisTemplate;

    public InnerSignatureFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains("/inner/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String timestamp = request.getHeader(HeaderConstants.X_INTERNAL_TIMESTAMP);
        String nonce = request.getHeader(HeaderConstants.X_INTERNAL_NONCE);
        String signature = request.getHeader(HeaderConstants.X_INTERNAL_SIGNATURE);

        if (timestamp == null || nonce == null || signature == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_MISSING);
        }

        validateTimestamp(timestamp);
        validateNonce(nonce);

        String method = request.getMethod();
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
        String body = method.equals("GET") || method.equals("DELETE") ? "" : wrapper.getBodyAsString();

        validateSignature(timestamp, nonce, body, signature);

        filterChain.doFilter(wrapper, response);
    }

    /**
     * 校验时间戳偏差（±5 分钟）
     */
    private void validateTimestamp(String timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long reqTime = Long.parseLong(timestamp);
        if (Math.abs(now - reqTime) > timestampToleranceSeconds) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * 校验 nonce 是否已使用（Redis SETNX 防重放）
     */
    private void validateNonce(String nonce) {
        String key = CacheConstants.Internal.NONCE + nonce;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * 校验签名
     */
    private void validateSignature(String timestamp, String nonce, String body, String expectedSignature) {
        String payload = timestamp + SecurityConstants.SIGN_PAYLOAD_SEPARATOR + nonce;
        if (!body.isEmpty()) {
            payload += SecurityConstants.SIGN_PAYLOAD_SEPARATOR + body;
        }

        String calculated = hmacSha256(payload);
        if (!calculated.equals(expectedSignature)) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * HMAC-SHA256 签名计算
     */
    private String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance(SecurityConstants.HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SecurityConstants.HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 签名计算失败", e);
        }
    }

    /**
     * 缓存请求体，使 InputStream 可重复读取
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        String getBodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
