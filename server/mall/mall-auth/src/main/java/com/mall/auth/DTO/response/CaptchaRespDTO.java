package com.mall.auth.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码 Key */
    private String captchaKey;
    /** 验证码图片 Base64 */
    private String captchaImage;
}
