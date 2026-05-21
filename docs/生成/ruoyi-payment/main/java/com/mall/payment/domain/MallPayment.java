package com.mall.payment.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 支付单对象 mall_payment
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallPayment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 支付单号，格式 PAY + 时间戳 + 随机数 */
    @Excel(name = "支付单号，格式 PAY + 时间戳 + 随机数")
    private String paymentNo;

    /** 关联订单号 */
    @Excel(name = "关联订单号")
    private String orderNo;

    /** 付款用户 ID */
    @Excel(name = "付款用户 ID")
    private String userId;

    /** 支付金额（单位：分） */
    @Excel(name = "支付金额", readConverterExp = "单=位：分")
    private String payAmount;

    /** 支付渠道编码 */
    @Excel(name = "支付渠道编码")
    private String channelCode;

    /** 渠道侧支付单号，对账用 */
    @Excel(name = "渠道侧支付单号，对账用")
    private String channelPaymentNo;

    /** 渠道侧支付状态 */
    @Excel(name = "渠道侧支付状态")
    private String channelPayStatus;

    /** 支付单状态 */
    @Excel(name = "支付单状态")
    private String paymentStatus;

    /** 支付成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付成功时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date paySuccessTime;

    /** 支付过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付过期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireTime;

    /** 异步通知地址 */
    @Excel(name = "异步通知地址")
    private String notifyUrl;

    /** 幂等键 */
    @Excel(name = "幂等键")
    private String idempotentKey;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private String version;

    /** 退款单信息 */
    private List<MallPaymentRefund> mallPaymentRefundList;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setPaymentNo(String paymentNo) 
    {
        this.paymentNo = paymentNo;
    }

    public String getPaymentNo() 
    {
        return paymentNo;
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

    public void setPayAmount(String payAmount) 
    {
        this.payAmount = payAmount;
    }

    public String getPayAmount() 
    {
        return payAmount;
    }

    public void setChannelCode(String channelCode) 
    {
        this.channelCode = channelCode;
    }

    public String getChannelCode() 
    {
        return channelCode;
    }

    public void setChannelPaymentNo(String channelPaymentNo) 
    {
        this.channelPaymentNo = channelPaymentNo;
    }

    public String getChannelPaymentNo() 
    {
        return channelPaymentNo;
    }

    public void setChannelPayStatus(String channelPayStatus) 
    {
        this.channelPayStatus = channelPayStatus;
    }

    public String getChannelPayStatus() 
    {
        return channelPayStatus;
    }

    public void setPaymentStatus(String paymentStatus) 
    {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentStatus() 
    {
        return paymentStatus;
    }

    public void setPaySuccessTime(Date paySuccessTime) 
    {
        this.paySuccessTime = paySuccessTime;
    }

    public Date getPaySuccessTime() 
    {
        return paySuccessTime;
    }

    public void setExpireTime(Date expireTime) 
    {
        this.expireTime = expireTime;
    }

    public Date getExpireTime() 
    {
        return expireTime;
    }

    public void setNotifyUrl(String notifyUrl) 
    {
        this.notifyUrl = notifyUrl;
    }

    public String getNotifyUrl() 
    {
        return notifyUrl;
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

    public List<MallPaymentRefund> getMallPaymentRefundList()
    {
        return mallPaymentRefundList;
    }

    public void setMallPaymentRefundList(List<MallPaymentRefund> mallPaymentRefundList)
    {
        this.mallPaymentRefundList = mallPaymentRefundList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("paymentNo", getPaymentNo())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("payAmount", getPayAmount())
            .append("channelCode", getChannelCode())
            .append("channelPaymentNo", getChannelPaymentNo())
            .append("channelPayStatus", getChannelPayStatus())
            .append("paymentStatus", getPaymentStatus())
            .append("paySuccessTime", getPaySuccessTime())
            .append("expireTime", getExpireTime())
            .append("notifyUrl", getNotifyUrl())
            .append("idempotentKey", getIdempotentKey())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .append("mallPaymentRefundList", getMallPaymentRefundList())
            .toString();
    }
}
