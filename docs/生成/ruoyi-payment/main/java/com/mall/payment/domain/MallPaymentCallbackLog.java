package com.mall.payment.DO;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 回调日志对象 mall_payment_callback_log
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public class MallPaymentCallbackLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 关联支付单号 */
    @Excel(name = "关联支付单号")
    private String paymentNo;

    /** 关联退款单号 */
    @Excel(name = "关联退款单号")
    private String refundNo;

    /** 渠道编码 */
    @Excel(name = "渠道编码")
    private String channelCode;

    /** 回调类型 */
    @Excel(name = "回调类型")
    private String callbackType;

    /** 原始回调报文 JSON */
    @Excel(name = "原始回调报文 JSON")
    private String rawBody;

    /** 验签结果 */
    @Excel(name = "验签结果")
    private String isVerified;

    /** 处理状态 */
    @Excel(name = "处理状态")
    private String processStatus;

    /** 处理完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "处理完成时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date processTime;

    /** 处理结果说明 */
    @Excel(name = "处理结果说明")
    private String processResult;

    /** 回调防重放 nonce */
    @Excel(name = "回调防重放 nonce")
    private String nonce;

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

    public void setPaymentNo(String paymentNo)
    {
        this.paymentNo = paymentNo;
    }

    public String getPaymentNo()
    {
        return paymentNo;
    }

    public void setRefundNo(String refundNo)
    {
        this.refundNo = refundNo;
    }

    public String getRefundNo()
    {
        return refundNo;
    }

    public void setChannelCode(String channelCode)
    {
        this.channelCode = channelCode;
    }

    public String getChannelCode()
    {
        return channelCode;
    }

    public void setCallbackType(String callbackType)
    {
        this.callbackType = callbackType;
    }

    public String getCallbackType()
    {
        return callbackType;
    }

    public void setRawBody(String rawBody)
    {
        this.rawBody = rawBody;
    }

    public String getRawBody()
    {
        return rawBody;
    }

    public void setIsVerified(String isVerified)
    {
        this.isVerified = isVerified;
    }

    public String getIsVerified()
    {
        return isVerified;
    }

    public void setProcessStatus(String processStatus)
    {
        this.processStatus = processStatus;
    }

    public String getProcessStatus()
    {
        return processStatus;
    }

    public void setProcessTime(Date processTime)
    {
        this.processTime = processTime;
    }

    public Date getProcessTime()
    {
        return processTime;
    }

    public void setProcessResult(String processResult)
    {
        this.processResult = processResult;
    }

    public String getProcessResult()
    {
        return processResult;
    }

    public void setNonce(String nonce)
    {
        this.nonce = nonce;
    }

    public String getNonce()
    {
        return nonce;
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
            .append("paymentNo", getPaymentNo())
            .append("refundNo", getRefundNo())
            .append("channelCode", getChannelCode())
            .append("callbackType", getCallbackType())
            .append("rawBody", getRawBody())
            .append("isVerified", getIsVerified())
            .append("processStatus", getProcessStatus())
            .append("processTime", getProcessTime())
            .append("processResult", getProcessResult())
            .append("nonce", getNonce())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
