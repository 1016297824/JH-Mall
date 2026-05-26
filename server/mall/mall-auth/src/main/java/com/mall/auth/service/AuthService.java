package com.mall.auth.service;

/**
 * C 端认证服务接口
 *
 * <p>提供密码登录、短信登录、注册、密码重置、修改密码、Token 刷新、登出等核心认证能力</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface AuthService {

    /**
     * 手机号注册
     *
     * @param phone    手机号
     * @param password 明文密码
     */
    void register(String phone, String password);

    /**
     * 密码登录
     *
     * @param phone    手机号
     * @param password 明文密码
     * @return JWT Token 响应（含 accessToken 和 refreshToken）
     */
    String loginByPassword(String phone, String password);

    /**
     * 短信验证码登录
     *
     * @param phone   手机号
     * @param smsCode 短信验证码
     * @return JWT Token 响应（含 accessToken 和 refreshToken）
     */
    String loginBySms(String phone, String smsCode);

    /**
     * 刷新 Token
     *
     * @param refreshToken 刷新令牌
     * @return 新的 JWT Token 响应
     */
    String refresh(String refreshToken);

    /**
     * 登出（吊销 accessToken）
     *
     * @param accessToken 访问令牌
     */
    void logout(String accessToken);

    /**
     * 重置密码
     *
     * @param phone       手机号
     * @param newPassword 新明文密码
     */
    void resetPassword(String phone, String newPassword);

    /**
     * 修改密码（需旧密码验证）
     *
     * @param userId      用户 ID
     * @param oldPassword 旧明文密码
     * @param newPassword 新明文密码
     */
    void changePassword(String userId, String oldPassword, String newPassword);
}
