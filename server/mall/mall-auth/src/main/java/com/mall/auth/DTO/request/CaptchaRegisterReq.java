package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码注册请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@NoArgsConstructor
public class CaptchaRegisterReq {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在8-32个字符之间")
    private String password;

    /** 验证码 Key */
    @NotBlank(message = "验证码Key不能为空")
    private String captchaKey;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    /** 是否同意隐私协议 */
    @NotNull(message = "请同意隐私协议")
    private Boolean isPrivacyAgreed;
}
