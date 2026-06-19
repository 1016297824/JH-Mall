package com.mall.search.controller.inner;

import com.mall.search.infrastructure.schedule.IndexRebuildTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索内部 Controller
 *
 * <p>供 ruoyi-job 定时调度全量重建，路径 /inner/search/**</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@RestController
@RequestMapping("/inner/search")
@RequiredArgsConstructor
public class RemoteSearchInnerController {

    private final IndexRebuildTask indexRebuildTask;

    @PostMapping("/index/rebuild")
    void rebuildIndex() {
        log.info("收到索引重建请求");
        indexRebuildTask.execute();
    }
}
