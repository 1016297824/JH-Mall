package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 购物车项 DO
 *
 * <p>对应表 {@code mall_order_cart}。同一用户同一 SKU 唯一（{@code uk_user_sku}）。</p>
 *
 * <p>价格单位为分。表无version 列，故不做乐观锁。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order_cart")
public class MallCartDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** SKU ID */
    @TableField("sku_id")
    private Long skuId;

    /** SPU ID */
    @TableField("spu_id")
    private Long spuId;

    /** SKU 编码（冗余，列表展示用） */
    @TableField("sku_code")
    private String skuCode;

    /** SKU 销售名称（冗余） */
    @TableField("sku_name")
    private String skuName;

    /** 商品主图 URL（冗余） */
    @TableField("main_image")
    private String mainImage;

    /**
     * 当前销售价（单位：分）
     *
     * <p>冗余字段。实时价以 mall-product 的 batchGetSku 为准，
     * 该字段仅在 Feign 降级时兜底展示（设计文档 §4.5）。</p>
     */
    @TableField("price")
    private Long price;

    /** 加入数量 */
    @TableField("quantity")
    private Integer quantity;

    /** 是否选中，1=选中 0=未选（下单只处理已选项） */
    @TableField("is_selected")
    private Integer isSelected;

    /** 逻辑删除标志（0=未删，1=已删） */
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