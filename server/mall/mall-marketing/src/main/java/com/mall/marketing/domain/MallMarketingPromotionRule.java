package com.mall.marketing.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 促销规则对象 mall_marketing_promotion_rule
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallMarketingPromotionRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;

    /** 关联活动 ID */
    @Excel(name = "关联活动 ID")
    private String promotionId;

    /** 规则类型 */
    @Excel(name = "规则类型")
    private String ruleType;

    /** 门槛金额（单位：分） */
    @Excel(name = "门槛金额")
    private String thresholdAmount;

    /** 优惠金额（单位：分） */
    @Excel(name = "优惠金额")
    private String benefitAmount;

    /** 折扣率 */
    @Excel(name = "折扣率")
    private String benefitRate;

    /** 是否互斥 */
    @Excel(name = "是否互斥")
    private String isExclusive;

    /** 优先级 */
    @Excel(name = "优先级")
    private String sortOrder;

    /** 逻辑删除标志 */
    private String isDeleted;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }
    public void setPromotionId(String promotionId) 
    {
        this.promotionId = promotionId;
    }

    public String getPromotionId() 
    {
        return promotionId;
    }
    public void setRuleType(String ruleType) 
    {
        this.ruleType = ruleType;
    }

    public String getRuleType() 
    {
        return ruleType;
    }
    public void setThresholdAmount(String thresholdAmount) 
    {
        this.thresholdAmount = thresholdAmount;
    }

    public String getThresholdAmount() 
    {
        return thresholdAmount;
    }
    public void setBenefitAmount(String benefitAmount) 
    {
        this.benefitAmount = benefitAmount;
    }

    public String getBenefitAmount() 
    {
        return benefitAmount;
    }
    public void setBenefitRate(String benefitRate) 
    {
        this.benefitRate = benefitRate;
    }

    public String getBenefitRate() 
    {
        return benefitRate;
    }
    public void setIsExclusive(String isExclusive) 
    {
        this.isExclusive = isExclusive;
    }

    public String getIsExclusive() 
    {
        return isExclusive;
    }
    public void setSortOrder(String sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public String getSortOrder() 
    {
        return sortOrder;
    }
    public void setIsDeleted(String isDeleted) 
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted() 
    {
        return isDeleted;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("promotionId", getPromotionId())
            .append("ruleType", getRuleType())
            .append("thresholdAmount", getThresholdAmount())
            .append("benefitAmount", getBenefitAmount())
            .append("benefitRate", getBenefitRate())
            .append("isExclusive", getIsExclusive())
            .append("sortOrder", getSortOrder())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
