package com.mall.product.DO;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 库存管理对象 mall_product_sku_stock
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallProductSkuStock extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** SKU ID，与 SKU 一对一 */
    @Excel(name = "SKU ID，与 SKU 一对一")
    private String skuId;

    /** 总库存 = 可用 + 锁定 + 已售 + 冻结 */
    @Excel(name = "总库存 = 可用 + 锁定 + 已售 + 冻结")
    private String totalStock;

    /** 可用库存 */
    @Excel(name = "可用库存")
    private String availableStock;

    /** 锁定库存 */
    @Excel(name = "锁定库存")
    private String lockedStock;

    /** 已售库存 */
    @Excel(name = "已售库存")
    private String soldStock;

    /** 冻结库存 */
    @Excel(name = "冻结库存")
    private String frozenStock;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号，库存扣减防超卖 */
    @Excel(name = "乐观锁版本号，库存扣减防超卖")
    private String version;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setSkuId(String skuId)
    {
        this.skuId = skuId;
    }

    public String getSkuId()
    {
        return skuId;
    }

    public void setTotalStock(String totalStock)
    {
        this.totalStock = totalStock;
    }

    public String getTotalStock()
    {
        return totalStock;
    }

    public void setAvailableStock(String availableStock)
    {
        this.availableStock = availableStock;
    }

    public String getAvailableStock()
    {
        return availableStock;
    }

    public void setLockedStock(String lockedStock)
    {
        this.lockedStock = lockedStock;
    }

    public String getLockedStock()
    {
        return lockedStock;
    }

    public void setSoldStock(String soldStock)
    {
        this.soldStock = soldStock;
    }

    public String getSoldStock()
    {
        return soldStock;
    }

    public void setFrozenStock(String frozenStock)
    {
        this.frozenStock = frozenStock;
    }

    public String getFrozenStock()
    {
        return frozenStock;
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
            .append("skuId", getSkuId())
            .append("totalStock", getTotalStock())
            .append("availableStock", getAvailableStock())
            .append("lockedStock", getLockedStock())
            .append("soldStock", getSoldStock())
            .append("frozenStock", getFrozenStock())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .toString();
    }
}
