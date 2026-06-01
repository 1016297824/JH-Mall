package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SPU 列表 VO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
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
    /** 热度分（Redis ZSet score，不持久化到 MySQL） */
    private Long hotScore;
    /** 所属类目 ID */
    private String categoryId;
    /** 所属品牌 ID */
    private String brandId;
}
