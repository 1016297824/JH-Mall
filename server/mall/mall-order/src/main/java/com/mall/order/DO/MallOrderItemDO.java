package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 订单项 DO
 *
 * <p>对应表 {@code mall_order_item}。商品名称、图片、售价均为<strong>下单时刻快照</strong>，
 * 后续商品变更不影响历史订单。</p>
 *
 * <p>表无 version 列，故不做乐观锁。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order_item")
public class MallOrderItemDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    @TableField("order_id")
    private Long orderId;

    /** SPU ID（快照） */
    @TableField("spu_id")
    private Long spuId;

    /** SKU ID（快照） */
    @TableField("sku_id")
    private Long skuId;

    /** SKU 编码（快照） */
    @TableField("sku_code")
    private String skuCode;

    /** SKU 名称（快照） */
    @TableField("sku_name")
    private String skuName;

    /** SPU 名称（快照） */
    @TableField("spu_name")
    private String spuName;

    /** 商品主图快照 URL */
    @TableField("main_image")
    private String mainImage;

    /** 销售属性 JSON 快照 */
    @TableField("attrs_json")
    private String attrsJson;

    /** 购买数量 */
    @TableField("quantity")
    private Integer quantity;

    /** 成交单价（单位：分） */
    @TableField("price")
    private Long price;

    /** 单项总价（单位：分） */
    @TableField("total_price")
    private Long totalPrice;

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