package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SpuVO {
    /** SPU ID */
    private String spuId;
    /** SPU 名称 */
    private String spuName;
    /** 主图 URL */
    private String mainImage;
    /** 最低价（分） */
    private Long priceMin;
    /** 最高价（分） */
    private Long priceMax;
    /** 累计销量 */
    private Integer salesCount;
    /** 所属类目 ID */
    private String categoryId;
    /** 所属品牌 ID */
    private String brandId;
}
