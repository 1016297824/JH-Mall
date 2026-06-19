package com.mall.search.controller;

import com.mall.search.service.IndexService;
import com.mall.search.service.SearchService;
import com.mall.search.service.SuggestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SearchController 单元测试
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private SuggestService suggestService;

    @MockitoBean
    private IndexService indexService;

    @Test
    void search_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"测试\",\"page\":1,\"size\":20}"))
                .andExpect(status().isOk());
    }

    @Test
    void suggest_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/search/suggest").param("keyword", "苹果"))
                .andExpect(status().isOk());
    }

    @Test
    void rebuildIndex_shouldReturn200() throws Exception {
        mockMvc.perform(post("/mall-search/index/rebuild"))
                .andExpect(status().isOk());
    }
}
