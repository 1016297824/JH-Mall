package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.service.CaptchaService;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.CaptchaException;
import com.wf.captcha.SpecCaptcha;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 *
 * <p>使用 EasyCaptcha 生成 130×48 的 4 位字符验证码，排除易混淆字符（0、o、1、i）。
 * 验证码文本存入 Redis，校验通过后删除 Key。同一 IP 错误次数超限后触发频率限制。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MallAuthConfigProperties authProperties;

    /**
     * 构造验证码服务
     *
     * @param redisTemplate  Redis 模板
     * @param authProperties 认证配置属性
     */
    public CaptchaServiceImpl(RedisTemplate<String, Object> redisTemplate, MallAuthConfigProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    /**
     * 生成图形验证码
     *
     * @return Map 包含 captchaKey（验证码键）和 captchaImage（Base64 图片）
     */
    @Override
    public Map<String, String> generate() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String text = captcha.text().toLowerCase();

        while (text.contains("0") || text.contains("o") || text.contains("1") || text.contains("i")) {
            captcha = new SpecCaptcha(130, 48, 4);
            text = captcha.text().toLowerCase();
        }
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(
                CacheConstants.Auth.CAPTCHA + captchaKey,
                text,
                authProperties.getSms().getCodeTtl(),
                TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captcha.toBase64());
        return result;
    }

    /**
     * 校验图形验证码
     *
     * @param captchaKey  验证码键
     * @param captchaCode 用户输入的验证码
     * @param clientIp    客户端 IP（用于频率限制）
     */
    @Override
    public void verify(String captchaKey, String captchaCode, String clientIp) {
        if (captchaKey == null || captchaCode == null || captchaKey.isEmpty() || captchaCode.isEmpty()) {
            throw new CaptchaException(ErrorCode.PARAM_MISSING);
        }

        String ipKey = CacheConstants.Auth.CAPTCHA_IP + clientIp;
        Integer ipCount = (Integer) redisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && ipCount >= authProperties.getSms().getIpDailyLimit()) {
            throw new CaptchaException(ErrorCode.CAPTCHA_RETRY_LIMIT);
        }

        String redisKey = CacheConstants.Auth.CAPTCHA + captchaKey;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            incrementIpCount(ipKey);
            throw new CaptchaException(ErrorCode.CAPTCHA_EXPIRED);
        }

        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            incrementIpCount(ipKey);
            throw new CaptchaException(ErrorCode.CAPTCHA_WRONG);
        }

        redisTemplate.delete(redisKey);
    }

    /**
     * 递增 IP 验证码错误计数（首次设置 TTL）
     *
     * @param ipKey 验证码 IP 计数 Key
     */
    private void incrementIpCount(String ipKey) {
        Long count = redisTemplate.opsForValue().increment(ipKey, 1L);
        if (count != null && count == 1) {
            redisTemplate.expire(ipKey, authProperties.getCaptcha().getIpTtl(), TimeUnit.SECONDS);
        }
    }
}
