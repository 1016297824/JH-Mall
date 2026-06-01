package com.mall.product.infrastructure.schedule;

import com.mall.product.service.IHotProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 热点排名刷新定时任务
 *
 * <p>由 ruoyi-job 或 {@code @Scheduled} 调用，
 * 遍历 ZSet Top 200 + PFCOUNT UV → 重算 score → ZADD 更新</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotRankRefreshTask {

    private final IHotProductService hotProductService;

    public void execute() {
        log.info("热点排名刷新开始");
        hotProductService.refreshHotRank();
        log.info("热点排名刷新完成");
    }
}
