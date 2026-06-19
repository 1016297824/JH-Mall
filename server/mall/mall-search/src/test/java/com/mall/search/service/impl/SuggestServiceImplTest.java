package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mall.search.service.SuggestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SuggestServiceImplTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private SuggestServiceImpl suggestService;

    @Test
    void suggest_nullKeyword_shouldReturnEmpty() {
        assertTrue(suggestService.suggest(null).isEmpty());
    }

    @Test
    void suggest_blankKeyword_shouldReturnEmpty() {
        assertTrue(suggestService.suggest("  ").isEmpty());
    }
}
