package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.service.CaptchaService;
import com.mall.common.constant.CacheConstants;
import com.mall.common.exception.CaptchaException;
import com.wf.captcha.SpecCaptcha;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MallAuthConfigProperties authProperties;

    public CaptchaServiceImpl(RedisTemplate<String, Object> redisTemplate, MallAuthConfigProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

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

    @Override
    public void verify(String captchaKey, String captchaCode, String clientIp) {
        if (captchaKey == null || captchaCode == null || captchaKey.isEmpty() || captchaCode.isEmpty()) {
            throw new CaptchaException("A0401", "请完整填写信息");
        }

        String ipKey = CacheConstants.Auth.CAPTCHA_IP + clientIp;
        Integer ipCount = (Integer) redisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && ipCount >= authProperties.getSms().getIpDailyLimit()) {
            throw new CaptchaException("A0241", "验证码尝试次数过多", "验证码尝试次数过多，请 24 小时后重试");
        }

        String redisKey = CacheConstants.Auth.CAPTCHA + captchaKey;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            incrementIpCount(ipKey);
            throw new CaptchaException("A0132", "验证码已过期", "验证码已过期，请重新获取");
        }

        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            incrementIpCount(ipKey);
            throw new CaptchaException("A0131", "验证码错误", "验证码错误，请重新输入");
        }

        redisTemplate.delete(redisKey);
    }

    private void incrementIpCount(String ipKey) {
        Long count = redisTemplate.opsForValue().increment(ipKey, 1L);
        if (count != null && count == 1) {
            redisTemplate.expire(ipKey, authProperties.getCaptcha().getIpTtl(), TimeUnit.SECONDS);
        }
    }
}
