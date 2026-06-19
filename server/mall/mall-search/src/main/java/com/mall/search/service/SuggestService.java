package com.mall.search.service;

import java.util.List;

/**
 * C 端搜索建议服务
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface SuggestService {

    /**
     * 根据关键词获取搜索建议
     *
     * @param keyword 搜索关键词
     * @return 建议词列表，keyword 为空时返回空列表
     */
    List<String> suggest(String keyword);
}
