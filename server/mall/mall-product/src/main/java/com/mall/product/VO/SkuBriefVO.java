package com.mall.product.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SKU 简要 VO（SPU 详情中展示 SKU 列表使用，含价位图片和销售属性）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
public class SkuBriefVO {
    /** SKU ID */
    private String skuId;
    /** SKU 编码（货号） */
    private String skuCode;
    /** SKU 名称 */
    private String skuName;
    /** 销售属性列表 */
    private List<SkuAttrVO> attrs;
    /** 销售价（分） */
    private Long price;
    /** SKU 图片 */
    private String image;
}
