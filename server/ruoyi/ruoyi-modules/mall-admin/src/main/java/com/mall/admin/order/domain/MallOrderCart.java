package com.mall.admin.order.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 购物车对象 mall_order_cart
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallOrderCart extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 用户 ID */
    @Excel(name = "用户 ID")
    private String userId;

    /** SKU ID */
    @Excel(name = "SKU ID")
    private String skuId;

    /** SPU ID */
    @Excel(name = "SPU ID")
    private String spuId;

    /** SKU 编码（冗余，快速展示） */
    @Excel(name = "SKU 编码", readConverterExp = "冗=余，快速展示")
    private String skuCode;

    /** SKU 销售名称（冗余） */
    @Excel(name = "SKU 销售名称", readConverterExp = "冗=余")
    private String skuName;

    /** 商品主图 URL（冗余） */
    @Excel(name = "商品主图 URL", readConverterExp = "冗=余")
    private String mainImage;

    /** 当前销售价（单位：分），加购时快照 */
    @Excel(name = "当前销售价", readConverterExp = "单=位：分")
    private String price;

    /** 加入数量 */
    @Excel(name = "加入数量")
    private String quantity;

    /** 是否选中 */
    @Excel(name = "是否选中")
    private String isSelected;

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

    public void setSkuId(String skuId)
    {
        this.skuId = skuId;
    }

    public String getSkuId()
    {
        return skuId;
    }

    public void setSpuId(String spuId)
    {
        this.spuId = spuId;
    }

    public String getSpuId()
    {
        return spuId;
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

    public void setMainImage(String mainImage)
    {
        this.mainImage = mainImage;
    }

    public String getMainImage()
    {
        return mainImage;
    }

    public void setPrice(String price)
    {
        this.price = price;
    }

    public String getPrice()
    {
        return price;
    }

    public void setQuantity(String quantity)
    {
        this.quantity = quantity;
    }

    public String getQuantity()
    {
        return quantity;
    }

    public void setIsSelected(String isSelected)
    {
        this.isSelected = isSelected;
    }

    public String getIsSelected()
    {
        return isSelected;
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
            .append("skuId", getSkuId())
            .append("spuId", getSpuId())
            .append("skuCode", getSkuCode())
            .append("skuName", getSkuName())
            .append("mainImage", getMainImage())
            .append("price", getPrice())
            .append("quantity", getQuantity())
            .append("isSelected", getIsSelected())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
