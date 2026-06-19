package com.mall.search.vo;

import lombok.Data;
import java.util.List;

/**
 * 搜索结果 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class SearchResultVO {
    /** 搜索结果列表 */
    private List<SearchItemVO> items;
    /** 聚合统计 */
    private AggregationVO aggregations;
    /** 总命中数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页条数 */
    private int size;
}
