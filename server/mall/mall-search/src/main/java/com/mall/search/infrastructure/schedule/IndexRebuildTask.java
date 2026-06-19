package com.mall.search.infrastructure.schedule;

import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 搜索索引全量重建定时任务
 *
 * <p>由 ruoyi-job 定时调度，调用链:
 * ruoyi-job → RemoteSearchInnerController → IndexRebuildTask.execute() → IndexService.rebuildIndex()</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexRebuildTask {

    private final IndexService indexService;

    public void execute() {
        log.info("定时全量重建索引开始");
        indexService.rebuildIndex();
        log.info("定时全量重建索引完成");
    }
}
