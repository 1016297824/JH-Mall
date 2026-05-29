package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.VO.GrowthRecordVO;
import com.mall.user.convert.response.GrowthConvert;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.service.IGrowthLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 成长值流水服务实现类
 *
 * <p>仅操作 mall_user_growth_log 表，提供成长值流水分页查询及 DO-VO 转换</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthLogServiceImpl implements IGrowthLogService {

    /** 成长值流水 Mapper */
    private final MallUserGrowthLogMapper mallUserGrowthLogMapper;

    /**
     * 分页查询用户成长值流水记录
     *
     * @param userId  用户 ID
     * @param bizType 业务类型编码，为空则查全部
     * @param page    页码，最小为 1
     * @param size    每页条数，最大 100
     * @return 成长值流水记录分页结果
     */
    @Override
    public IPage<GrowthRecordVO> getGrowthRecords(Long userId, String bizType, int page, int size) {
        // 参数安全处理：防止负数页码和超大分页
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(size, 100);
        Page<MallUserGrowthLogDO> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<MallUserGrowthLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserGrowthLogDO::getUserId, userId);
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(MallUserGrowthLogDO::getBizType, bizType);
        }
        wrapper.orderByDesc(MallUserGrowthLogDO::getCreateTime);
        IPage<MallUserGrowthLogDO> logPage = mallUserGrowthLogMapper.selectPage(pageParam, wrapper);
        return logPage.convert(GrowthConvert::toGrowthRecordVO);
    }
}
