package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新 Token 请求
 *
 * @author JH-Mall
 * @date 2026/05/31
 */
@Data
@NoArgsConstructor
public class RefreshTokenReqDTO {

    /** 刷新令牌 */
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
