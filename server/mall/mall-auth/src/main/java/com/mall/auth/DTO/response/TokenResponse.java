package com.mall.auth.DTO.response;

/**
 * Token 响应
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class TokenResponse {

    /** 访问令牌 */
    private String accessToken;
    /** 刷新令牌 */
    private String refreshToken;
    /** 过期时间（秒） */
    private long expiresIn;

    /**
     * 无参构造
     */
    public TokenResponse() {
    }

    /**
     * 全参构造
     *
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn    过期时间（秒）
     */
    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    /**
     * 获取访问令牌
     *
     * @return 访问令牌
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 设置访问令牌
     *
     * @param accessToken 访问令牌
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 获取刷新令牌
     *
     * @return 刷新令牌
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 设置刷新令牌
     *
     * @param refreshToken 刷新令牌
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 获取过期时间
     *
     * @return 过期时间（秒）
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * 设置过期时间
     *
     * @param expiresIn 过期时间（秒）
     */
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
