package com.mall.search.convert.response;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.vo.AggregationVO;
import com.mall.search.vo.SearchItemVO;
import com.mall.search.vo.SearchResultVO;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 搜索转换器
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public final class SearchConvert {

    private static final BigDecimal PRICE_DIVISOR = new BigDecimal("100");

    private SearchConvert() {
    }

    /**
     * 将 ES 搜索结果转换为 {@link SearchResultVO}
     *
     * @param hits  ES 搜索结果
     * @param page  当前页码
     * @param size  每页条数
     * @return 搜索结果 VO
     */
    public static SearchResultVO toSearchResultVO(SearchHits<ProductIndexDO> hits, int page, int size) {
        List<SearchItemVO> items = new ArrayList<>();
        for (SearchHit<ProductIndexDO> hit : hits) {
            items.add(toSearchItemVO(hit));
        }
        SearchResultVO result = new SearchResultVO();
        result.setItems(items);
        result.setTotal(hits.getTotalHits());
        result.setPage(page);
        result.setSize(size);

        // 提取聚合数据（categories / brands）
        if (hits.hasAggregations()) {
            ElasticsearchAggregations esAggs = (ElasticsearchAggregations) hits.getAggregations();
            AggregationVO aggVO = new AggregationVO();
            aggVO.setCategories(extractTermBuckets(esAggs, "categories"));
            aggVO.setBrands(extractTermBuckets(esAggs, "brands"));
            result.setAggregations(aggVO);
        }

        return result;
    }

    /**
     * 将单个命中记录转换为 {@link SearchItemVO}
     *
     * @param hit ES 命中记录
     * @return 搜索结果条目 VO
     */
    public static SearchItemVO toSearchItemVO(SearchHit<ProductIndexDO> hit) {
        ProductIndexDO source = hit.getContent();
        SearchItemVO vo = new SearchItemVO();
        vo.setSpuId(source.getProductId());
        vo.setSpuName(source.getSpuName());
        vo.setImage(source.getImage());
        vo.setSalesCount(source.getSalesCount());
        // 价格：分 → 元，保留两位小数
        if (source.getPrice() != null) {
            vo.setPrice(BigDecimal.valueOf(source.getPrice())
                    .divide(PRICE_DIVISOR, 2, RoundingMode.HALF_UP)
                    .toPlainString());
        }
        // 高亮字段提取
        List<String> highlights = hit.getHighlightField("spuName");
        if (highlights != null && !highlights.isEmpty()) {
            vo.setSpuNameHighlight(highlights.get(0));
        }
        return vo;
    }

    /**
     * 从 ES 聚合结果中提取指定名称的 terms 桶列表
     *
     * @param esAggs  ES 聚合容器
     * @param aggName 聚合名称
     * @return 聚合桶列表，聚合不存在时返回空列表
     */
    private static List<AggregationVO.AggregationBucket> extractTermBuckets(
            ElasticsearchAggregations esAggs, String aggName) {
        ElasticsearchAggregation esAgg = esAggs.get(aggName);
        if (esAgg == null) {
            return Collections.emptyList();
        }
        Aggregate aggregate = esAgg.aggregation().getAggregate();
        if (!aggregate.isLterms()) {
            return Collections.emptyList();
        }
        LongTermsAggregate lterms = aggregate.lterms();
        Buckets<LongTermsBucket> buckets = lterms.buckets();
        if (!buckets.isArray()) {
            return Collections.emptyList();
        }
        List<LongTermsBucket> bucketList = buckets.array();
        List<AggregationVO.AggregationBucket> result = new ArrayList<>(bucketList.size());
        for (LongTermsBucket bucket : bucketList) {
            AggregationVO.AggregationBucket b = new AggregationVO.AggregationBucket();
            b.setKey(String.valueOf(bucket.key()));
            b.setCount(bucket.docCount());
            // 从 top_hits 子聚合提取名称
            extractNameFromTopHits(b, bucket.aggregations());
            result.add(b);
        }
        return result;
    }

    /**
     * 从 top_hits 子聚合中提取名称字段（categoryName 或 brandName）
     */
    private static void extractNameFromTopHits(AggregationVO.AggregationBucket bucket,
                                                Map<String, Aggregate> subAggs) {
        Aggregate topHit = subAggs.get("top_hit");
        if (topHit == null || !topHit.isTopHits()) {
            return;
        }
        var hits = topHit.topHits().hits().hits();
        if (hits.isEmpty()) {
            return;
        }
        var source = hits.get(0).source();
        if (source == null) {
            return;
        }
        // source 类型为 JsonData，转 Jakarta JsonObject 取字段
        var node = source.toJson().asJsonObject();
        if (node.containsKey("categoryName")) {
            bucket.setName(node.get("categoryName").toString().replace("\"", ""));
        } else if (node.containsKey("brandName")) {
            bucket.setName(node.get("brandName").toString().replace("\"", ""));
        }
    }
}
