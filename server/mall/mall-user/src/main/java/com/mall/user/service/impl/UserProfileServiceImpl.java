package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.GenderEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.DTO.request.UpdateProfileRequest;
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
import java.time.format.DateTimeFormatter;
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

    private final MallUserMapper mallUserMapper;

    private final MallUserMemberMapper mallUserMemberMapper;

    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;

    private final MallPointsAccountMapper mallPointsAccountMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RemoteAuthAdapter remoteAuthAdapter;

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
        UserProfileVO cached = (UserProfileVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

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
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        MallUserDO user = mallUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(parseBirthday(request.getBirthday()));
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        user.setUpdateTime(LocalDateTime.now());
        mallUserMapper.updateById(user);

        // 清除缓存，下次查询时重新加载
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

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(String.valueOf(user.getId()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setGenderName(getGenderName(user.getGender()));
        if (user.getBirthday() != null) {
            vo.setBirthday(user.getBirthday().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        // 通过 Feign 调用 auth 服务脱敏手机号
        vo.setPhone(remoteAuthAdapter.maskPhone(user.getPhone()));
        vo.setEmail(user.getEmail());

        MallUserMemberDO member = getMemberByUserId(userId);
        if (member != null) {
            vo.setGrowth(member.getGrowth());
            vo.setTotalGrowth(member.getTotalGrowth());
            List<MallUserMemberLevelDO> levels = mallUserMemberLevelMapper.selectList(null);
            MallUserMemberLevelDO currentLevel = levels.stream()
                    .filter(l -> l.getId().equals(member.getLevelId()))
                    .findFirst()
                    .orElse(null);
            if (currentLevel != null) {
                vo.setMembershipLevel(currentLevel.getLevelName());
                vo.setMembershipIcon(currentLevel.getIcon());
            }
        }

        MallPointsAccountDO account = getPointsAccountByUserId(userId);
        if (account != null) {
            vo.setPoints(account.getTotalPoints());
            vo.setAvailablePoints(account.getAvailablePoints());
        }

        return vo;
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

    /**
     * 根据性别编码获取中文描述
     *
     * @param gender 性别编码
     * @return 性别中文描述，null 或未知编码返回"未知"
     */
    private String getGenderName(Integer gender) {
        if (gender == null) {
            return GenderEnum.UNKNOWN.getDescription();
        }
        for (GenderEnum e : GenderEnum.values()) {
            if (e.getCode() == gender) {
                return e.getDescription();
            }
        }
        return GenderEnum.UNKNOWN.getDescription();
    }

    /**
     * 将生日字符串解析为 LocalDateTime
     *
     * @param birthday 生日字符串，格式 yyyy-MM-dd
     * @return 解析后的 LocalDateTime，解析失败返回 null
     */
    private LocalDateTime parseBirthday(String birthday) {
        try {
            return LocalDateTime.parse(birthday + "T00:00:00");
        } catch (Exception e) {
            log.warn("解析生日失败, birthday={}", birthday, e);
            return null;
        }
    }
}
