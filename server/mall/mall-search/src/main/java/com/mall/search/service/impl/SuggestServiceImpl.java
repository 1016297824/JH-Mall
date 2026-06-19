package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mall.search.service.SuggestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<String> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        // TODO: completion suggester 调用
        return List.of();
    }
}
