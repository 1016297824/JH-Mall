package com.mall.user.vo;

import java.util.List;

import lombok.Data;

/**
 * 会员信息响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class MembershipVO {

    /** 当前会员等级 */
    private MemberLevelVO currentLevel;

    /** 当前成长值 */
    private Integer growth;

    /** 累计成长值 */
    private Integer totalGrowth;

    /** 下一级会员等级 */
    private MemberLevelVO nextLevel;

    /** 会员权益列表 */
    private List<String> benefits;
}
