package com.mall.search.convert.response;

import com.mall.search.DO.ProductIndexDO;
import com.mall.search.vo.SearchItemVO;
import com.mall.search.vo.SearchResultVO;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索转换器
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public final class SearchConvert {

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
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .toPlainString());
        }
        // 高亮字段提取
        List<String> highlights = hit.getHighlightField("spuName");
        if (highlights != null && !highlights.isEmpty()) {
            vo.setSpuNameHighlight(highlights.get(0));
        }
        return vo;
    }
}
