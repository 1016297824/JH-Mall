package com.ruoyi.job.task;

import com.mall.api.feign.RemoteProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-product 定时任务代理
 *
 * <p>ruoyi-job 通过 Feign 调 mall-product /inner/product/** 端点，
 * 在管理端以 Bean 调度：{@code mallProductTask.compensateOutbox()}</p>
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Slf4j
@Component("mallProductTask")
@RequiredArgsConstructor
public class MallProductTask {

    private final RemoteProductService remoteProductService;

    /**
     * Outbox 补偿投递
     *
     * <p>调 mall-product /inner/product/outbox/compensate，
     * 扫描 Outbox 表待发送消息并补偿投递</p>
     */
    public void compensateOutbox() {
        log.info("ruoyi-job 触发 Outbox 补偿");
        int total = remoteProductService.compensateOutbox();
        log.info("ruoyi-job Outbox 补偿完成，共处理 {} 条", total);
    }
}
