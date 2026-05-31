package com.mall.auth.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 响应
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRespDTO {

    /** 访问令牌 */
    private String accessToken;
    /** 刷新令牌 */
    private String refreshToken;
    /** 过期时间（秒） */
    private long expiresIn;
}
