package com.mall.marketing.mapper;

import java.util.List;
import com.mall.marketing.DO.MallMarketingPromotion;
import com.mall.marketing.DO.MallMarketingPromotionRule;

/**
 * 活动管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface MallMarketingPromotionMapper
{
    /**
     * 查询活动管理
     *
     * @param id 活动管理主键
     * @return 活动管理
     */
    public MallMarketingPromotion selectMallMarketingPromotionById(String id);

    /**
     * 查询活动管理列表
     *
     * @param mallMarketingPromotion 活动管理
     * @return 活动管理集合
     */
    public List<MallMarketingPromotion> selectMallMarketingPromotionList(MallMarketingPromotion mallMarketingPromotion);

    /**
     * 新增活动管理
     *
     * @param mallMarketingPromotion 活动管理
     * @return 结果
     */
    public int insertMallMarketingPromotion(MallMarketingPromotion mallMarketingPromotion);

    /**
     * 修改活动管理
     *
     * @param mallMarketingPromotion 活动管理
     * @return 结果
     */
    public int updateMallMarketingPromotion(MallMarketingPromotion mallMarketingPromotion);

    /**
     * 删除活动管理
     *
     * @param id 活动管理主键
     * @return 结果
     */
    public int deleteMallMarketingPromotionById(String id);

    /**
     * 批量删除活动管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallMarketingPromotionByIds(String[] ids);

    /**
     * 批量删除促销规则
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallMarketingPromotionRuleByPromotionIds(String[] ids);

    /**
     * 批量新增促销规则
     *
     * @param mallMarketingPromotionRuleList 促销规则列表
     * @return 结果
     */
    public int batchMallMarketingPromotionRule(List<MallMarketingPromotionRule> mallMarketingPromotionRuleList);


    /**
     * 通过活动管理主键删除促销规则信息
     *
     * @param id 活动管理ID
     * @return 结果
     */
    public int deleteMallMarketingPromotionRuleByPromotionId(String id);
}
