package com.mall.user.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 积分账户对象 mall_user_points_account
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUserPointsAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 用户 ID，与用户一对一 */
    @Excel(name = "用户 ID，与用户一对一")
    private String userId;

    /** 累计获取积分，仅增不减 */
    @Excel(name = "累计获取积分，仅增不减")
    private Long totalPoints;

    /** 可用积分余额 */
    @Excel(name = "可用积分余额")
    private Long availablePoints;

    /** 已使用积分 */
    @Excel(name = "已使用积分")
    private Long usedPoints;

    /** 已过期积分 */
    @Excel(name = "已过期积分")
    private Long expiredPoints;

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

    public void setTotalPoints(Long totalPoints) 
    {
        this.totalPoints = totalPoints;
    }

    public Long getTotalPoints() 
    {
        return totalPoints;
    }

    public void setAvailablePoints(Long availablePoints) 
    {
        this.availablePoints = availablePoints;
    }

    public Long getAvailablePoints() 
    {
        return availablePoints;
    }

    public void setUsedPoints(Long usedPoints) 
    {
        this.usedPoints = usedPoints;
    }

    public Long getUsedPoints() 
    {
        return usedPoints;
    }

    public void setExpiredPoints(Long expiredPoints) 
    {
        this.expiredPoints = expiredPoints;
    }

    public Long getExpiredPoints() 
    {
        return expiredPoints;
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
            .append("totalPoints", getTotalPoints())
            .append("availablePoints", getAvailablePoints())
            .append("usedPoints", getUsedPoints())
            .append("expiredPoints", getExpiredPoints())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
