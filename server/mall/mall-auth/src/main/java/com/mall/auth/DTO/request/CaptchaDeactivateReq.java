package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码注销账户请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@NoArgsConstructor
public class CaptchaDeactivateReq {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
