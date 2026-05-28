package com.mall.user.VO;

import lombok.Data;

/**
 * 成长值响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class GrowthVO {

    /** 当前成长值 */
    private Integer growth;

    /** 累计成长值 */
    private Integer totalGrowth;

    /** 当前会员等级 */
    private MemberLevelVO currentLevel;

    /** 下一级会员等级 */
    private MemberLevelVO nextLevel;

    /** 距离下一级所需成长值 */
    private Integer needGrowth;

    /** 等级进度百分比 */
    private Integer progressPercent;
}
