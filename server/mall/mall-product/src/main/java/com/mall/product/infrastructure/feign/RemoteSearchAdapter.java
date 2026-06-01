package com.mall.product.infrastructure.feign;

import com.mall.api.feign.RemoteSearchService;
import com.mall.api.feign.RemoteSearchService.SearchSyncRequest;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 搜索远程调用适配器
 *
 * <p>封装 RemoteSearchService Feign 调用，统一异常处理</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteSearchAdapter {

    private final RemoteSearchService remoteSearchService;

    /**
     * 同步商品索引到搜索引擎
     *
     * <p>封装 RemoteSearchService Feign 调用，Feign 异常统一转为 BusinessException，
     * 由调用方 (SearchSyncProducer) 根据异常决定是否降级到 Outbox。</p>
     *
     * @param request 搜索同步请求（含 spuId + operation + timestamp）
     */
    public void syncProduct(SearchSyncRequest request) {
        try {
            remoteSearchService.syncProduct(request);
        } catch (Exception e) {
            log.error("实时同步搜索索引失败, spuId={}, operation={}", request.getSpuId(), request.getOperation(), e);
            // 转为 BusinessException，上层 SearchSyncProducer 捕获后写 Outbox
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
