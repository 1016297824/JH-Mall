package com.mall.search.vo;

import lombok.Data;
import java.util.List;

/**
 * 聚合统计 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class AggregationVO {
    /** 类目聚合 */
    private List<AggregationBucket> categories;
    /** 品牌聚合 */
    private List<AggregationBucket> brands;
    /** 价格区间聚合 */
    private List<AggregationBucket> priceRanges;

    @Data
    public static class AggregationBucket {
        /** 聚合值 */
        private String key;
        /** 聚合名称 */
        private String name;
        /** 文档数量 */
        private long count;
    }
}
