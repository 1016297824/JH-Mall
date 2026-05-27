package com.mall.user.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 用户会员信息对象 mall_user_member
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUserMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 用户 ID，与 mall_user 一对一 */
    @Excel(name = "用户 ID，与 mall_user 一对一")
    private String userId;

    /** 当前会员等级 ID，关联 mall_user_member_level */
    @Excel(name = "当前会员等级 ID，关联 mall_user_member_level")
    private String levelId;

    /** 当前成长值 */
    @Excel(name = "当前成长值")
    private String growth;

    /** 累计获取成长值，仅增不减 */
    @Excel(name = "累计获取成长值，仅增不减")
    private String totalGrowth;

    /** 最近一次等级生效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最近一次等级生效时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date levelStartTime;

    /** 等级到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "等级到期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date levelEndTime;

    /** 首次成为会员时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "首次成为会员时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date becomeTime;

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

    public void setLevelId(String levelId) 
    {
        this.levelId = levelId;
    }

    public String getLevelId() 
    {
        return levelId;
    }

    public void setGrowth(String growth) 
    {
        this.growth = growth;
    }

    public String getGrowth() 
    {
        return growth;
    }

    public void setTotalGrowth(String totalGrowth) 
    {
        this.totalGrowth = totalGrowth;
    }

    public String getTotalGrowth() 
    {
        return totalGrowth;
    }

    public void setLevelStartTime(Date levelStartTime) 
    {
        this.levelStartTime = levelStartTime;
    }

    public Date getLevelStartTime() 
    {
        return levelStartTime;
    }

    public void setLevelEndTime(Date levelEndTime) 
    {
        this.levelEndTime = levelEndTime;
    }

    public Date getLevelEndTime() 
    {
        return levelEndTime;
    }

    public void setBecomeTime(Date becomeTime) 
    {
        this.becomeTime = becomeTime;
    }

    public Date getBecomeTime() 
    {
        return becomeTime;
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
            .append("levelId", getLevelId())
            .append("growth", getGrowth())
            .append("totalGrowth", getTotalGrowth())
            .append("levelStartTime", getLevelStartTime())
            .append("levelEndTime", getLevelEndTime())
            .append("becomeTime", getBecomeTime())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
