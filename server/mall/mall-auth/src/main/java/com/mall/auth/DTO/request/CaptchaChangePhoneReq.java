package com.mall.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 验证码换绑手机请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CaptchaChangePhoneReq {

    /** 旧手机号 */
    @NotBlank(message = "旧手机号不能为空")
    private String oldPhone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 新手机号 */
    @NotBlank(message = "新手机号不能为空")
    private String newPhone;

    /**
     * 获取旧手机号
     *
     * @return 旧手机号
     */
    public String getOldPhone() {
        return oldPhone;
    }

    /**
     * 设置旧手机号
     *
     * @param oldPhone 旧手机号
     */
    public void setOldPhone(String oldPhone) {
        this.oldPhone = oldPhone;
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
     * 获取新手机号
     *
     * @return 新手机号
     */
    public String getNewPhone() {
        return newPhone;
    }

    /**
     * 设置新手机号
     *
     * @param newPhone 新手机号
     */
    public void setNewPhone(String newPhone) {
        this.newPhone = newPhone;
    }
}
