package com.mall.user.schedule;

import java.time.LocalDateTime;
import java.util.List;

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
 * <p>由 ruoyi-job 调度，通过 {@code /inner/user/points/expire} 端点调用。
 * 分批将用户可用积分清零，逐条记录过期流水日志</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsExpireTask {

    /** 每批处理的记录数 */
    private static final int PAGE_SIZE = 500;

    private final MallPointsAccountMapper pointsAccountMapper;

    private final MallUserPointsLogMapper pointsLogMapper;

    /**
     * 执行年度积分清零
     *
     * <p>分页查询所有可用积分大于 0 的账户，逐条清零并写入过期日志，
     * 直到没有更多数据为止</p>
     *
     * @return 共清零的积分总数
     */
    public int execute() {
        log.info("开始年度积分清零");
        int page = 1;
        int totalExpired = 0;
        // 分页查询所有可用积分大于 0 的账户，逐批清零
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

                // 执行积分清零：可用积分移至过期字段
                pointsAccountMapper.expirePoints(account.getUserId());

                // 逐条记录积分过期流水日志
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
        return totalExpired;
    }
}
