package com.mall.user.VO;

import lombok.Data;

/**
 * 会员等级响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class MemberLevelVO {

    /** 等级名称 */
    private String levelName;

    /** 等级图标 */
    private String icon;

    /** 等级值 */
    private Integer levelValue;
}
