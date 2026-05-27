package com.mall.marketing.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 活动管理对象 mall_marketing_promotion
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallMarketingPromotion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 活动名称 */
    @Excel(name = "活动名称")
    private String promotionName;

    /** 活动类型 */
    @Excel(name = "活动类型")
    private String promotionType;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "活动开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "活动结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 活动状态 */
    @Excel(name = "活动状态")
    private String promotionStatus;

    /** 活动描述 */
    @Excel(name = "活动描述")
    private String description;

    /** 活动 Banner 图 URL */
    @Excel(name = "活动 Banner 图 URL")
    private String bannerImage;

    /** 排序值 */
    @Excel(name = "排序值")
    private String sortOrder;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 促销规则信息 */
    private List<MallMarketingPromotionRule> mallMarketingPromotionRuleList;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setPromotionName(String promotionName) 
    {
        this.promotionName = promotionName;
    }

    public String getPromotionName() 
    {
        return promotionName;
    }

    public void setPromotionType(String promotionType) 
    {
        this.promotionType = promotionType;
    }

    public String getPromotionType() 
    {
        return promotionType;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setPromotionStatus(String promotionStatus) 
    {
        this.promotionStatus = promotionStatus;
    }

    public String getPromotionStatus() 
    {
        return promotionStatus;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setBannerImage(String bannerImage) 
    {
        this.bannerImage = bannerImage;
    }

    public String getBannerImage() 
    {
        return bannerImage;
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

    public List<MallMarketingPromotionRule> getMallMarketingPromotionRuleList()
    {
        return mallMarketingPromotionRuleList;
    }

    public void setMallMarketingPromotionRuleList(List<MallMarketingPromotionRule> mallMarketingPromotionRuleList)
    {
        this.mallMarketingPromotionRuleList = mallMarketingPromotionRuleList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("promotionName", getPromotionName())
            .append("promotionType", getPromotionType())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("promotionStatus", getPromotionStatus())
            .append("description", getDescription())
            .append("bannerImage", getBannerImage())
            .append("sortOrder", getSortOrder())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("mallMarketingPromotionRuleList", getMallMarketingPromotionRuleList())
            .toString();
    }
}
