package com.mall.order.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 金额快照对象 mall_order_amount
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallOrderAmount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 订单 ID */
    @Excel(name = "订单 ID")
    private String orderId;

    /** 商品明细快照 */
    @Excel(name = "商品明细快照")
    private String itemsJson;

    /** 优惠券使用快照 */
    @Excel(name = "优惠券使用快照")
    private String couponSnapshotJson;

    /** 活动优惠快照 */
    @Excel(name = "活动优惠快照")
    private String promotionSnapshotJson;

    /** 积分抵扣金额（单位：分） */
    @Excel(name = "积分抵扣金额", readConverterExp = "单=位：分")
    private String pointsDiscount;

    /** 商品总金额（单位：分） */
    @Excel(name = "商品总金额", readConverterExp = "单=位：分")
    private String totalAmount;

    /** 优惠总金额（单位：分） */
    @Excel(name = "优惠总金额", readConverterExp = "单=位：分")
    private String discountAmount;

    /** 运费（单位：分） */
    @Excel(name = "运费", readConverterExp = "单=位：分")
    private String freightAmount;

    /** 实付金额（单位：分） */
    @Excel(name = "实付金额", readConverterExp = "单=位：分")
    private String payAmount;

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

    public void setOrderId(String orderId) 
    {
        this.orderId = orderId;
    }

    public String getOrderId() 
    {
        return orderId;
    }

    public void setItemsJson(String itemsJson) 
    {
        this.itemsJson = itemsJson;
    }

    public String getItemsJson() 
    {
        return itemsJson;
    }

    public void setCouponSnapshotJson(String couponSnapshotJson) 
    {
        this.couponSnapshotJson = couponSnapshotJson;
    }

    public String getCouponSnapshotJson() 
    {
        return couponSnapshotJson;
    }

    public void setPromotionSnapshotJson(String promotionSnapshotJson) 
    {
        this.promotionSnapshotJson = promotionSnapshotJson;
    }

    public String getPromotionSnapshotJson() 
    {
        return promotionSnapshotJson;
    }

    public void setPointsDiscount(String pointsDiscount) 
    {
        this.pointsDiscount = pointsDiscount;
    }

    public String getPointsDiscount() 
    {
        return pointsDiscount;
    }

    public void setTotalAmount(String totalAmount) 
    {
        this.totalAmount = totalAmount;
    }

    public String getTotalAmount() 
    {
        return totalAmount;
    }

    public void setDiscountAmount(String discountAmount) 
    {
        this.discountAmount = discountAmount;
    }

    public String getDiscountAmount() 
    {
        return discountAmount;
    }

    public void setFreightAmount(String freightAmount) 
    {
        this.freightAmount = freightAmount;
    }

    public String getFreightAmount() 
    {
        return freightAmount;
    }

    public void setPayAmount(String payAmount) 
    {
        this.payAmount = payAmount;
    }

    public String getPayAmount() 
    {
        return payAmount;
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
            .append("orderId", getOrderId())
            .append("itemsJson", getItemsJson())
            .append("couponSnapshotJson", getCouponSnapshotJson())
            .append("promotionSnapshotJson", getPromotionSnapshotJson())
            .append("pointsDiscount", getPointsDiscount())
            .append("totalAmount", getTotalAmount())
            .append("discountAmount", getDiscountAmount())
            .append("freightAmount", getFreightAmount())
            .append("payAmount", getPayAmount())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
