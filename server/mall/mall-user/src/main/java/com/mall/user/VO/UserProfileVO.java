package com.mall.user.vo;

import lombok.Data;

/**
 * 用户资料响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class UserProfileVO {

    /** 用户ID */
    private String userId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别：0未知/1男/2女 */
    private Integer gender;

    /** 性别名称 */
    private String genderName;

    /** 生日 */
    private String birthday;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 会员等级名称 */
    private String membershipLevel;

    /** 会员等级图标 */
    private String membershipIcon;

    /** 当前成长值 */
    private Integer growth;

    /** 累计成长值 */
    private Integer totalGrowth;

    /** 当前积分 */
    private Integer points;

    /** 可用积分 */
    private Integer availablePoints;
}
