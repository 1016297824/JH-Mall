package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.IGrowthService;
import com.mall.user.vo.GrowthRecordVO;
import com.mall.user.vo.GrowthVO;
import com.mall.user.vo.MemberLevelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 成长值服务实现类
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements IGrowthService {

    private final MallUserMemberMapper mallUserMemberMapper;

    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;

    private final MallUserGrowthLogMapper mallUserGrowthLogMapper;

    @Override
    public GrowthVO getGrowth(Long userId) {
        MallUserMemberDO member = getMemberByUserId(userId);
        List<MallUserMemberLevelDO> levels = mallUserMemberLevelMapper.selectList(null);
        MallUserMemberLevelDO currentLevel = findLevelById(levels, member.getLevelId());
        MallUserMemberLevelDO nextLevel = findNextLevel(levels, member.getLevelId());

        GrowthVO vo = new GrowthVO();
        vo.setGrowth(member.getGrowth());
        vo.setTotalGrowth(member.getTotalGrowth());
        vo.setCurrentLevel(toMemberLevelVO(currentLevel));
        vo.setNextLevel(nextLevel != null ? toMemberLevelVO(nextLevel) : null);
        if (nextLevel != null && currentLevel != null) {
            vo.setNeedGrowth(nextLevel.getMinGrowth() - member.getGrowth());
            int totalSpan = nextLevel.getMinGrowth() - currentLevel.getMinGrowth();
            int progress = totalSpan > 0
                    ? (member.getGrowth() - currentLevel.getMinGrowth()) * 100 / totalSpan
                    : 100;
            vo.setProgressPercent(progress);
        } else {
            vo.setNeedGrowth(0);
            vo.setProgressPercent(100);
        }
        return vo;
    }

    @Override
    public IPage<GrowthRecordVO> getGrowthRecords(Long userId, String bizType, int page, int size) {
        Page<MallUserGrowthLogDO> pageParam = new Page<>(page, size);
        IPage<MallUserGrowthLogDO> logPage = mallUserGrowthLogMapper.selectByUserIdPage(pageParam, userId, bizType);
        return logPage.convert(this::toGrowthRecordVO);
    }

    private MallUserMemberDO getMemberByUserId(Long userId) {
        LambdaQueryWrapper<MallUserMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserMemberDO::getUserId, userId)
                .eq(MallUserMemberDO::getIsDeleted, 0);
        return mallUserMemberMapper.selectOne(wrapper);
    }

    private MallUserMemberLevelDO findLevelById(List<MallUserMemberLevelDO> levels, Long levelId) {
        return levels.stream()
                .filter(l -> l.getId().equals(levelId))
                .findFirst()
                .orElse(null);
    }

    private MallUserMemberLevelDO findNextLevel(List<MallUserMemberLevelDO> levels, Long currentLevelId) {
        MallUserMemberLevelDO current = findLevelById(levels, currentLevelId);
        if (current == null) {
            return null;
        }
        return levels.stream()
                .filter(l -> l.getMinGrowth() > current.getMaxGrowth())
                .sorted((a, b) -> Integer.compare(a.getMinGrowth(), b.getMinGrowth()))
                .findFirst()
                .orElse(null);
    }

    private MemberLevelVO toMemberLevelVO(MallUserMemberLevelDO level) {
        MemberLevelVO vo = new MemberLevelVO();
        if (level != null) {
            vo.setLevelName(level.getLevelName());
            vo.setIcon(level.getIcon());
            vo.setLevelValue(level.getLevelValue());
        }
        return vo;
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
