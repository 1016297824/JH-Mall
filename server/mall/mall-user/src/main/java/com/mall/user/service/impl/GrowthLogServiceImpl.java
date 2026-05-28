package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.service.IGrowthService;
import com.mall.user.vo.GrowthRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;

/**
 * 成长值流水服务 — 仅操作 mall_user_growth_log 表
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements IGrowthService {

    private final MallUserGrowthLogMapper mallUserGrowthLogMapper;

    @Override
    public IPage<GrowthRecordVO> getGrowthRecords(Long userId, String bizType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(size, 100);
        Page<MallUserGrowthLogDO> pageParam = new Page<>(safePage, safeSize);
        IPage<MallUserGrowthLogDO> logPage = mallUserGrowthLogMapper.selectByUserIdPage(pageParam, userId, bizType);
        return logPage.convert(this::toGrowthRecordVO);
    }

    private GrowthRecordVO toGrowthRecordVO(MallUserGrowthLogDO logDO) {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(logDO.getId());
        vo.setBizType(logDO.getBizType());
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logDO.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logDO.getBizType());
        vo.setChangeType(logDO.getChangeType());
        vo.setGrowth(logDO.getGrowth());
        vo.setBeforeGrowth(logDO.getBeforeGrowth());
        vo.setAfterGrowth(logDO.getAfterGrowth());
        vo.setRemark(logDO.getRemark());
        if (logDO.getCreateTime() != null) {
            vo.setCreateTime(Date.from(logDO.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}
