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
import com.mall.user.VO.PointsRecordVO;
import com.mall.user.VO.PointsVO;
import com.mall.user.convert.response.PointsConvert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 积分服务实现类
 *
 * <p>提供积分账户查询、积分流水查询、积分增加等功能。
 * 积分增加采用乐观锁重试机制，最多重试 {MAX_RETRY} 次，避免并发冲突</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements IPointsService {

    /** 乐观锁最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 积分账户 Mapper */
    private final MallPointsAccountMapper mallPointsAccountMapper;

    /** 积分流水 Mapper */
    private final MallUserPointsLogMapper mallUserPointsLogMapper;

    /**
     * 查询用户积分信息
     *
     * @param userId 用户 ID
     * @return 积分信息 VO
     * @throws BusinessException 积分账户不存在时抛出 ACCOUNT_NOT_FOUND
     */
    @Override
    public PointsVO getPoints(Long userId) {
        LambdaQueryWrapper<MallPointsAccountDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallPointsAccountDO::getUserId, userId)
                .eq(MallPointsAccountDO::getIsDeleted, 0);
        MallPointsAccountDO account = mallPointsAccountMapper.selectOne(wrapper);
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return PointsConvert.toPointsVO(account);
    }

    /**
     * 分页查询用户积分流水记录
     *
     * @param userId  用户 ID
     * @param bizType 业务类型编码，为空则查全部
     * @param page    页码，最小为 1
     * @param size    每页条数，最大 100
     * @return 积分流水分页结果
     */
    @Override
    public IPage<PointsRecordVO> getPointsRecords(Long userId, String bizType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(size, 100);
        Page<MallUserPointsLogDO> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<MallUserPointsLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserPointsLogDO::getUserId, userId);
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(MallUserPointsLogDO::getBizType, bizType);
        }
        wrapper.orderByDesc(MallUserPointsLogDO::getCreateTime);
        IPage<MallUserPointsLogDO> logPage = mallUserPointsLogMapper.selectPage(pageParam, wrapper);
        return logPage.convert(PointsConvert::toPointsRecordVO);
    }

    /**
     * 为用户增加积分
     *
     * <p>使用乐观锁（version 字段）保证并发安全，失败时最多重试 {MAX_RETRY} 次，
     * 全部失败则抛出 SYSTEM_ERROR</p>
     *
     * @param userId  用户 ID
     * @param points  增加的积分
     * @param bizType 业务类型枚举
     * @param bizNo   业务流水号，可为 null
     * @throws BusinessException 积分账户不存在或乐观锁重试耗尽
     */
    @Override
    public void addPoints(Long userId, int points, BizTypeEnum bizType, String bizNo) {
        // 乐观锁重试，最多 MAX_RETRY 次，防止并发冲突
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
            // 乐观锁更新：version 匹配才更新成功，否则重试
            int rows = mallPointsAccountMapper.addPoints(userId, points, currentVersion);
            if (rows > 0) {
                // 乐观锁成功，写入积分流水日志
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
}
