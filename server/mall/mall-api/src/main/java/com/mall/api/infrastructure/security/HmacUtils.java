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
 * @author JH-Mall
 * @date 2026/05/28
 */
final class HmacUtils {

    /** 工具类禁止实例化 */
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
            // 用密钥初始化 HmacSHA256 算法
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SecurityConstants.HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            // 执行签名并输出 hex 编码
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // 算法不可用或密钥非法，抛系统内部异常
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL);
        }
    }
}
