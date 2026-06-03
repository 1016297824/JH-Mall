package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 商品 SKU DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
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

    /** 销售属性 JSON（如 [{"k":"颜色","v":"红色"},{"k":"尺寸","v":"XL"}]） */
    @TableField("attrs_json")
    private String attrsJson;

    /** 销售价（分，C 端实际售价） */
    @TableField("price")
    private Long price;

    /** 市场价/划线价（分，展示原价用于对比，高于销售价） */
    @TableField("market_price")
    private Long marketPrice;

    /** 成本价（分，内部核算用，不对外展示） */
    @TableField("cost_price")
    private Long costPrice;

    /** SKU 图片 URL */
    @TableField("image")
    private String image;

    /** 重量（克，用于运费计算） */
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
