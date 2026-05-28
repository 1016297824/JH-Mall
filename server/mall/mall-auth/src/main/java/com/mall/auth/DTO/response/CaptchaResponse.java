package com.mall.auth.DTO.response;

/**
 * 验证码响应
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CaptchaResponse {

    /** 验证码 Key */
    private String captchaKey;
    /** 验证码图片 Base64 */
    private String captchaImage;

    /**
     * 无参构造
     */
    public CaptchaResponse() {
    }

    /**
     * 全参构造
     *
     * @param captchaKey   验证码 Key
     * @param captchaImage 验证码图片 Base64
     */
    public CaptchaResponse(String captchaKey, String captchaImage) {
        this.captchaKey = captchaKey;
        this.captchaImage = captchaImage;
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
     * 获取验证码图片 Base64
     *
     * @return 验证码图片 Base64
     */
    public String getCaptchaImage() {
        return captchaImage;
    }

    /**
     * 设置验证码图片 Base64
     *
     * @param captchaImage 验证码图片 Base64
     */
    public void setCaptchaImage(String captchaImage) {
        this.captchaImage = captchaImage;
    }
}
