package com.mall.marketing.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 用户优惠券记录对象 mall_marketing_coupon_record
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallMarketingCouponRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;

    /** 关联优惠券定义 ID */
    @Excel(name = "关联优惠券定义 ID")
    private String couponId;

    /** 领取用户 ID */
    @Excel(name = "领取用户 ID")
    private String userId;

    /** 优惠券编码（全局唯一） */
    @Excel(name = "优惠券编码")
    private String couponCode;

    /** 记录状态 */
    @Excel(name = "记录状态")
    private String recordStatus;

    /** 使用/锁定的订单号 */
    @Excel(name = "使用/锁定的订单号")
    private String orderNo;

    /** 券面值（单位：分） */
    @Excel(name = "券面值")
    private String faceValue;

    /** 锁定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "锁定时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date lockTime;

    /** 使用时间 */
    @Excel(name = "使用时间")
    private Date useTime;

    /** 释放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "释放时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date releaseTime;

    /** 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireTime;

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
    public void setCouponId(String couponId) 
    {
        this.couponId = couponId;
    }

    public String getCouponId() 
    {
        return couponId;
    }
    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getUserId() 
    {
        return userId;
    }
    public void setCouponCode(String couponCode) 
    {
        this.couponCode = couponCode;
    }

    public String getCouponCode() 
    {
        return couponCode;
    }
    public void setRecordStatus(String recordStatus) 
    {
        this.recordStatus = recordStatus;
    }

    public String getRecordStatus() 
    {
        return recordStatus;
    }
    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }
    public void setFaceValue(String faceValue) 
    {
        this.faceValue = faceValue;
    }

    public String getFaceValue() 
    {
        return faceValue;
    }
    public void setLockTime(Date lockTime) 
    {
        this.lockTime = lockTime;
    }

    public Date getLockTime() 
    {
        return lockTime;
    }
    public void setUseTime(Date useTime) 
    {
        this.useTime = useTime;
    }

    public Date getUseTime() 
    {
        return useTime;
    }
    public void setReleaseTime(Date releaseTime) 
    {
        this.releaseTime = releaseTime;
    }

    public Date getReleaseTime() 
    {
        return releaseTime;
    }
    public void setExpireTime(Date expireTime) 
    {
        this.expireTime = expireTime;
    }

    public Date getExpireTime() 
    {
        return expireTime;
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
            .append("couponId", getCouponId())
            .append("userId", getUserId())
            .append("couponCode", getCouponCode())
            .append("recordStatus", getRecordStatus())
            .append("orderNo", getOrderNo())
            .append("faceValue", getFaceValue())
            .append("lockTime", getLockTime())
            .append("useTime", getUseTime())
            .append("releaseTime", getReleaseTime())
            .append("expireTime", getExpireTime())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
