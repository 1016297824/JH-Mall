package com.mall.search.convert.response;

import com.mall.search.DO.ProductIndexDO;
import com.mall.search.vo.SearchItemVO;
import com.mall.search.vo.SearchResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchConvertTest {

    @Test
    @SuppressWarnings("unchecked")
    void toSearchResultVO_shouldConvertHitsCorrectly() {
        ProductIndexDO source = new ProductIndexDO();
        source.setProductId(1L);
        source.setSpuName("测试商品");
        source.setPrice(9900);
        source.setImage("http://img.jpg");
        source.setSalesCount(100);

        SearchHit<ProductIndexDO> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(source);
        when(hit.getHighlightField("spuName")).thenReturn(List.of("<em>测试</em>商品"));

        SearchHits<ProductIndexDO> hits = mock(SearchHits.class);
        when(hits.iterator()).thenReturn(List.of(hit).iterator());
        when(hits.getTotalHits()).thenReturn(1L);
        doReturn(TotalHitsRelation.EQUAL_TO).when(hits).getTotalHitsRelation();

        SearchResultVO result = SearchConvert.toSearchResultVO(hits, 1, 20);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1, result.getItems().size());

        SearchItemVO item = result.getItems().get(0);
        assertEquals(1L, item.getSpuId());
        assertEquals("测试商品", item.getSpuName());
        assertEquals("<em>测试</em>商品", item.getSpuNameHighlight());
        assertEquals("99.00", item.getPrice());
        assertEquals(100, item.getSalesCount());
    }
}
