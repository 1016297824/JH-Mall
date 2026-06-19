package com.mall.search.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.DTO.request.SearchReqDTO;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.vo.SearchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceImplTest {

    @Mock private ElasticsearchOperations operations;
    @Mock private MallSearchConfigProperties configProperties;
    @Mock private SearchHits<ProductIndexDO> hits;
    private SearchServiceImpl searchService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MallSearchConfigProperties.Page pageConfig = mock(MallSearchConfigProperties.Page.class);
        when(pageConfig.getMaxSize()).thenReturn(60);
        when(pageConfig.getMaxDepth()).thenReturn(10000);
        MallSearchConfigProperties.Result resultConfig = mock(MallSearchConfigProperties.Result.class);
        when(resultConfig.getCacheTtl()).thenReturn(60L);
        when(configProperties.getPage()).thenReturn(pageConfig);
        when(configProperties.getResult()).thenReturn(resultConfig);

        when(operations.search(any(Query.class), eq(ProductIndexDO.class))).thenReturn(hits);
        when(hits.getTotalHits()).thenReturn(0L);
        when(hits.iterator()).thenReturn(Collections.emptyIterator());

        searchService = new SearchServiceImpl(operations, configProperties);
    }

    @Test
    void search_pageSizeExceedsMax_shouldThrow() {
        SearchReqDTO req = new SearchReqDTO();
        req.setSize(100);
        assertThrows(BusinessException.class, () -> searchService.search(req));
    }

    @Test
    void search_validRequest_shouldReturnResult() {
        SearchReqDTO req = new SearchReqDTO();
        req.setKeyword("测试");
        req.setPage(1);
        req.setSize(20);
        SearchResultVO result = searchService.search(req);
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
    }
}
