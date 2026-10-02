package com.mall.marketing.service;

import com.mall.marketing.dto.response.PromotionResp;

import java.util.List;

/**
 * 促销活动服务
 *
 * <p>C 端活动展示。设计依据：
 * {@code docs/design/14_mall-marketing详细设计.md} §2.2 第 4 项（无需登录）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface PromotionService {

    /**
     * 查询当前进行中的活动列表
     *
     * <p>「进行中」= 状态为 {@code promotion_status=1} 且当前时间落在
     * {@code [start_time, end_time)} 区间内，按 {@code sort_order} 升序。</p>
     *
     * @param limit 单次返回上限
     * @return 进行中的活动列表
     */
    List<PromotionResp> listActivePromotions(int limit);
}
