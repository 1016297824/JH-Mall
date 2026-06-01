package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * SKU 库存 DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@TableName("mall_product_sku_stock")
public class MallSkuStockDO {
    /** 库存记录 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** SKU ID（一对一关系，每个 SKU 对应一条库存记录） */
    @TableField("sku_id")
    private Long skuId;
    /** 总库存 = 可用 + 锁定 + 已售 + 冻结 */
    @TableField("total_stock")
    private Integer totalStock;
    /** 可用库存（可销售库存，预扣时减此值、加 locked） */
    @TableField("available_stock")
    private Integer availableStock;
    /** 锁定库存（已下单未支付，订单超时释放回 available） */
    @TableField("locked_stock")
    private Integer lockedStock;
    /** 已售库存（订单支付完成时累加，不参与库存扣减运算） */
    @TableField("sold_stock")
    private Integer soldStock;
    /** 冻结库存（售后/维权处理中，暂不可售） */
    @TableField("frozen_stock")
    private Integer frozenStock;
    /** 逻辑删除标识（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;
    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
    /** 乐观锁版本号（每次更新 version+1，用于并发安全扣减） */
    @Version
    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public Integer getLockedStock() { return lockedStock; }
    public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
    public Integer getSoldStock() { return soldStock; }
    public void setSoldStock(Integer soldStock) { this.soldStock = soldStock; }
    public Integer getFrozenStock() { return frozenStock; }
    public void setFrozenStock(Integer frozenStock) { this.frozenStock = frozenStock; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
