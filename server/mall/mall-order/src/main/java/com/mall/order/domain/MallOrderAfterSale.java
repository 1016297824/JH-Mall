package com.mall.order.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 售后管理对象 mall_order_after_sale
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallOrderAfterSale extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 售后单号 */
    @Excel(name = "售后单号")
    private String afterSaleNo;

    /** 关联订单 ID */
    @Excel(name = "关联订单 ID")
    private String orderId;

    /** 关联订单项 ID */
    @Excel(name = "关联订单项 ID")
    private String orderItemId;

    /** 申请人用户 ID */
    @Excel(name = "申请人用户 ID")
    private String userId;

    /** 售后类型 */
    @Excel(name = "售后类型")
    private String afterSaleType;

    /** 退款原因 */
    @Excel(name = "退款原因")
    private String reason;

    /** 退款金额（单位：分） */
    @Excel(name = "退款金额", readConverterExp = "单=位：分")
    private String amount;

    /** 售后状态 */
    @Excel(name = "售后状态")
    private String afterSaleStatus;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "申请时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date applyTime;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date approveTime;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String approveRemark;

    /** 退货物流公司 */
    @Excel(name = "退货物流公司")
    private String returnExpressCompany;

    /** 退货物流单号 */
    @Excel(name = "退货物流单号")
    private String returnExpressNo;

    /** 商家确认收货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "商家确认收货时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date receiptTime;

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

    public void setAfterSaleNo(String afterSaleNo) 
    {
        this.afterSaleNo = afterSaleNo;
    }

    public String getAfterSaleNo() 
    {
        return afterSaleNo;
    }

    public void setOrderId(String orderId) 
    {
        this.orderId = orderId;
    }

    public String getOrderId() 
    {
        return orderId;
    }

    public void setOrderItemId(String orderItemId) 
    {
        this.orderItemId = orderItemId;
    }

    public String getOrderItemId() 
    {
        return orderItemId;
    }

    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getUserId() 
    {
        return userId;
    }

    public void setAfterSaleType(String afterSaleType) 
    {
        this.afterSaleType = afterSaleType;
    }

    public String getAfterSaleType() 
    {
        return afterSaleType;
    }

    public void setReason(String reason) 
    {
        this.reason = reason;
    }

    public String getReason() 
    {
        return reason;
    }

    public void setAmount(String amount) 
    {
        this.amount = amount;
    }

    public String getAmount() 
    {
        return amount;
    }

    public void setAfterSaleStatus(String afterSaleStatus) 
    {
        this.afterSaleStatus = afterSaleStatus;
    }

    public String getAfterSaleStatus() 
    {
        return afterSaleStatus;
    }

    public void setApplyTime(Date applyTime) 
    {
        this.applyTime = applyTime;
    }

    public Date getApplyTime() 
    {
        return applyTime;
    }

    public void setApproveTime(Date approveTime) 
    {
        this.approveTime = approveTime;
    }

    public Date getApproveTime() 
    {
        return approveTime;
    }

    public void setApproveRemark(String approveRemark) 
    {
        this.approveRemark = approveRemark;
    }

    public String getApproveRemark() 
    {
        return approveRemark;
    }

    public void setReturnExpressCompany(String returnExpressCompany) 
    {
        this.returnExpressCompany = returnExpressCompany;
    }

    public String getReturnExpressCompany() 
    {
        return returnExpressCompany;
    }

    public void setReturnExpressNo(String returnExpressNo) 
    {
        this.returnExpressNo = returnExpressNo;
    }

    public String getReturnExpressNo() 
    {
        return returnExpressNo;
    }

    public void setReceiptTime(Date receiptTime) 
    {
        this.receiptTime = receiptTime;
    }

    public Date getReceiptTime() 
    {
        return receiptTime;
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
            .append("afterSaleNo", getAfterSaleNo())
            .append("orderId", getOrderId())
            .append("orderItemId", getOrderItemId())
            .append("userId", getUserId())
            .append("afterSaleType", getAfterSaleType())
            .append("reason", getReason())
            .append("amount", getAmount())
            .append("afterSaleStatus", getAfterSaleStatus())
            .append("applyTime", getApplyTime())
            .append("approveTime", getApproveTime())
            .append("approveRemark", getApproveRemark())
            .append("returnExpressCompany", getReturnExpressCompany())
            .append("returnExpressNo", getReturnExpressNo())
            .append("receiptTime", getReceiptTime())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
