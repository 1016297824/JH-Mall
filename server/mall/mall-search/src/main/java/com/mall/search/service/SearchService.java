package com.mall.search.service;

import com.mall.search.DTO.request.SearchReqDTO;
import com.mall.search.vo.SearchResultVO;

/**
 * C 端商品搜索服务
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface SearchService {

    /**
     * 商品搜索
     *
     * @param req 搜索请求参数
     * @return 搜索结果 VO
     */
    SearchResultVO search(SearchReqDTO req);
}
