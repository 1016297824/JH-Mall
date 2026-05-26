package com.mall.auth.service;

/**
 * C 端短信服务接口
 *
 * <p>调用第三方短信平台发送验证码短信</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void send(String phone, String code);
}
