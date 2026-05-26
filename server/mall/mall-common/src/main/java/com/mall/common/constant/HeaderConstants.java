package com.mall.common.constant;

public final class HeaderConstants {

    /** C端 JWT Token */
    public static final String AUTHORIZATION = "Authorization";

    /** C端用户ID，由网关 MallAuthFilter 注入 */
    public static final String X_USER_ID = "X-User-Id";

    /** C端认证用户名，由网关 MallAuthFilter 注入 */
    public static final String X_USER_NAME = "X-User-Name";

    /** 全链路追踪ID */
    public static final String X_REQUEST_ID = "X-Request-Id";

    /** 下单幂等键，客户端生成 UUID v4 */
    public static final String IDEMPOTENT_KEY = "Idempotent-Key";

    /** 客户端真实IP（代理透传） */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /** 客户端真实IP（Nginx） */
    public static final String X_REAL_IP = "X-Real-IP";

    /** 内部服务签名 — Unix时间戳 */
    public static final String X_INTERNAL_TIMESTAMP = "X-Internal-Timestamp";

    /** 内部服务签名 — 随机数 */
    public static final String X_INTERNAL_NONCE = "X-Internal-Nonce";

    /** 内部服务签名 — HMAC-SHA256 */
    public static final String X_INTERNAL_SIGNATURE = "X-Internal-Signature";

    /** 支付回调验签通过标记 */
    public static final String X_INTERNAL_VERIFIED = "X-Internal-Verified";

    /** 网关透传 Header 前缀，C端/管理端共用 */
    public static final String X_USER_PREFIX = "X-User-";

    /** 商城侧透传 Header 前缀 */
    public static final String X_MALL_PREFIX = "X-Mall-";

    private HeaderConstants() {
    }
}
