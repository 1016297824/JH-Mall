package com.mall.auth.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息响应
 *
 * @author JH-Mall
 * @date 2026/05/31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoRespDTO {

    /** 用户 ID */
    private String userId;
}
