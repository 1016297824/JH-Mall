package com.mall.api.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

import com.mall.common.constant.HeaderConstants;
import com.mall.common.constant.SecurityConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feign 内部请求签名拦截器
 *
 * <p>引入 mall-api 的模块自动生效，为所有 Feign 调用注入 HMAC-SHA256 签名头。</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
public class InnerFeignRequestInterceptor implements RequestInterceptor {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 内部签名密钥 */
    @Value("${mall.security.internal-secret}")
    private String secret;

    /**
     * 为 Feign 请求注入 HMAC-SHA256 内部签名头
     *
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        // 生成秒级 Unix 时间戳与随机 nonce
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();
        // 读取 Feign 请求体（JSON 字符串）
        String body = template.body() != null ? new String(template.body(), StandardCharsets.UTF_8) : "";

        // 拼接签名载荷：timestamp + separator + nonce [+ separator + body]
        String payload = timestamp + SecurityConstants.SIGN_PAYLOAD_SEPARATOR + nonce;
        if (!body.isEmpty()) {
            payload += SecurityConstants.SIGN_PAYLOAD_SEPARATOR + body;
        }

        // 计算 HMAC-SHA256 签名
        String signature = HmacUtils.sign(payload, secret);

        // 注入三个签名头
        template.header(HeaderConstants.X_INTERNAL_TIMESTAMP, timestamp);
        template.header(HeaderConstants.X_INTERNAL_NONCE, nonce);
        template.header(HeaderConstants.X_INTERNAL_SIGNATURE, signature);
    }

    /**
     * 生成 16 位 hex 随机 nonce
     */
    private String generateNonce() {
        // 8 字节 SecureRandom → 16 位 hex 字符串
        byte[] bytes = new byte[8];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
