package com.mall.admin.payment.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 退款单对象 mall_payment_refund
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallPaymentRefund extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 退款单号，格式 REF + 时间戳 + 随机数 */
    @Excel(name = "退款单号，格式 REF + 时间戳 + 随机数")
    private String refundNo;

    /** 关联支付单 ID */
    @Excel(name = "关联支付单 ID")
    private String paymentId;

    /** 关联订单号 */
    @Excel(name = "关联订单号")
    private String orderNo;

    /** 关联售后单号 */
    @Excel(name = "关联售后单号")
    private String afterSaleNo;

    /** 退款用户 ID */
    @Excel(name = "退款用户 ID")
    private String userId;

    /** 退款金额（单位：分） */
    @Excel(name = "退款金额", readConverterExp = "单=位：分")
    private String refundAmount;

    /** 退款原因 */
    @Excel(name = "退款原因")
    private String refundReason;

    /** 退款渠道编码，必须与原始支付渠道一致 */
    @Excel(name = "退款渠道编码，必须与原始支付渠道一致")
    private String channelCode;

    /** 渠道侧退款单号，对账用 */
    @Excel(name = "渠道侧退款单号，对账用")
    private String channelRefundNo;

    /** 渠道侧退款状态 */
    @Excel(name = "渠道侧退款状态")
    private String channelRefundStatus;

    /** 退款单状态 */
    @Excel(name = "退款单状态")
    private String refundStatus;

    /** 退款成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "退款成功时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date refundSuccessTime;

    /** 幂等键 */
    @Excel(name = "幂等键")
    private String idempotentKey;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private String version;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
    public void setRefundNo(String refundNo)
    {
        this.refundNo = refundNo;
    }

    public String getRefundNo()
    {
        return refundNo;
    }
    public void setPaymentId(String paymentId)
    {
        this.paymentId = paymentId;
    }

    public String getPaymentId()
    {
        return paymentId;
    }
    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }
    public void setAfterSaleNo(String afterSaleNo)
    {
        this.afterSaleNo = afterSaleNo;
    }

    public String getAfterSaleNo()
    {
        return afterSaleNo;
    }
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }
    public void setRefundAmount(String refundAmount)
    {
        this.refundAmount = refundAmount;
    }

    public String getRefundAmount()
    {
        return refundAmount;
    }
    public void setRefundReason(String refundReason)
    {
        this.refundReason = refundReason;
    }

    public String getRefundReason()
    {
        return refundReason;
    }
    public void setChannelCode(String channelCode)
    {
        this.channelCode = channelCode;
    }

    public String getChannelCode()
    {
        return channelCode;
    }
    public void setChannelRefundNo(String channelRefundNo)
    {
        this.channelRefundNo = channelRefundNo;
    }

    public String getChannelRefundNo()
    {
        return channelRefundNo;
    }
    public void setChannelRefundStatus(String channelRefundStatus)
    {
        this.channelRefundStatus = channelRefundStatus;
    }

    public String getChannelRefundStatus()
    {
        return channelRefundStatus;
    }
    public void setRefundStatus(String refundStatus)
    {
        this.refundStatus = refundStatus;
    }

    public String getRefundStatus()
    {
        return refundStatus;
    }
    public void setRefundSuccessTime(Date refundSuccessTime)
    {
        this.refundSuccessTime = refundSuccessTime;
    }

    public Date getRefundSuccessTime()
    {
        return refundSuccessTime;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("refundNo", getRefundNo())
            .append("paymentId", getPaymentId())
            .append("orderNo", getOrderNo())
            .append("afterSaleNo", getAfterSaleNo())
            .append("userId", getUserId())
            .append("refundAmount", getRefundAmount())
            .append("refundReason", getRefundReason())
            .append("channelCode", getChannelCode())
            .append("channelRefundNo", getChannelRefundNo())
            .append("channelRefundStatus", getChannelRefundStatus())
            .append("refundStatus", getRefundStatus())
            .append("refundSuccessTime", getRefundSuccessTime())
            .append("idempotentKey", getIdempotentKey())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .toString();
    }
}
