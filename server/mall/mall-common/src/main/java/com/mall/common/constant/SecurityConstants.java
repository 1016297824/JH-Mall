package com.mall.common.constant;

/**
 * C 端安全常量（算法名称、签名分隔符等）
 *
 * @author AI
 * @date 2026/05/28
 */
public final class SecurityConstants {

    /** HMAC-SHA256 算法名称（JCA 标准名称） */
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /** 内部签名 payload 分隔符 */
    public static final String SIGN_PAYLOAD_SEPARATOR = "|";

    private SecurityConstants() {
    }
}
