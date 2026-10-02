package com.mall.order.VO;

import lombok.Data;

/**
 * 购物车项视图对象
 *
 * <p>价格单位为分。</p>
 *
 * <p>{@code availableQty} 与 {@code isOnSale} 由mall-product 实时返回，
 * 不落库，仅用于前端灰显与超量提示（设计文档 §4.5）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class CartVO {

    /** 购物车项 ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** 所属 SPU ID */
    private Long spuId;

    /** SKU 编码 */
    private String skuCode;

    /** SKU 销售名称 */
    private String skuName;

    /** 商品主图 URL */
    private String mainImage;

    /**
     * 展示价格（单位：分）
     *
     * <p>优先取 mall-product 的实时价；Feign 降级时回退为购物车表冗余价格。</p>
     */
    private Long price;

    /** 购买数量 */
    private Integer quantity;

    /** 是否选中，1=选中 0=未选 */
    private Integer isSelected;

    /** 实时可用库存，-1 表示实时数据不可用（Feign 降级） */
    private Integer availableQty;

    /** 是否在售；{@code null} 表示实时数据不可用（Feign 降级） */
    private Boolean onSale;

    /**
     * 该项是否可结算
     *
     * <p>在售、有库存、数量不超库存 三者同时满足。用于前端禁用下单按钮。</p>
     */
    private Boolean purchasable;
}