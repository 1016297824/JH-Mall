package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 简要 VO（SPU 详情中展示 SKU 列表使用，仅含价位图片）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
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
