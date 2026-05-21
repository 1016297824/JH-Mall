package com.mall.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * SKU 管理对象 mall_product_sku
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallProductSku extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 所属 SPU ID */
    @Excel(name = "所属 SPU ID")
    private String spuId;

    /** SKU 编码 */
    @Excel(name = "SKU 编码")
    private String skuCode;

    /** SKU 销售名称 */
    @Excel(name = "SKU 销售名称")
    private String skuName;

    /** 销售属性 */
    @Excel(name = "销售属性")
    private String attrsJson;

    /** 销售价（单位：分） */
    @Excel(name = "销售价（单位：分）")
    private String price;

    /** 市场价/划线价（单位：分） */
    @Excel(name = "市场价/划线价（单位：分）")
    private String marketPrice;

    /** 成本价（单位：分） */
    private String costPrice;

    /** SKU 级图片 */
    @Excel(name = "SKU 级图片")
    private String image;

    /** 重量（单位：克） */
    @Excel(name = "重量（单位：克）")
    private String weight;

    /** 累计销量 */
    @Excel(name = "累计销量")
    private String salesCount;

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
    public void setAttrsJson(String attrsJson) 
    {
        this.attrsJson = attrsJson;
    }

    public String getAttrsJson() 
    {
        return attrsJson;
    }
    public void setPrice(String price) 
    {
        this.price = price;
    }

    public String getPrice() 
    {
        return price;
    }
    public void setMarketPrice(String marketPrice) 
    {
        this.marketPrice = marketPrice;
    }

    public String getMarketPrice() 
    {
        return marketPrice;
    }
    public void setCostPrice(String costPrice) 
    {
        this.costPrice = costPrice;
    }

    public String getCostPrice() 
    {
        return costPrice;
    }
    public void setImage(String image) 
    {
        this.image = image;
    }

    public String getImage() 
    {
        return image;
    }
    public void setWeight(String weight) 
    {
        this.weight = weight;
    }

    public String getWeight() 
    {
        return weight;
    }
    public void setSalesCount(String salesCount) 
    {
        this.salesCount = salesCount;
    }

    public String getSalesCount() 
    {
        return salesCount;
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
            .append("spuId", getSpuId())
            .append("skuCode", getSkuCode())
            .append("skuName", getSkuName())
            .append("attrsJson", getAttrsJson())
            .append("price", getPrice())
            .append("marketPrice", getMarketPrice())
            .append("costPrice", getCostPrice())
            .append("image", getImage())
            .append("weight", getWeight())
            .append("salesCount", getSalesCount())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
