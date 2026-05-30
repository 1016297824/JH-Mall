package com.mall.product.infrastructure.feign;

import com.mall.api.feign.RemoteSearchService;
import com.mall.api.feign.RemoteSearchService.SearchSyncRequest;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteSearchAdapter {

    private final RemoteSearchService remoteSearchService;

    public void syncProduct(SearchSyncRequest request) {
        try {
            remoteSearchService.syncProduct(request);
        } catch (Exception e) {
            log.error("实时同步搜索索引失败, spuId={}, operation={}", request.getSpuId(), request.getOperation(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
