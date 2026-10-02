package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.DO.MallPromotionRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 促销规则 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallPromotionRuleMapper extends BaseMapper<MallPromotionRuleDO> {

    /**
     * 批量查询活动下的规则
     *
     * <p>试算时一次性把候选活动的规则全部取回，避免 N+1 查询；
     * 按 {@code promotion_id} + {@code sort_order} 升序，便于调用方顺序匹配。</p>
     *
     * @param promotionIds 活动 ID 集合，空集合直接返回空列表
     * @return 规则列表
     */
    default List<MallPromotionRuleDO> selectByPromotionIds(List<Long> promotionIds) {
        if (promotionIds == null || promotionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<MallPromotionRuleDO>()
                .in(MallPromotionRuleDO::getPromotionId, promotionIds)
                .eq(MallPromotionRuleDO::getIsDeleted, 0)
                .orderByAsc(MallPromotionRuleDO::getPromotionId)
                .orderByAsc(MallPromotionRuleDO::getSortOrder)
                .orderByAsc(MallPromotionRuleDO::getId));
    }
}
