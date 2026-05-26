package com.mall.auth.service;

import java.util.Map;

/**
 * C 端图形验证码服务接口
 *
 * <p>提供验证码生成与校验能力。验证码文本存入 Redis，Base64 图片返回前端。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface CaptchaService {

    /**
     * 生成图形验证码
     *
     * @return Map 包含 captchaKey（验证码键）和 captchaImage（Base64 图片）
     */
    Map<String, String> generate();

    /**
     * 校验图形验证码
     *
     * @param captchaKey  验证码键
     * @param captchaCode 用户输入的验证码
     * @param clientIp    客户端 IP（用于频率限制）
     */
    void verify(String captchaKey, String captchaCode, String clientIp);
}
