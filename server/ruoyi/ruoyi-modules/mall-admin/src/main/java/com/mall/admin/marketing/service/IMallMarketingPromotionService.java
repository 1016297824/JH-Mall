package com.mall.admin.marketing.service;

import java.util.List;
import com.mall.admin.marketing.domain.MallMarketingPromotion;

/**
 * 活动管理Service接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface IMallMarketingPromotionService
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
     * 批量删除活动管理
     *
     * @param ids 需要删除的活动管理主键集合
     * @return 结果
     */
    public int deleteMallMarketingPromotionByIds(String[] ids);

    /**
     * 删除活动管理信息
     *
     * @param id 活动管理主键
     * @return 结果
     */
    public int deleteMallMarketingPromotionById(String id);
}
