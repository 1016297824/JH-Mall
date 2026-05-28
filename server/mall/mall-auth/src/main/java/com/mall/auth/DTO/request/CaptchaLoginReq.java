package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 验证码登录请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CaptchaLoginReq {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 Key */
    @NotBlank(message = "验证码Key不能为空")
    private String captchaKey;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    /**
     * 获取手机号
     *
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取验证码 Key
     *
     * @return 验证码 Key
     */
    public String getCaptchaKey() {
        return captchaKey;
    }

    /**
     * 设置验证码 Key
     *
     * @param captchaKey 验证码 Key
     */
    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    /**
     * 获取验证码
     *
     * @return 验证码
     */
    public String getCaptchaCode() {
        return captchaCode;
    }

    /**
     * 设置验证码
     *
     * @param captchaCode 验证码
     */
    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
