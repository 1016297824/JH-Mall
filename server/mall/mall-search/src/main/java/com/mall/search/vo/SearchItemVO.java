package com.mall.search.vo;

import lombok.Data;

/**
 * 搜索结果条目 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class SearchItemVO {
    /** SPU ID */
    private Long spuId;
    /** 商品名称 */
    private String spuName;
    /** 商品名称高亮片段 */
    private String spuNameHighlight;
    /** 最低售价（元，已除以100） */
    private String price;
    /** 商品主图 */
    private String image;
    /** 累计销量 */
    private Integer salesCount;
}
