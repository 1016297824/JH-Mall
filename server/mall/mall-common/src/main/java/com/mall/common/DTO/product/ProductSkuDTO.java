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
    /** 销售价（分） */
    private Long price;
    /** SKU 图片 */
    private String image;
    /** 是否在售 */
    private Boolean isOnSale;
    /** 可用库存 */
    private Integer availableQty;
}
