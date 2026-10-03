package com.mall.common.DTO.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品 SKU DTO
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuDTO {
    /** SKU ID */
    private Long skuId;
    /** 所属 SPU ID */
    private Long spuId;
    /** SKU 编码 */
    private String skuCode;
    /** SKU 销售名称 */
    private String skuName;
    /**
     * 所属 SPU 名称
     *
     * <p>下单时 {@code mall_order_item.spu_name} 是 NOT NULL 的快照字段，
     * 调用方需要它；由 mall-product 批量查询时一并带出，避免调用方再发一次 SPU 查询。</p>
     */
    private String spuName;
    /** 销售价（分） */
    private Long price;
    /** SKU 图片 */
    private String image;
    /** 是否在售 */
    private Boolean isOnSale;
    /** 可用库存 */
    private Integer availableQty;
}
