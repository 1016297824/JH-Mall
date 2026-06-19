package com.mall.search.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.api.feign.RemoteProductService;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.DTO.request.SearchReqDTO;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.convert.response.SearchConvert;
import com.mall.search.service.SearchService;
import com.mall.search.vo.SearchItemVO;
import com.mall.search.vo.SearchResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;

/**
 * C 端商品搜索服务实现
 *
 * <p>基于 Elasticsearch 进行全文检索，使用 Caffeine 缓存搜索结果。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations operations;
    private final MallSearchConfigProperties configProperties;
    private final RemoteProductService remoteProductService;
    private final Cache<String, SearchResultVO> searchCache;

    public SearchServiceImpl(ElasticsearchOperations operations, MallSearchConfigProperties configProperties,
                              RemoteProductService remoteProductService) {
        this.operations = operations;
        this.configProperties = configProperties;
        this.remoteProductService = remoteProductService;
        this.searchCache = Caffeine.newBuilder()
                .expireAfterWrite(configProperties.getResult().getCacheTtl(), TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    @Override
    public SearchResultVO search(SearchReqDTO req) {
        validateSearchRequest(req);

        String cacheKey = buildCacheKey(req);
        SearchResultVO cached = searchCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        NativeQuery query = buildQuery(req);
        SearchHits<ProductIndexDO> hits;
        try {
            hits = operations.search(query, ProductIndexDO.class);
        } catch (ElasticsearchException e) {
            log.error("ES 搜索服务不可用，尝试降级", e);
            // Double-check cache before fallback
            cached = searchCache.getIfPresent(cacheKey);
            if (cached != null) {
                return cached;
            }
            try {
                int fallbackPage = req.getPage() != null ? req.getPage() : 1;
                int fallbackSize = req.getSize() != null ? req.getSize() : 20;
                return fallbackSearch(req.getKeyword(), fallbackPage, fallbackSize, cacheKey);
            } catch (Exception fallbackEx) {
                log.error("搜索降级也失败", fallbackEx);
                throw new BusinessException(ErrorCode.ES_UNAVAILABLE);
            }
        }

        int page = req.getPage() != null ? req.getPage() : 1;
        int size = req.getSize() != null ? req.getSize() : 20;
        SearchResultVO result = SearchConvert.toSearchResultVO(hits, page, size);

        searchCache.put(cacheKey, result);
        return result;
    }

    /**
     * 参数校验
     *
     * @param req 搜索请求
     */
    private void validateSearchRequest(SearchReqDTO req) {
        int maxSize = configProperties.getPage().getMaxSize();
        int size = req.getSize() != null ? req.getSize() : 20;
        if (size > maxSize) {
            throw new BusinessException(ErrorCode.SEARCH_PARAM_ERROR);
        }

        int maxDepth = configProperties.getPage().getMaxDepth();
        int page = req.getPage() != null ? req.getPage() : 1;
        int from = (page - 1) * size;
        if (from + size > maxDepth) {
            throw new BusinessException(ErrorCode.SEARCH_RESULT_LIMIT);
        }
    }

    /**
     * 构建 ES 搜索查询
     *
     * <p>multi_match(spuName^3/subTitle^1.5)
     * + filter(isOnSale=true/categoryId/brandId/price range)
     * + sort + highlight(spuName/&lt;em&gt;) + 分页 + aggregation(categories/brands)</p>
     *
     * @param req 搜索请求
     * @return ES 原生查询
     */
    private NativeQuery buildQuery(SearchReqDTO req) {
        int page = req.getPage() != null ? req.getPage() : 1;
        int size = req.getSize() != null ? req.getSize() : 20;

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildEsQuery(req))
                .withPageable(PageRequest.of(page - 1, size))
                .withTimeout(Duration.ofSeconds(2));

        // 排序
        List<SortOptions> sorts = buildSorts(req);
        if (!sorts.isEmpty()) {
            builder = builder.withSort(sorts);
        }

        // 高亮
        builder = builder.withHighlightQuery(buildHighlightQuery());

        // 聚合（带 top_hits 子聚合获取名称）
        builder = builder.withAggregation("categories",
                        Aggregation.of(a -> a.terms(t -> t.field("categoryId").size(50))
                                .aggregations("top_hit", Aggregation.of(a2 -> a2.topHits(th -> th
                                        .size(1).source(src -> src.filter(f -> f.includes("categoryName"))))))))
                .withAggregation("brands",
                        Aggregation.of(a -> a.terms(t -> t.field("brandId").size(50))
                                .aggregations("top_hit", Aggregation.of(a2 -> a2.topHits(th -> th
                                        .size(1).source(src -> src.filter(f -> f.includes("brandName"))))))));
        return builder.build();
    }

    /**
     * 构建 ES Query
     *
     * @param req 搜索请求
     * @return ES Query
     */
    private Query buildEsQuery(SearchReqDTO req) {
        List<Query> filterQueries = new ArrayList<>();

        // 仅上架商品
        filterQueries.add(Query.of(q -> q.term(t -> t
                .field("isOnSale")
                .value(FieldValue.of(true)))));

        // 类目过滤（多选）
        if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
            filterQueries.add(Query.of(q -> q.terms(t -> t
                    .field("categoryId")
                    .terms(terms -> terms.value(
                            req.getCategoryIds().stream().map(FieldValue::of).toList())))));
        }

        // 品牌过滤（多选）
        if (req.getBrandIds() != null && !req.getBrandIds().isEmpty()) {
            filterQueries.add(Query.of(q -> q.terms(t -> t
                    .field("brandId")
                    .terms(terms -> terms.value(
                            req.getBrandIds().stream().map(FieldValue::of).toList())))));
        }

        // 价格区间过滤（前端已转分为单位），使用 number range
        if (req.getPriceMin() != null || req.getPriceMax() != null) {
            Double gte = req.getPriceMin() != null ? (double) req.getPriceMin() : null;
            Double lte = req.getPriceMax() != null ? (double) req.getPriceMax() : null;
            filterQueries.add(Query.of(q -> q.range(r -> r
                    .number(n -> n.field("price").gte(gte).lte(lte)))));
        }

        // 多字段匹配查询
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            Query multiMatch = Query.of(q -> q.multiMatch(m -> m
                    .query(req.getKeyword())
                    .fields("spuName^3", "subTitle^1.5")
                    .type(TextQueryType.BestFields)));
            return Query.of(q -> q.bool(b -> b
                    .must(multiMatch)
                    .filter(filterQueries)));
        }

        // 无关键词时 match_all
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.matchAll(ma -> ma))
                .filter(filterQueries)));
    }

    /**
     * 构建排序
     *
     * @param req 搜索请求
     * @return 排序列表
     */
    private List<SortOptions> buildSorts(SearchReqDTO req) {
        List<SortOptions> sorts = new ArrayList<>();
        if (req.getSort() != null && !req.getSort().isEmpty()) {
            SortOrder order = "ASC".equalsIgnoreCase(req.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
            sorts.add(SortOptions.of(s -> s.field(f -> f
                    .field(req.getSort())
                    .order(order))));
        }
        return sorts;
    }

    /**
     * 构建高亮查询
     *
     * @return HighlightQuery
     */
    private HighlightQuery buildHighlightQuery() {
        HighlightFieldParameters fieldParams = HighlightFieldParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();
        HighlightField highlightField = new HighlightField("spuName", fieldParams);
        Highlight highlight = new Highlight(List.of(highlightField));
        return new HighlightQuery(highlight, ProductIndexDO.class);
    }

    /**
     * 构建缓存 Key
     *
     * @param req 搜索请求
     * @return 缓存 Key 字符串
     */
    private String buildCacheKey(SearchReqDTO req) {
        StringBuilder sb = new StringBuilder("search:");
        sb.append(nullToEmpty(req.getKeyword())).append(':');
        sb.append(req.getCategoryIds()).append(':');
        sb.append(req.getBrandIds()).append(':');
        sb.append(req.getPriceMin()).append(':');
        sb.append(req.getPriceMax()).append(':');
        sb.append(nullToEmpty(req.getSort())).append(':');
        sb.append(nullToEmpty(req.getSortOrder())).append(':');
        sb.append(defaultPage(req.getPage())).append(':');
        sb.append(defaultSize(req.getSize()));
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static int defaultPage(Integer page) {
        return page != null ? page : 1;
    }

    private static int defaultSize(Integer size) {
        return size != null ? size : 20;
    }

    /**
     * ES 降级搜索，通过 Feign 调用 mall-product DB 兜底查询
     *
     * @param keyword  搜索关键词
     * @param page     页码
     * @param size     每页条数
     * @param cacheKey 缓存 Key
     * @return 搜索结果
     */
    private SearchResultVO fallbackSearch(String keyword, int page, int size, String cacheKey) {
        Map<String, Object> response = remoteProductService.searchFallback(keyword, page, size);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
        if (dataList == null || dataList.isEmpty()) {
            SearchResultVO empty = new SearchResultVO();
            empty.setItems(new ArrayList<>());
            empty.setTotal(0);
            empty.setPage(page);
            empty.setSize(size);
            return empty;
        }
        List<SearchItemVO> items = new ArrayList<>(dataList.size());
        for (Map<String, Object> spu : dataList) {
            items.add(mapToSearchItemVO(spu));
        }
        SearchResultVO result = new SearchResultVO();
        result.setItems(items);
        result.setTotal(items.size());
        result.setPage(page);
        result.setSize(size);
        searchCache.put(cacheKey, result);
        return result;
    }

    /**
     * 将 SpuVO JSON Map 转换为 SearchItemVO
     *
     * @param spu SpuVO 的 Map 表示
     * @return SearchItemVO
     */
    private SearchItemVO mapToSearchItemVO(Map<String, Object> spu) {
        SearchItemVO vo = new SearchItemVO();
        Object spuIdObj = spu.get("spuId");
        if (spuIdObj instanceof String) {
            vo.setSpuId(Long.valueOf((String) spuIdObj));
        }
        vo.setSpuName((String) spu.get("spuName"));
        vo.setImage((String) spu.get("mainImage"));
        // 价格：分 → 元
        Object priceObj = spu.get("priceMin");
        if (priceObj instanceof Number) {
            vo.setPrice(BigDecimal.valueOf(((Number) priceObj).longValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .toPlainString());
        }
        Object salesObj = spu.get("salesCount");
        if (salesObj instanceof Number) {
            vo.setSalesCount(((Number) salesObj).intValue());
        }
        return vo;
    }
}
