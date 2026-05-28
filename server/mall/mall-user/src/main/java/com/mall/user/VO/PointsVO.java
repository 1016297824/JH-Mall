package com.mall.user.VO;

import lombok.Data;

/**
 * 积分余额响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class PointsVO {

    /** 累计积分 */
    private Integer totalPoints;

    /** 可用积分 */
    private Integer availablePoints;

    /** 已使用积分 */
    private Integer usedPoints;

    /** 已过期积分 */
    private Integer expiredPoints;
}
