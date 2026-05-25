package com.mall.auth.service;

import java.util.Map;

public interface CaptchaService {

    Map<String, String> generate();

    void verify(String captchaKey, String captchaCode, String clientIp);
}
