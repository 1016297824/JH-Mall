package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SkuBriefVO {
    /** SKU ID */
    private String skuId;
    /** SKU 名称 */
    private String skuName;
    /** 销售价（分） */
    private Long price;
    /** SKU 图片 */
    private String image;
}
