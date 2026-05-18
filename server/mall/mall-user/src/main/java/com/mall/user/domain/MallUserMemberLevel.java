package com.mall.user.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 会员等级定义对象 mall_user_member_level
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUserMemberLevel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 等级名称，如「普通会员」、「银卡会员」、「金卡会员」、「钻石会员」 */
    @Excel(name = "等级名称，如「普通会员」、「银卡会员」、「金卡会员」、「钻石会员」")
    private String levelName;

    /** 等级值，数值越大等级越高 */
    @Excel(name = "等级值，数值越大等级越高")
    private String levelValue;

    /** 该等级所需的最低成长值 */
    @Excel(name = "该等级所需的最低成长值")
    private Long minGrowth;

    /** 该等级的最高成长值 */
    @Excel(name = "该等级的最高成长值")
    private Long maxGrowth;

    /** 等级图标 URL */
    @Excel(name = "等级图标 URL")
    private String icon;

    /** 权益 JSON，如折扣率、免邮费、积分倍数 */
    @Excel(name = "权益 JSON，如折扣率、免邮费、积分倍数")
    private String benefitsJson;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setLevelName(String levelName) 
    {
        this.levelName = levelName;
    }

    public String getLevelName() 
    {
        return levelName;
    }

    public void setLevelValue(String levelValue) 
    {
        this.levelValue = levelValue;
    }

    public String getLevelValue() 
    {
        return levelValue;
    }

    public void setMinGrowth(Long minGrowth) 
    {
        this.minGrowth = minGrowth;
    }

    public Long getMinGrowth() 
    {
        return minGrowth;
    }

    public void setMaxGrowth(Long maxGrowth) 
    {
        this.maxGrowth = maxGrowth;
    }

    public Long getMaxGrowth() 
    {
        return maxGrowth;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
    }

    public void setBenefitsJson(String benefitsJson) 
    {
        this.benefitsJson = benefitsJson;
    }

    public String getBenefitsJson() 
    {
        return benefitsJson;
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
            .append("levelName", getLevelName())
            .append("levelValue", getLevelValue())
            .append("minGrowth", getMinGrowth())
            .append("maxGrowth", getMaxGrowth())
            .append("icon", getIcon())
            .append("benefitsJson", getBenefitsJson())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
