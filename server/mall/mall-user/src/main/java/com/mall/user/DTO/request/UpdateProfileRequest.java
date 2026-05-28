package com.mall.user.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改个人资料请求 DTO
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
@NoArgsConstructor
public class UpdateProfileRequest {

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别：0未知/1男/2女 */
    private Integer gender;

    /** 生日 */
    private String birthday;

    /** 邮箱 */
    private String email;
}
