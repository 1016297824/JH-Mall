package com.mall.search.controller.inner;

import com.mall.api.feign.RemoteSearchService;
import com.mall.search.infrastructure.schedule.IndexRebuildTask;
import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索内部 Controller
 *
 * <p>供 ruoyi-job 定时调度全量重建、mall-product 实时同步索引，路径 /inner/search/**</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@RestController
@RequestMapping("/inner/search")
@RequiredArgsConstructor
public class RemoteSearchInnerController {

    /** 缺省操作类型：生产端只发 UPSERT / DELETE，缺失时按 UPSERT 处理 */
    private static final String DEFAULT_OPERATION = "UPSERT";

    private final IndexRebuildTask indexRebuildTask;

    private final IndexService indexService;

    @PostMapping("/index/rebuild")
    void rebuildIndex() {
        log.info("收到索引重建请求");
        indexRebuildTask.execute();
    }

    /**
     * 实时同步单个商品的索引
     *
     * <p>实现契约 {@link RemoteSearchService#syncProduct}（{@code POST /inner/search/product/sync}）。
     * 该端点在本次修复前<b>从未实现</b>——mall-product 每次实时调用都 404，
     * 于是所有商品变更都降级走 Outbox → 补偿任务 → MQ，实时通道形同不存在。</p>
     *
     * @param request 同步请求（spuId / operation / timestamp）
     */
    @PostMapping("/product/sync")
    void syncProduct(@RequestBody RemoteSearchService.SearchSyncRequest request) {
        if (request == null || request.getSpuId() == null) {
            log.warn("实时同步请求缺少 spuId，忽略: {}", request);
            return;
        }
        String operation = request.getOperation() != null ? request.getOperation() : DEFAULT_OPERATION;
        long timestamp = request.getTimestamp() != null ? request.getTimestamp() : 0L;
        indexService.syncProduct(request.getSpuId(), operation, timestamp);
    }
}
