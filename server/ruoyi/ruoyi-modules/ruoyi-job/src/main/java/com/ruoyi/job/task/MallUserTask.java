package com.ruoyi.job.task;

import com.mall.api.feign.RemoteUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-user 定时任务代理
 *
 * <p>ruoyi-job 通过 Feign 调 mall-user /inner/user/** 端点，
 * 在管理端以 Bean 调度：{@code mallUserTask.expirePoints()}</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component("mallUserTask")
@RequiredArgsConstructor
public class MallUserTask {

    private final RemoteUserService remoteUserService;

    /**
     * 年度积分清零
     *
     * <p>调 mall-user /inner/user/points/expire，
     * 分批清零用户可用积分并记录过期流水</p>
     */
    public void expirePoints() {
        log.info("ruoyi-job 触发积分清零");
        int total = remoteUserService.expirePoints();
        log.info("ruoyi-job 积分清零完成，共清零 {} 积分", total);
    }
}
