package com.mall.payment.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 支付渠道对象 mall_payment_channel
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallPaymentChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 渠道编码 */
    @Excel(name = "渠道编码")
    private String channelCode;

    /** 渠道展示名称 */
    @Excel(name = "渠道展示名称")
    private String channelName;

    /** 渠道类型 */
    @Excel(name = "渠道类型")
    private String channelType;

    /** 渠道配置 */
    @Excel(name = "渠道配置")
    private String configJson;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;

    /** 排序值 */
    @Excel(name = "排序值")
    private String sortOrder;

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

    public void setChannelCode(String channelCode) 
    {
        this.channelCode = channelCode;
    }

    public String getChannelCode() 
    {
        return channelCode;
    }

    public void setChannelName(String channelName) 
    {
        this.channelName = channelName;
    }

    public String getChannelName() 
    {
        return channelName;
    }

    public void setChannelType(String channelType) 
    {
        this.channelType = channelType;
    }

    public String getChannelType() 
    {
        return channelType;
    }

    public void setConfigJson(String configJson) 
    {
        this.configJson = configJson;
    }

    public String getConfigJson() 
    {
        return configJson;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
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
            .append("channelCode", getChannelCode())
            .append("channelName", getChannelName())
            .append("channelType", getChannelType())
            .append("configJson", getConfigJson())
            .append("isEnabled", getIsEnabled())
            .append("sortOrder", getSortOrder())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
