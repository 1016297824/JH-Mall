package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 商品 SKU DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@TableName("mall_product_sku")
public class MallProductSkuDO {

    /** SKU ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属 SPU ID */
    @TableField("spu_id")
    private Long spuId;

    /** SKU 编码 */
    @TableField("sku_code")
    private String skuCode;

    /** SKU 销售名称 */
    @TableField("sku_name")
    private String skuName;

    /** 销售属性 JSON */
    @TableField("attrs_json")
    private String attrsJson;

    /** 销售价（分） */
    @TableField("price")
    private Long price;

    /** 市场价/划线价（分） */
    @TableField("market_price")
    private Long marketPrice;

    /** 成本价（分） */
    @TableField("cost_price")
    private Long costPrice;

    /** SKU 图片 URL */
    @TableField("image")
    private String image;

    /** 重量（克） */
    @TableField("weight")
    private Integer weight;

    /** 销量 */
    @TableField("sales_count")
    private Integer salesCount;

    /** 逻辑删除标识（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getAttrsJson() { return attrsJson; }
    public void setAttrsJson(String attrsJson) { this.attrsJson = attrsJson; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Long getMarketPrice() { return marketPrice; }
    public void setMarketPrice(Long marketPrice) { this.marketPrice = marketPrice; }
    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
