package com.mall.order.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 订单项对象 mall_order_item
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallOrderItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 订单 ID */
    @Excel(name = "订单 ID")
    private String orderId;

    /** SPU ID（快照） */
    @Excel(name = "SPU ID", readConverterExp = "快=照")
    private String spuId;

    /** SKU ID（快照） */
    @Excel(name = "SKU ID", readConverterExp = "快=照")
    private String skuId;

    /** SKU 编码（快照） */
    @Excel(name = "SKU 编码", readConverterExp = "快=照")
    private String skuCode;

    /** SKU 名称（快照） */
    @Excel(name = "SKU 名称", readConverterExp = "快=照")
    private String skuName;

    /** SPU 名称（快照） */
    @Excel(name = "SPU 名称", readConverterExp = "快=照")
    private String spuName;

    /** 商品主图快照 URL */
    @Excel(name = "商品主图快照 URL")
    private String mainImage;

    /** 销售属性 JSON 快照 */
    @Excel(name = "销售属性 JSON 快照")
    private String attrsJson;

    /** 购买数量 */
    @Excel(name = "购买数量")
    private String quantity;

    /** 成交单价（单位：分） */
    @Excel(name = "成交单价", readConverterExp = "单=位：分")
    private String price;

    /** 单项总价（单位：分） */
    @Excel(name = "单项总价", readConverterExp = "单=位：分")
    private String totalPrice;

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
    public void setOrderId(String orderId) 
    {
        this.orderId = orderId;
    }

    public String getOrderId() 
    {
        return orderId;
    }
    public void setSpuId(String spuId) 
    {
        this.spuId = spuId;
    }

    public String getSpuId() 
    {
        return spuId;
    }
    public void setSkuId(String skuId) 
    {
        this.skuId = skuId;
    }

    public String getSkuId() 
    {
        return skuId;
    }
    public void setSkuCode(String skuCode) 
    {
        this.skuCode = skuCode;
    }

    public String getSkuCode() 
    {
        return skuCode;
    }
    public void setSkuName(String skuName) 
    {
        this.skuName = skuName;
    }

    public String getSkuName() 
    {
        return skuName;
    }
    public void setSpuName(String spuName) 
    {
        this.spuName = spuName;
    }

    public String getSpuName() 
    {
        return spuName;
    }
    public void setMainImage(String mainImage) 
    {
        this.mainImage = mainImage;
    }

    public String getMainImage() 
    {
        return mainImage;
    }
    public void setAttrsJson(String attrsJson) 
    {
        this.attrsJson = attrsJson;
    }

    public String getAttrsJson() 
    {
        return attrsJson;
    }
    public void setQuantity(String quantity) 
    {
        this.quantity = quantity;
    }

    public String getQuantity() 
    {
        return quantity;
    }
    public void setPrice(String price) 
    {
        this.price = price;
    }

    public String getPrice() 
    {
        return price;
    }
    public void setTotalPrice(String totalPrice) 
    {
        this.totalPrice = totalPrice;
    }

    public String getTotalPrice() 
    {
        return totalPrice;
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
            .append("spuId", getSpuId())
            .append("skuId", getSkuId())
            .append("skuCode", getSkuCode())
            .append("skuName", getSkuName())
            .append("spuName", getSpuName())
            .append("mainImage", getMainImage())
            .append("attrsJson", getAttrsJson())
            .append("quantity", getQuantity())
            .append("price", getPrice())
            .append("totalPrice", getTotalPrice())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
