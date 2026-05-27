package com.mall.order.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 订单管理对象 mall_order
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 订单号，格式 JH + 时间戳 + 随机数 */
    @Excel(name = "订单号，格式 JH + 时间戳 + 随机数")
    private String orderNo;

    /** 用户 ID */
    @Excel(name = "用户 ID")
    private String userId;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String orderStatus;

    /** 商品总金额（单位：分） */
    @Excel(name = "商品总金额", readConverterExp = "单=位：分")
    private String totalAmount;

    /** 优惠总金额（单位：分） */
    @Excel(name = "优惠总金额", readConverterExp = "单=位：分")
    private String discountAmount;

    /** 运费金额（单位：分） */
    @Excel(name = "运费金额", readConverterExp = "单=位：分")
    private String freightAmount;

    /** 实付金额（单位：分） */
    @Excel(name = "实付金额", readConverterExp = "单=位：分")
    private String payAmount;

    /** 支付成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付成功时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payTime;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发货时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date deliveryTime;

    /** 交易完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易完成时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date completeTime;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "取消时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date cancelTime;

    /** 取消类型 */
    @Excel(name = "取消类型")
    private String cancelType;

    /** 取消原因 */
    @Excel(name = "取消原因")
    private String cancelReason;

    /** 支付过期时间，默认创建后 30 分钟 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付过期时间，默认创建后 30 分钟", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payExpireTime;

    /** 幂等键 */
    @Excel(name = "幂等键")
    private String idempotentKey;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private String version;

    /** 订单项信息 */
    private List<MallOrderItem> mallOrderItemList;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }

    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getUserId() 
    {
        return userId;
    }

    public void setOrderStatus(String orderStatus) 
    {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus() 
    {
        return orderStatus;
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

    public void setPayTime(Date payTime) 
    {
        this.payTime = payTime;
    }

    public Date getPayTime() 
    {
        return payTime;
    }

    public void setDeliveryTime(Date deliveryTime) 
    {
        this.deliveryTime = deliveryTime;
    }

    public Date getDeliveryTime() 
    {
        return deliveryTime;
    }

    public void setCompleteTime(Date completeTime) 
    {
        this.completeTime = completeTime;
    }

    public Date getCompleteTime() 
    {
        return completeTime;
    }

    public void setCancelTime(Date cancelTime) 
    {
        this.cancelTime = cancelTime;
    }

    public Date getCancelTime() 
    {
        return cancelTime;
    }

    public void setCancelType(String cancelType) 
    {
        this.cancelType = cancelType;
    }

    public String getCancelType() 
    {
        return cancelType;
    }

    public void setCancelReason(String cancelReason) 
    {
        this.cancelReason = cancelReason;
    }

    public String getCancelReason() 
    {
        return cancelReason;
    }

    public void setPayExpireTime(Date payExpireTime) 
    {
        this.payExpireTime = payExpireTime;
    }

    public Date getPayExpireTime() 
    {
        return payExpireTime;
    }

    public void setIdempotentKey(String idempotentKey) 
    {
        this.idempotentKey = idempotentKey;
    }

    public String getIdempotentKey() 
    {
        return idempotentKey;
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

    public List<MallOrderItem> getMallOrderItemList()
    {
        return mallOrderItemList;
    }

    public void setMallOrderItemList(List<MallOrderItem> mallOrderItemList)
    {
        this.mallOrderItemList = mallOrderItemList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("orderStatus", getOrderStatus())
            .append("totalAmount", getTotalAmount())
            .append("discountAmount", getDiscountAmount())
            .append("freightAmount", getFreightAmount())
            .append("payAmount", getPayAmount())
            .append("payTime", getPayTime())
            .append("deliveryTime", getDeliveryTime())
            .append("completeTime", getCompleteTime())
            .append("cancelTime", getCancelTime())
            .append("cancelType", getCancelType())
            .append("cancelReason", getCancelReason())
            .append("payExpireTime", getPayExpireTime())
            .append("remark", getRemark())
            .append("idempotentKey", getIdempotentKey())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .append("mallOrderItemList", getMallOrderItemList())
            .toString();
    }
}
