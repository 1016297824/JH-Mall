package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.enums.user.GrowthChangeTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;
import com.mall.user.service.IPointsService;
import com.mall.user.vo.PointsRecordVO;
import com.mall.user.vo.PointsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 积分服务实现类
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements IPointsService {

    private static final int MAX_RETRY = 3;

    private final MallPointsAccountMapper mallPointsAccountMapper;

    private final MallUserPointsLogMapper mallUserPointsLogMapper;

    @Override
    public PointsVO getPoints(Long userId) {
        LambdaQueryWrapper<MallPointsAccountDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallPointsAccountDO::getUserId, userId)
                .eq(MallPointsAccountDO::getIsDeleted, 0);
        MallPointsAccountDO account = mallPointsAccountMapper.selectOne(wrapper);
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        PointsVO vo = new PointsVO();
        vo.setTotalPoints(account.getTotalPoints());
        vo.setAvailablePoints(account.getAvailablePoints());
        vo.setUsedPoints(account.getUsedPoints());
        vo.setExpiredPoints(account.getExpiredPoints());
        return vo;
    }

    @Override
    public IPage<PointsRecordVO> getPointsRecords(Long userId, String bizType, int page, int size) {
        Page<MallUserPointsLogDO> pageParam = new Page<>(page, size);
        IPage<MallUserPointsLogDO> logPage = mallUserPointsLogMapper.selectByUserIdPage(pageParam, userId, bizType);
        return logPage.convert(this::toPointsRecordVO);
    }

    @Override
    public void addPoints(Long userId, int points, BizTypeEnum bizType, String bizNo) {
        for (int i = 0; i < MAX_RETRY; i++) {
            LambdaQueryWrapper<MallPointsAccountDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MallPointsAccountDO::getUserId, userId)
                    .eq(MallPointsAccountDO::getIsDeleted, 0);
            MallPointsAccountDO account = mallPointsAccountMapper.selectOne(wrapper);
            if (account == null) {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
            int currentVersion = account.getVersion();
            int beforePoints = account.getAvailablePoints();
            int rows = mallPointsAccountMapper.addPoints(userId, points, currentVersion);
            if (rows > 0) {
                MallUserPointsLogDO logDO = new MallUserPointsLogDO();
                logDO.setUserId(userId);
                logDO.setBizType(bizType.getCode());
                logDO.setBizNo(bizNo);
                logDO.setChangeType(GrowthChangeTypeEnum.INCREASE.getCode());
                logDO.setPoints(points);
                logDO.setBeforePoints(beforePoints);
                logDO.setAfterPoints(beforePoints + points);
                logDO.setRemark(bizType.getName());
                logDO.setIsDeleted(0);
                logDO.setCreateTime(LocalDateTime.now());
                logDO.setUpdateTime(LocalDateTime.now());
                mallUserPointsLogMapper.insert(logDO);
                log.info("积分增加成功, userId={}, points={}, bizType={}, bizNo={}", userId, points, bizType.getCode(), bizNo);
                return;
            }
            log.warn("积分增加乐观锁冲突, userId={}, retry={}", userId, i + 1);
        }
        log.error("积分增加失败, 乐观锁重试{}次均失败, userId={}", MAX_RETRY, userId);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private PointsRecordVO toPointsRecordVO(MallUserPointsLogDO logDO) {
        PointsRecordVO vo = new PointsRecordVO();
        vo.setId(logDO.getId());
        vo.setBizType(logDO.getBizType());
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logDO.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logDO.getBizType());
        vo.setChangeType(logDO.getChangeType());
        vo.setPoints(logDO.getPoints());
        vo.setBeforePoints(logDO.getBeforePoints());
        vo.setAfterPoints(logDO.getAfterPoints());
        vo.setRemark(logDO.getRemark());
        if (logDO.getCreateTime() != null) {
            vo.setCreateTime(Date.from(logDO.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}
