package com.mall.user.DO;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 成长值流水对象 mall_user_growth_log
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUserGrowthLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 用户 ID */
    @Excel(name = "用户 ID")
    private String userId;

    /** 业务类型：order（下单）/ signin（签到）/ refund（退款）/ admin（管理员调整） */
    @Excel(name = "业务类型：order", readConverterExp = "下=单")
    private String bizType;

    /** 业务单号 */
    @Excel(name = "业务单号")
    private String bizNo;

    /** 变动方向：1=增加 2=减少 */
    @Excel(name = "变动方向：1=增加 2=减少")
    private String changeType;

    /** 本次变动成长值 */
    @Excel(name = "本次变动成长值")
    private String growth;

    /** 变动前成长值余额 */
    @Excel(name = "变动前成长值余额")
    private String beforeGrowth;

    /** 变动后成长值余额 */
    @Excel(name = "变动后成长值余额")
    private String afterGrowth;

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

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizNo(String bizNo)
    {
        this.bizNo = bizNo;
    }

    public String getBizNo()
    {
        return bizNo;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
    }

    public String getChangeType()
    {
        return changeType;
    }

    public void setGrowth(String growth)
    {
        this.growth = growth;
    }

    public String getGrowth()
    {
        return growth;
    }

    public void setBeforeGrowth(String beforeGrowth)
    {
        this.beforeGrowth = beforeGrowth;
    }

    public String getBeforeGrowth()
    {
        return beforeGrowth;
    }

    public void setAfterGrowth(String afterGrowth)
    {
        this.afterGrowth = afterGrowth;
    }

    public String getAfterGrowth()
    {
        return afterGrowth;
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
            .append("userId", getUserId())
            .append("bizType", getBizType())
            .append("bizNo", getBizNo())
            .append("changeType", getChangeType())
            .append("growth", getGrowth())
            .append("beforeGrowth", getBeforeGrowth())
            .append("afterGrowth", getAfterGrowth())
            .append("remark", getRemark())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
