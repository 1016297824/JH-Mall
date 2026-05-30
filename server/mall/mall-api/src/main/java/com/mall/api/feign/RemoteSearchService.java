package com.mall.api.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * C 端搜索服务 Feign 接口
 *
 * <p>提供给 mall-product 调用，触发搜索索引同步</p>
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@FeignClient(contextId = "mall-search", value = "mall-search")
public interface RemoteSearchService {

    /**
     * 同步商品到搜索引擎
     *
     * @param request 同步请求
     */
    @PostMapping("/inner/search/product/sync")
    void syncProduct(@RequestBody SearchSyncRequest request);

    /**
     * 搜索索引同步请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class SearchSyncRequest {
        /** SPU ID */
        private Long spuId;
        /** 同步操作类型 */
        private String operation;
        /** 时间戳 */
        private Long timestamp;
    }
}
