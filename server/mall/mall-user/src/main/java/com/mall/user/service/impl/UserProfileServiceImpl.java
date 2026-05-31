package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.DTO.request.UpdateProfileDTO;
import com.mall.user.convert.request.UserProfileConvert;
import com.mall.user.convert.response.UserConvert;
import com.mall.user.infrastructure.feign.RemoteAuthAdapter;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.IUserProfileService;
import com.mall.user.VO.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户资料服务实现类
 *
 * <p>提供用户资料查询、修改功能，查询结果缓存至 Redis。
 * 资料包含昵称、头像、性别、生日、手机号（脱敏）、邮箱、会员等级、积分等</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    /** 用户 Mapper */
    private final MallUserMapper mallUserMapper;

    /** 用户会员 Mapper */
    private final MallUserMemberMapper mallUserMemberMapper;

    /** 会员等级 Mapper */
    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;

    /** 积分账户 Mapper */
    private final MallPointsAccountMapper mallPointsAccountMapper;

    /** Redis 模板 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 认证服务 Feign 适配器 */
    private final RemoteAuthAdapter remoteAuthAdapter;

    /** 用户模块配置属性 */
    private final MallUserConfigProperties mallUserConfigProperties;

    /**
     * 查询用户资料
     *
     * <p>优先从 Redis 缓存读取，缓存未命中时查库并回写缓存</p>
     *
     * @param userId 用户 ID
     * @return 用户资料 VO
     */
    @Override
    public UserProfileVO getProfile(Long userId) {
        String cacheKey = CacheConstants.User.PROFILE + userId;
        // 缓存命中直接返回，避免查库
        UserProfileVO cached = (UserProfileVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查库构建资料并回写缓存
        UserProfileVO vo = buildProfile(userId);
        long ttl = mallUserConfigProperties.getProfile().getCacheTtl();
        redisTemplate.opsForValue().set(cacheKey, vo, Duration.ofSeconds(ttl));
        return vo;
    }

    /**
     * 修改用户资料
     *
     * <p>仅更新传入的非空字段，更新后清除缓存并重新返回最新资料</p>
     *
     * @param userId  用户 ID
     * @param request 修改请求，各字段为 null 表示不修改
     * @return 更新后的用户资料 VO
     */
    @Override
    public UserProfileVO updateProfile(Long userId, UpdateProfileDTO request) {
        MallUserDO user = mallUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 仅更新传入的非空字段，null 表示不修改
        UserProfileConvert.merge(request, user);
        user.setUpdateTime(LocalDateTime.now());
        mallUserMapper.updateById(user);

        // 更新后清除缓存，下次查询时重新加载最新资料
        String cacheKey = CacheConstants.User.PROFILE + userId;
        redisTemplate.delete(cacheKey);

        log.info("更新用户资料成功, userId={}", userId);
        return getProfile(userId);
    }

    /**
     * 构建用户资料 VO
     *
     * <p>聚合用户基本信息、会员等级、积分账户，并通过 Feign 对手机号脱敏</p>
     *
     * @param userId 用户 ID
     * @return 用户资料 VO
     */
    private UserProfileVO buildProfile(Long userId) {
        MallUserDO user = mallUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        String maskedPhone = remoteAuthAdapter.maskPhone(user.getPhone());

        MallUserMemberDO member = getMemberByUserId(userId);
        MallUserMemberLevelDO level = null;
        if (member != null) {
            List<MallUserMemberLevelDO> levels = mallUserMemberLevelMapper.selectList(null);
            level = levels.stream()
                    .filter(l -> l.getId().equals(member.getLevelId()))
                    .findFirst()
                    .orElse(null);
        }

        MallPointsAccountDO account = getPointsAccountByUserId(userId);

        return UserConvert.toUserProfileVO(user, maskedPhone, member, level, account);
    }

    /**
     * 根据用户 ID 查询会员信息
     *
     * @param userId 用户 ID
     * @return 会员 DO，未找到返回 null
     */
    private MallUserMemberDO getMemberByUserId(Long userId) {
        LambdaQueryWrapper<MallUserMemberDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserMemberDO::getUserId, userId)
                .eq(MallUserMemberDO::getIsDeleted, 0);
        return mallUserMemberMapper.selectOne(wrapper);
    }

    /**
     * 根据用户 ID 查询积分账户
     *
     * @param userId 用户 ID
     * @return 积分账户 DO，未找到返回 null
     */
    private MallPointsAccountDO getPointsAccountByUserId(Long userId) {
        LambdaQueryWrapper<MallPointsAccountDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallPointsAccountDO::getUserId, userId)
                .eq(MallPointsAccountDO::getIsDeleted, 0);
        return mallPointsAccountMapper.selectOne(wrapper);
    }
}
