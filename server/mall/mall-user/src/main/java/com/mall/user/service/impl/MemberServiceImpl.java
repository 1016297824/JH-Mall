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
 * <p>提供会员信息查询、成长值管理、等级自动升级、成长值流水分页查询等功能。
 * 成长值增加后自动检测是否满足升级条件</p>
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

    /** JSON 解析器，用于解析会员权益配置 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 查询用户会员信息
     *
     * <p>包含当前成长值、当前等级、下一等级及等级权益</p>
     *
     * @param userId 用户 ID
     * @return 会员信息 VO
     */
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

    /**
     * 查询用户成长值信息
     *
     * <p>包含当前成长值、累计成长值、当前等级、下一等级及升级进度百分比</p>
     *
     * @param userId 用户 ID
     * @return 成长值信息 VO
     */
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

    /**
     * 为用户增加成长值
     *
     * <p>先增加成长值，再写入流水日志，最后检测是否可升级</p>
     *
     * @param userId  用户 ID
     * @param growth  增加的成长值
     * @param bizType 业务类型枚举
     * @param bizNo   业务流水号，可为 null
     */
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

    /**
     * 根据用户 ID 查询会员信息
     *
     * @param userId 用户 ID
     * @return 会员 DO
     * @throws BusinessException 会员不存在时抛出 ACCOUNT_NOT_FOUND
     */
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

    /**
     * 根据等级 ID 查找等级信息
     *
     * @param levels  全部等级列表
     * @param levelId 等级 ID
     * @return 匹配的等级 DO，未找到返回 null
     */
    private MallUserMemberLevelDO findLevelById(List<MallUserMemberLevelDO> levels, Long levelId) {
        return levels.stream()
                .filter(l -> l.getId().equals(levelId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找当前等级对应的下一等级
     *
     * <p>按成长值升序排列，取第一个 minGrowth 大于当前等级 maxGrowth 的等级</p>
     *
     * @param levels         全部等级列表
     * @param currentLevelId 当前等级 ID
     * @return 下一等级 DO，已是最高等级则返回 null
     */
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

    /**
     * 检测会员是否可升级
     *
     * <p>若当前成长值达到下一等级最低门槛，自动升级并记录日志</p>
     *
     * @param userId    用户 ID
     * @param newGrowth 新的成长值
     * @param levels    全部等级列表
     */
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

    /**
     * 将等级 DO 转换为等级 VO
     *
     * @param level 等级 DO
     * @return 等级 VO
     */
    private MemberLevelVO toMemberLevelVO(MallUserMemberLevelDO level) {
        MemberLevelVO vo = new MemberLevelVO();
        if (level != null) {
            vo.setLevelName(level.getLevelName());
            vo.setIcon(level.getIcon());
            vo.setLevelValue(level.getLevelValue());
        }
        return vo;
    }

    /**
     * 解析等级权益 JSON 为字符串列表
     *
     * @param level 等级 DO
     * @return 权益名称列表，解析失败返回空列表
     */
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

    /**
     * 将成长值流水 DO 转换为 VO
     *
     * @param logDO 成长值流水 DO
     * @return 成长值流水 VO
     */
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
        // 将 LocalDateTime 转为 Date 供前端展示
        if (logDO.getCreateTime() != null) {
            vo.setCreateTime(Date.from(logDO.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}
