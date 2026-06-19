package com.mall.search.DTO.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C 端商品搜索请求
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@NoArgsConstructor
public class SearchReqDTO {

    /** 搜索关键词 */
    private String keyword;

    /** 类目 ID 过滤 */
    private Long categoryId;

    /** 品牌 ID 过滤 */
    private Long brandId;

    /** 价格区间下限（分） */
    private Integer priceMin;

    /** 价格区间上限（分） */
    private Integer priceMax;

    /** 标签过滤 */
    private String[] tags;

    /** 排序字段：_score / salesCount / price / createTime */
    private String sort;

    /** 排序方向：ASC / DESC，默认 DESC */
    private String sortOrder;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数，默认 20，最大 60 */
    private Integer size = 20;
}
