package com.mall.api.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
 * @author AI
 * @date 2026/05/28
 */
@Component
public class InnerFeignRequestInterceptor implements RequestInterceptor {

    @Value("${mall.security.internal-secret}")
    private String secret;

    @Override
    public void apply(RequestTemplate template) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();
        String body = template.body() != null ? new String(template.body(), StandardCharsets.UTF_8) : "";

        String payload = timestamp + SecurityConstants.SIGN_PAYLOAD_SEPARATOR + nonce;
        if (!body.isEmpty()) {
            payload += SecurityConstants.SIGN_PAYLOAD_SEPARATOR + body;
        }

        String signature = hmacSha256(payload);

        template.header(HeaderConstants.X_INTERNAL_TIMESTAMP, timestamp);
        template.header(HeaderConstants.X_INTERNAL_NONCE, nonce);
        template.header(HeaderConstants.X_INTERNAL_SIGNATURE, signature);
    }

    /**
     * 生成 16 位 hex 随机 nonce
     */
    private String generateNonce() {
        byte[] bytes = new byte[8];
        new java.security.SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * HMAC-SHA256 签名计算
     *
     * @param payload 待签名数据
     * @return 签名 hex 字符串
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
}
