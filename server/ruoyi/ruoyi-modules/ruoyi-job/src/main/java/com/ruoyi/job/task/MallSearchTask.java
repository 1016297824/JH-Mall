package com.ruoyi.job.task;

import com.mall.api.feign.RemoteSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-search 定时任务代理
 *
 * <p>ruoyi-job 通过 Feign 调 mall-search /inner/search/** 端点，
 * 在管理端以 Bean 调度：{@code mallSearchTask.rebuildIndex()}</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component("mallSearchTask")
@RequiredArgsConstructor
public class MallSearchTask {

    private final RemoteSearchService remoteSearchService;

    /**
     * 触发搜索索引全量重建
     *
     * <p>调 mall-search /inner/search/index/rebuild</p>
     */
    public void rebuildIndex() {
        log.info("ruoyi-job 触发搜索索引重建");
        remoteSearchService.rebuildIndex();
        log.info("ruoyi-job 搜索索引重建完成");
    }
}
