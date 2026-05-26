package com.mall.auth.service;

import com.mall.auth.dto.response.TokenResponse;

/**
 * C 端 Token 服务接口
 *
 * <p>提供 JWT Token 的签发、校验、刷新、吊销能力。accessToken 与 refreshToken 均以 jti 为主键在 Redis 中维护会话与黑名单。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface TokenService {

    /**
     * 签发 Token（同时生成 accessToken 和 refreshToken）
     *
     * @param userId 用户 ID
     * @return Token 响应
     */
    TokenResponse issue(String userId);

    /**
     * 校验 accessToken 并返回 userId
     *
     * @param accessToken 访问令牌
     * @return 用户 ID
     */
    String verify(String accessToken);

    /**
     * 使用 refreshToken 刷新 Token（旧 refreshToken 一次性使用后立即失效）
     *
     * @param refreshToken 刷新令牌
     * @return 新的 Token 响应
     */
    TokenResponse refresh(String refreshToken);

    /**
     * 吊销单条 accessToken（加入黑名单）
     *
     * @param accessToken 访问令牌
     */
    void revoke(String accessToken);

    /**
     * 吊销用户所有 Token（通过 pattern 匹配 session Key）
     *
     * @param userId 用户 ID
     */
    void revokeAll(String userId);
}
