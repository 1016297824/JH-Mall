package com.mall.user.schedule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.enums.user.GrowthChangeTypeEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 积分过期定时任务
 *
 * <p>分批将用户可用积分清零，每批 500 条</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsExpireTask {

    private static final int PAGE_SIZE = 500;

    private final MallPointsAccountMapper pointsAccountMapper;

    private final MallUserPointsLogMapper pointsLogMapper;

    @Scheduled(cron = "0 0 0 31 12 ?")
    public void execute() {
        log.info("开始年度积分清零");
        int page = 1;
        int totalExpired = 0;
        while (true) {
            Page<MallPointsAccountDO> pageParam = new Page<>(page, PAGE_SIZE);
            LambdaQueryWrapper<MallPointsAccountDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.gt(MallPointsAccountDO::getAvailablePoints, 0);
            Page<MallPointsAccountDO> result = pointsAccountMapper.selectPage(pageParam, wrapper);
            List<MallPointsAccountDO> batch = result.getRecords();
            if (batch.isEmpty()) {
                break;
            }
            for (MallPointsAccountDO account : batch) {
                int available = account.getAvailablePoints() != null ? account.getAvailablePoints() : 0;
                if (available <= 0) {
                    continue;
                }

                pointsAccountMapper.expirePoints(account.getUserId());

                MallUserPointsLogDO logDO = new MallUserPointsLogDO();
                logDO.setUserId(account.getUserId());
                logDO.setBizType(BizTypeEnum.EXPIRE.getCode());
                logDO.setChangeType(GrowthChangeTypeEnum.DECREASE.getCode());
                logDO.setPoints(available);
                logDO.setBeforePoints(available);
                logDO.setAfterPoints(0);
                logDO.setRemark("年度积分清零");
                logDO.setCreateTime(LocalDateTime.now());
                pointsLogMapper.insert(logDO);

                totalExpired += available;
            }
            page++;
        }
        log.info("年度积分清零完成，共清零 {} 积分", totalExpired);
    }
}
