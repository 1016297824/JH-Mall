package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.service.ICaptchaService;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.CaptchaException;
import com.wf.captcha.SpecCaptcha;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CaptchaServiceImpl implements ICaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MallAuthConfigProperties authProperties;

    /**
     * 生成图形验证码
     *
     * @return Map 包含 captchaKey（验证码键）和 captchaImage（Base64 图片）
     */
    @Override
    public Map<String, String> generate() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String text = captcha.text().toLowerCase();

        // 排除易混淆字符（0/o、1/i），避免用户识别困难
        while (text.contains("0") || text.contains("o") || text.contains("1") || text.contains("i")) {
            captcha = new SpecCaptcha(130, 48, 4);
            text = captcha.text().toLowerCase();
        }
        // 生成 UUID 作为验证码 Key（去横线）
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 验证码文本写入 Redis，TTL 由配置项 sms.codeTtl 控制（默认 300s）
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

        // 校验同一 IP 验证码错误次数是否超限
        String ipKey = CacheConstants.Auth.CAPTCHA_IP + clientIp;
        Integer ipCount = (Integer) redisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && ipCount >= authProperties.getSms().getIpDailyLimit()) {
            throw new CaptchaException(ErrorCode.CAPTCHA_RETRY_LIMIT);
        }

        // 从 Redis 取出验证码文本
        String redisKey = CacheConstants.Auth.CAPTCHA + captchaKey;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            // Key 不存在说明超时或已被消费
            incrementIpCount(ipKey);
            throw new CaptchaException(ErrorCode.CAPTCHA_EXPIRED);
        }

        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            incrementIpCount(ipKey);
            throw new CaptchaException(ErrorCode.CAPTCHA_WRONG);
        }

        // 校验通过立即删除，确保一次性使用
        redisTemplate.delete(redisKey);
    }

    /**
     * 递增 IP 验证码错误计数（首次设置 TTL）
     *
     * @param ipKey 验证码 IP 计数 Key
     */
    private void incrementIpCount(String ipKey) {
        // INCR 原子递增，首次写入时设 TTL（仅首次返回 1）
        Long count = redisTemplate.opsForValue().increment(ipKey, 1L);
        if (count != null && count == 1) {
            redisTemplate.expire(ipKey, authProperties.getCaptcha().getIpTtl(), TimeUnit.SECONDS);
        }
    }
}
