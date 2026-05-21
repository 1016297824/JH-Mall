package com.mall.marketing.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 优惠券定义对象 mall_marketing_coupon
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallMarketingCoupon extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 优惠券名称 */
    @Excel(name = "优惠券名称")
    private String couponName;

    /** 优惠券类型 */
    @Excel(name = "优惠券类型")
    private String couponType;

    /** 优惠面值（单位：分），满减/无门槛时 = 减免金额，折扣券 = 0 */
    @Excel(name = "优惠面值", readConverterExp = "单=位：分")
    private String faceValue;

    /** 折扣率（百分比），折扣券专用，80=8折 */
    @Excel(name = "折扣率", readConverterExp = "百=分比")
    private String discountRate;

    /** 折扣上限（单位：分），折扣券专用 */
    @Excel(name = "折扣上限", readConverterExp = "单=位：分")
    private String discountLimit;

    /** 最低订单金额门槛（单位：分），0=无门槛 */
    @Excel(name = "最低订单金额门槛", readConverterExp = "单=位：分")
    private String minOrderAmount;

    /** 发行总量，0=不限量 */
    @Excel(name = "发行总量，0=不限量")
    private String totalCount;

    /** 剩余可领取数量 */
    @Excel(name = "剩余可领取数量")
    private String remainCount;

    /** 每人限领数量 */
    @Excel(name = "每人限领数量")
    private String perUserLimit;

    /** 有效期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date useStartTime;

    /** 有效期截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期截止时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date useEndTime;

    /** 优惠券状态 */
    @Excel(name = "优惠券状态")
    private String couponStatus;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号，控制领取并发 */
    @Excel(name = "乐观锁版本号，控制领取并发")
    private String version;

    /** 用户优惠券记录信息 */
    private List<MallMarketingCouponRecord> mallMarketingCouponRecordList;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setCouponName(String couponName) 
    {
        this.couponName = couponName;
    }

    public String getCouponName() 
    {
        return couponName;
    }

    public void setCouponType(String couponType) 
    {
        this.couponType = couponType;
    }

    public String getCouponType() 
    {
        return couponType;
    }

    public void setFaceValue(String faceValue) 
    {
        this.faceValue = faceValue;
    }

    public String getFaceValue() 
    {
        return faceValue;
    }

    public void setDiscountRate(String discountRate) 
    {
        this.discountRate = discountRate;
    }

    public String getDiscountRate() 
    {
        return discountRate;
    }

    public void setDiscountLimit(String discountLimit) 
    {
        this.discountLimit = discountLimit;
    }

    public String getDiscountLimit() 
    {
        return discountLimit;
    }

    public void setMinOrderAmount(String minOrderAmount) 
    {
        this.minOrderAmount = minOrderAmount;
    }

    public String getMinOrderAmount() 
    {
        return minOrderAmount;
    }

    public void setTotalCount(String totalCount) 
    {
        this.totalCount = totalCount;
    }

    public String getTotalCount() 
    {
        return totalCount;
    }

    public void setRemainCount(String remainCount) 
    {
        this.remainCount = remainCount;
    }

    public String getRemainCount() 
    {
        return remainCount;
    }

    public void setPerUserLimit(String perUserLimit) 
    {
        this.perUserLimit = perUserLimit;
    }

    public String getPerUserLimit() 
    {
        return perUserLimit;
    }

    public void setUseStartTime(Date useStartTime) 
    {
        this.useStartTime = useStartTime;
    }

    public Date getUseStartTime() 
    {
        return useStartTime;
    }

    public void setUseEndTime(Date useEndTime) 
    {
        this.useEndTime = useEndTime;
    }

    public Date getUseEndTime() 
    {
        return useEndTime;
    }

    public void setCouponStatus(String couponStatus) 
    {
        this.couponStatus = couponStatus;
    }

    public String getCouponStatus() 
    {
        return couponStatus;
    }

    public void setIsDeleted(String isDeleted) 
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted() 
    {
        return isDeleted;
    }

    public void setVersion(String version) 
    {
        this.version = version;
    }

    public String getVersion() 
    {
        return version;
    }

    public List<MallMarketingCouponRecord> getMallMarketingCouponRecordList()
    {
        return mallMarketingCouponRecordList;
    }

    public void setMallMarketingCouponRecordList(List<MallMarketingCouponRecord> mallMarketingCouponRecordList)
    {
        this.mallMarketingCouponRecordList = mallMarketingCouponRecordList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("couponName", getCouponName())
            .append("couponType", getCouponType())
            .append("faceValue", getFaceValue())
            .append("discountRate", getDiscountRate())
            .append("discountLimit", getDiscountLimit())
            .append("minOrderAmount", getMinOrderAmount())
            .append("totalCount", getTotalCount())
            .append("remainCount", getRemainCount())
            .append("perUserLimit", getPerUserLimit())
            .append("useStartTime", getUseStartTime())
            .append("useEndTime", getUseEndTime())
            .append("couponStatus", getCouponStatus())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .append("mallMarketingCouponRecordList", getMallMarketingCouponRecordList())
            .toString();
    }
}
