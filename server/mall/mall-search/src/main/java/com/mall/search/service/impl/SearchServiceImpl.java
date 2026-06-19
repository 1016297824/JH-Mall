package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.DTO.request.SearchReqDTO;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.convert.response.SearchConvert;
import com.mall.search.service.SearchService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final Cache<String, SearchResultVO> searchCache;

    public SearchServiceImpl(ElasticsearchOperations operations, MallSearchConfigProperties configProperties) {
        this.operations = operations;
        this.configProperties = configProperties;
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
            log.error("ES 搜索服务不可用", e);
            throw new BusinessException(ErrorCode.ES_UNAVAILABLE);
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
     * <p>multi_match(spuName^3/subTitle^1.5/spuSpecs^1.0)
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
                .withPageable(PageRequest.of(page - 1, size));

        // 排序
        List<SortOptions> sorts = buildSorts(req);
        if (!sorts.isEmpty()) {
            builder = builder.withSort(sorts);
        }

        // 高亮
        builder = builder.withHighlightQuery(buildHighlightQuery());

        // 聚合
        builder = builder.withAggregation("categories",
                        Aggregation.of(a -> a.terms(t -> t.field("categoryId"))))
                .withAggregation("brands",
                        Aggregation.of(a -> a.terms(t -> t.field("brandId"))));

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

        // 类目过滤
        if (req.getCategoryId() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t
                    .field("categoryId")
                    .value(FieldValue.of(req.getCategoryId())))));
        }

        // 品牌过滤
        if (req.getBrandId() != null) {
            filterQueries.add(Query.of(q -> q.term(t -> t
                    .field("brandId")
                    .value(FieldValue.of(req.getBrandId())))));
        }

        // 价格区间过滤（单位：分），使用 number range
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
                    .fields("spuName^3", "subTitle^1.5", "spuSpecs^1.0")));
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
        sb.append(req.getKeyword()).append(':');
        sb.append(req.getCategoryId()).append(':');
        sb.append(req.getBrandId()).append(':');
        sb.append(req.getPriceMin()).append(':');
        sb.append(req.getPriceMax()).append(':');
        sb.append(req.getSort()).append(':');
        sb.append(req.getSortOrder()).append(':');
        sb.append(req.getPage()).append(':');
        sb.append(req.getSize());
        return sb.toString();
    }
}
