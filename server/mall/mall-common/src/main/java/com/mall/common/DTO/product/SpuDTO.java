package com.mall.common.DTO.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品 SPU DTO
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpuDTO {
    /** SPU ID */
    private Long spuId;
    /** SPU 名称 */
    private String spuName;
    /** 主图 */
    private String mainImage;
    /** 最低价（分） */
    private Long priceMin;
    /** 最高价（分） */
    private Long priceMax;
    /** 上下架状态 */
    private Integer publishStatus;
    /** 累计销量 */
    private Integer salesCount;
    /** 类目 ID */
    private Long categoryId;
    /** 品牌 ID */
    private Long brandId;
}
