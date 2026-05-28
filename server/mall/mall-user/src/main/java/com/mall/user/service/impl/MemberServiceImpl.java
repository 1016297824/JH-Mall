package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.enums.user.GrowthChangeTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.IMemberService;
import com.mall.user.VO.GrowthRecordVO;
import com.mall.user.VO.GrowthVO;
import com.mall.user.VO.MemberLevelVO;
import com.mall.user.VO.MembershipVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 会员服务实现类
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements IMemberService {

    private final MallUserMemberMapper mallUserMemberMapper;

    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;

    private final MallUserGrowthLogMapper mallUserGrowthLogMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public MembershipVO getMembership(Long userId) {
        MallUserMemberDO member = getMemberByUserId(userId);
        List<MallUserMemberLevelDO> levels = mallUserMemberLevelMapper.selectList(null);
        MallUserMemberLevelDO currentLevel = findLevelById(levels, member.getLevelId());
        MallUserMemberLevelDO nextLevel = findNextLevel(levels, member.getLevelId());

        MembershipVO vo = new MembershipVO();
        vo.setGrowth(member.getGrowth());
        vo.setTotalGrowth(member.getTotalGrowth());
        vo.setCurrentLevel(toMemberLevelVO(currentLevel));
        vo.setNextLevel(nextLevel != null ? toMemberLevelVO(nextLevel) : null);
        vo.setBenefits(parseBenefits(currentLevel));
        return vo;
    }

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
        if (nextLevel != null) {
            vo.setNeedGrowth(nextLevel.getMinGrowth() - member.getGrowth());
            int totalSpan = nextLevel.getMinGrowth() - currentLevel.getMinGrowth();
            int progress = totalSpan > 0 ? (member.getGrowth() - currentLevel.getMinGrowth()) * 100 / totalSpan : 100;
            vo.setProgressPercent(progress);
        } else {
            vo.setNeedGrowth(0);
            vo.setProgressPercent(100);
        }
        return vo;
    }

    @Override
    public void addGrowth(Long userId, int growth, BizTypeEnum bizType, String bizNo) {
        MallUserMemberDO member = getMemberByUserId(userId);
        int beforeGrowth = member.getGrowth();
        mallUserMemberMapper.addGrowth(userId, growth);

        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();
        logDO.setUserId(userId);
        logDO.setBizType(bizType.getCode());
        logDO.setBizNo(bizNo);
        logDO.setChangeType(GrowthChangeTypeEnum.INCREASE.getCode());
        logDO.setGrowth(growth);
        logDO.setBeforeGrowth(beforeGrowth);
        logDO.setAfterGrowth(beforeGrowth + growth);
        logDO.setRemark(bizType.getName());
        logDO.setIsDeleted(0);
        logDO.setCreateTime(LocalDateTime.now());
        logDO.setUpdateTime(LocalDateTime.now());
        mallUserGrowthLogMapper.insert(logDO);

        List<MallUserMemberLevelDO> levels = mallUserMemberLevelMapper.selectList(null);
        checkUpgrade(userId, beforeGrowth + growth, levels);
        log.info("成长值增加成功, userId={}, growth={}, bizType={}, bizNo={}", userId, growth, bizType.getCode(), bizNo);
    }

    @Override
    public IPage<GrowthRecordVO> getGrowthRecords(Long userId, String bizType, int page, int size) {
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
        return logPage.convert(this::toGrowthRecordVO);
    }

    private MallUserMemberDO getMemberByUserId(Long userId) {
        LambdaQueryWrapper<MallUserMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserMemberDO::getUserId, userId)
                .eq(MallUserMemberDO::getIsDeleted, 0);
        MallUserMemberDO member = mallUserMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return member;
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

    private void checkUpgrade(Long userId, int newGrowth, List<MallUserMemberLevelDO> levels) {
        MallUserMemberDO member = getMemberByUserId(userId);
        MallUserMemberLevelDO currentLevel = findLevelById(levels, member.getLevelId());
        if (currentLevel == null || currentLevel.getMaxGrowth() == null) {
            return;
        }
        MallUserMemberLevelDO nextLevel = findNextLevel(levels, member.getLevelId());
        if (nextLevel != null && newGrowth >= nextLevel.getMinGrowth()) {
            mallUserMemberMapper.updateLevel(userId, nextLevel.getId());
            log.info("会员自动升级, userId={}, oldLevel={}, newLevel={}", userId, member.getLevelId(), nextLevel.getId());
        }
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

    @SuppressWarnings("unchecked")
    private List<String> parseBenefits(MallUserMemberLevelDO level) {
        if (level == null || level.getBenefitsJson() == null) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(level.getBenefitsJson(), List.class);
        } catch (Exception e) {
            log.warn("解析会员权益JSON失败, levelId={}", level.getId(), e);
            return Collections.emptyList();
        }
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
