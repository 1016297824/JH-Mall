package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码换绑手机请求
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@NoArgsConstructor
public class CaptchaChangePhoneReqDTO {

    /** 旧手机号 */
    @NotBlank(message = "旧手机号不能为空")
    private String oldPhone;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 新手机号 */
    @NotBlank(message = "新手机号不能为空")
    private String newPhone;
}
