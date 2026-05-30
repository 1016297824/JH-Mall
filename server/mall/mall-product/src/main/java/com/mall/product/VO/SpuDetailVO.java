package com.mall.product.VO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SpuDetailVO extends SpuVO {
    /** 详情 HTML */
    private String description;
    /** 轮播图 URL 列表 */
    private List<String> images;
    /** 评价条数 */
    private Integer reviewCount;
    /** SKU 简要列表 */
    private List<SkuBriefVO> skus;
}
