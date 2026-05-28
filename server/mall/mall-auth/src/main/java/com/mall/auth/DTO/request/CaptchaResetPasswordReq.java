package com.mall.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 验证码重置密码请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CaptchaResetPasswordReq {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在8-32个字符之间")
    private String newPassword;

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
     * 获取新密码
     *
     * @return 新密码
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * 设置新密码
     *
     * @param newPassword 新密码
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
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
