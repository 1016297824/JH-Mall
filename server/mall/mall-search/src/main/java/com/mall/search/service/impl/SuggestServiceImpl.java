package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.CacheConstants;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.service.SuggestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * C 端搜索建议服务实现
 *
 * <p>基于 Elasticsearch Completion Suggester 实现搜索关键词建议。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestServiceImpl implements SuggestService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final MallSearchConfigProperties configProperties;

    @Override
    public List<String> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        // 先查 Redis 缓存
        String cacheKey = CacheConstants.Search.SUGGEST + keyword;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return OBJECT_MAPPER.readValue(cached, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("搜索建议缓存反序列化失败，回源 ES: keyword={}", keyword, e);
            }
        }
        // ES completion suggester
        List<String> result = querySuggestFromEs(keyword);
        // 写入缓存
        if (!result.isEmpty()) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(result);
                stringRedisTemplate.opsForValue().set(cacheKey, json,
                        configProperties.getSuggest().getCacheTtl(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("搜索建议缓存写入失败: keyword={}", keyword, e);
            }
        }
        return result;
    }

    /**
     * 调用 ES completion suggester 查询建议词
     *
     * @param keyword 搜索关键词
     * @return 建议词列表，无结果时返回空列表
     */
    private List<String> querySuggestFromEs(String keyword) {
        try {
            var response = elasticsearchClient.search(s -> s
                            .index("mall_product")
                            .suggest(sg -> sg
                                    .text(keyword)
                                    .suggesters("product_suggest", FieldSuggester.of(fs -> fs
                                            .completion(cs -> cs
                                                    .field("suggest")
                                                    .size(10)
                                                    .skipDuplicates(true)
                                            )))
                            ),
                    Void.class
            );
            Map<String, List<Suggestion<Void>>> suggestMap = response.suggest();
            List<Suggestion<Void>> suggestions = suggestMap.get("product_suggest");
            if (suggestions == null || suggestions.isEmpty()) {
                return List.of();
            }
            var suggest = suggestions.get(0);
            if (suggest.completion() == null || suggest.completion().options().isEmpty()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            suggest.completion().options().forEach(opt -> result.add(opt.text()));
            return result;
        } catch (Exception e) {
            log.error("搜索建议 ES 查询失败: keyword={}", keyword, e);
            return List.of();
        }
    }
}
