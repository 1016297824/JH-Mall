package com.mall.api.infrastructure.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 内部请求签名校验过滤器
 *
 * <p>拦截 /inner/** 路径，校验 {@code X-Internal-*} 签名头。</p>
 * <p>{@code @Component} 自动注册，各模块无需额外配置。</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
public class InnerSignatureFilter extends OncePerRequestFilter {

    /** 内部签名密钥 */
    @Value("${mall.security.internal-secret}")
    private String secret;

    /** 时间戳容差秒数 */
    @Value("${mall.security.internal-timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造注入
     *
     * @param redisTemplate Redis 操作模板
     */
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
        // 提取签名头
        String timestamp = request.getHeader(HeaderConstants.X_INTERNAL_TIMESTAMP);
        String nonce = request.getHeader(HeaderConstants.X_INTERNAL_NONCE);
        String signature = request.getHeader(HeaderConstants.X_INTERNAL_SIGNATURE);

        // 签名头缺失直接拒绝
        if (timestamp == null || nonce == null || signature == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_MISSING);
        }

        // 校验时间戳偏差与 nonce 防重放
        validateTimestamp(timestamp);
        validateNonce(nonce);

        // GET/DELETE 无请求体，不参与签名计算
        String method = request.getMethod();
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
        String body = HttpMethod.GET.name().equals(method) || HttpMethod.DELETE.name().equals(method)
                ? "" : wrapper.getBodyAsString();

        // 校验 HMAC-SHA256 签名
        validateSignature(timestamp, nonce, body, signature);

        filterChain.doFilter(wrapper, response);
    }

    /**
     * 校验时间戳偏差
     *
     * @param timestamp 请求头中的 Unix 秒级时间戳
     */
    private void validateTimestamp(String timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long reqTime = Long.parseLong(timestamp);
        // 时间戳偏差超过配置容差（默认 300s）视为无效
        if (Math.abs(now - reqTime) > timestampToleranceSeconds) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * 校验 nonce 是否已使用（Redis SETNX 防重放）
     *
     * @param nonce 请求头中的一次性随机数
     */
    private void validateNonce(String nonce) {
        // Redis key: mall:internal:nonce:<nonce>
        String key = CacheConstants.Internal.NONCE + nonce;
        // SETNX 原子写入，key 已存在说明该 nonce 已被使用（重放攻击）
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, CacheConstants.Internal.NONCE_VALUE, Duration.ofSeconds(timestampToleranceSeconds));
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * 校验签名
     *
     * @param timestamp         时间戳
     * @param nonce             随机数
     * @param body              请求体
     * @param expectedSignature 期望的签名
     */
    private void validateSignature(String timestamp, String nonce, String body, String expectedSignature) {
        // 签名载荷：timestamp + separator + nonce [+ separator + body]
        String payload = timestamp + SecurityConstants.SIGN_PAYLOAD_SEPARATOR + nonce;
        if (!body.isEmpty()) {
            payload += SecurityConstants.SIGN_PAYLOAD_SEPARATOR + body;
        }

        String calculated = HmacUtils.sign(payload, secret);
        if (!calculated.equals(expectedSignature)) {
            throw new BusinessException(ErrorCode.INTERNAL_SIGN_INVALID);
        }
    }

    /**
     * 缓存请求体，使 InputStream 可重复读取
     *
     * @author JH-Mall
     * @date 2026/05/28
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        /** 缓存的请求体字节 */
        private final byte[] body;

        /**
         * 构造时一次性读取请求体
         *
         * @param request 原始请求
         * @throws IOException 读取失败
         */
        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        /**
         * 将缓存的字节转为字符串
         *
         * @return 请求体字符串
         */
        String getBodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            // 用缓存字节包装可重复读取的输入流
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
                    // 纯内存流，无需异步监听
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
