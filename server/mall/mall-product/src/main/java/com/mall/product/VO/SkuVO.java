package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 详情 VO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
public class SkuVO {
    /** SKU ID */
    private String skuId;
    /** 所属 SPU ID */
    private String spuId;
    /** SKU 编码 */
    private String skuCode;
    /** SKU 销售名称 */
    private String skuName;
    /** 销售属性 JSON */
    private String attrsJson;
    /** 销售价（分） */
    private Long price;
    /** 市场价/划线价（分） */
    private Long marketPrice;
    /** SKU 图片 */
    private String image;
    /** 重量（克） */
    private Integer weight;
    /** 可用库存 */
    private Integer availableStock;
}
