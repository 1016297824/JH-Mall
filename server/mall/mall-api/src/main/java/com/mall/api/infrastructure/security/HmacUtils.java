package com.mall.api.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mall.common.constant.SecurityConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;

/**
 * HMAC-SHA256 签名工具
 *
 * @author AI
 * @date 2026/05/28
 */
final class HmacUtils {

    private HmacUtils() {
    }

    /**
     * HMAC-SHA256 签名计算
     *
     * @param payload 待签名数据
     * @param secret  密钥
     * @return 签名字符串
     */
    static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(SecurityConstants.HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SecurityConstants.HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL);
        }
    }
}
