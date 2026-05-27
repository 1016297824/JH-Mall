package com.mall.marketing.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.marketing.DO.MallMarketingPromotionRule;
import com.mall.marketing.mapper.MallMarketingPromotionMapper;
import com.mall.marketing.DO.MallMarketingPromotion;
import com.mall.marketing.service.IMallMarketingPromotionService;

/**
 * 活动管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@Service
public class MallMarketingPromotionServiceImpl implements IMallMarketingPromotionService
{
    @Autowired
    private MallMarketingPromotionMapper mallMarketingPromotionMapper;

    /**
     * 查询活动管理
     *
     * @param id 活动管理主键
     * @return 活动管理
     */
    @Override
    public MallMarketingPromotion selectMallMarketingPromotionById(String id)
    {
        return mallMarketingPromotionMapper.selectMallMarketingPromotionById(id);
    }

    /**
     * 查询活动管理列表
     *
     * @param mallMarketingPromotion 活动管理
     * @return 活动管理
     */
    @Override
    public List<MallMarketingPromotion> selectMallMarketingPromotionList(MallMarketingPromotion mallMarketingPromotion)
    {
        return mallMarketingPromotionMapper.selectMallMarketingPromotionList(mallMarketingPromotion);
    }

    /**
     * 新增活动管理
     *
     * @param mallMarketingPromotion 活动管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMallMarketingPromotion(MallMarketingPromotion mallMarketingPromotion)
    {
        mallMarketingPromotion.setCreateTime(DateUtils.getNowDate());
        int rows = mallMarketingPromotionMapper.insertMallMarketingPromotion(mallMarketingPromotion);
        insertMallMarketingPromotionRule(mallMarketingPromotion);
        return rows;
    }

    /**
     * 修改活动管理
     *
     * @param mallMarketingPromotion 活动管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallMarketingPromotion(MallMarketingPromotion mallMarketingPromotion)
    {
        mallMarketingPromotion.setUpdateTime(DateUtils.getNowDate());
        mallMarketingPromotionMapper.deleteMallMarketingPromotionRuleByPromotionId(mallMarketingPromotion.getId());
        insertMallMarketingPromotionRule(mallMarketingPromotion);
        return mallMarketingPromotionMapper.updateMallMarketingPromotion(mallMarketingPromotion);
    }

    /**
     * 批量删除活动管理
     *
     * @param ids 需要删除的活动管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallMarketingPromotionByIds(String[] ids)
    {
        mallMarketingPromotionMapper.deleteMallMarketingPromotionRuleByPromotionIds(ids);
        return mallMarketingPromotionMapper.deleteMallMarketingPromotionByIds(ids);
    }

    /**
     * 删除活动管理信息
     *
     * @param id 活动管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallMarketingPromotionById(String id)
    {
        mallMarketingPromotionMapper.deleteMallMarketingPromotionRuleByPromotionId(id);
        return mallMarketingPromotionMapper.deleteMallMarketingPromotionById(id);
    }

    /**
     * 新增促销规则信息
     *
     * @param mallMarketingPromotion 活动管理对象
     */
    public void insertMallMarketingPromotionRule(MallMarketingPromotion mallMarketingPromotion)
    {
        List<MallMarketingPromotionRule> mallMarketingPromotionRuleList = mallMarketingPromotion.getMallMarketingPromotionRuleList();
        String id = mallMarketingPromotion.getId();
        if (StringUtils.isNotNull(mallMarketingPromotionRuleList))
        {
            List<MallMarketingPromotionRule> list = new ArrayList<MallMarketingPromotionRule>();
            for (MallMarketingPromotionRule mallMarketingPromotionRule : mallMarketingPromotionRuleList)
            {
                mallMarketingPromotionRule.setPromotionId(id);
                list.add(mallMarketingPromotionRule);
            }
            if (list.size() > 0)
            {
                mallMarketingPromotionMapper.batchMallMarketingPromotionRule(list);
            }
        }
    }
}
