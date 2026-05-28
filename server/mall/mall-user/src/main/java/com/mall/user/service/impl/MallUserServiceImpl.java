package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.UserStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.IMallUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 *
 * <p>提供用户注册、查询、密码更新、手机号更新、状态管理等核心功能</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallUserServiceImpl implements IMallUserService {

    private final MallUserMapper mallUserMapper;
    private final MallUserMemberMapper mallUserMemberMapper;
    private final MallPointsAccountMapper mallPointsAccountMapper;

    @Override
    public MallUserDO selectByPhone(String phone) {
        String phoneHash = sha256(phone);
        return selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUserDO selectByPhoneHash(String phoneHash) {
        LambdaQueryWrapper<MallUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserDO::getPhoneHash, phoneHash)
               .eq(MallUserDO::getIsDeleted, 0);
        return mallUserMapper.selectOne(wrapper);
    }

    @Override
    public MallUserDO selectById(Long userId) {
        return mallUserMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerByPhone(String phone, String phoneHash, String password) {
        MallUserDO user = new MallUserDO();
        user.setPhone(phone);
        user.setPhoneHash(phoneHash);
        user.setPassword(password);
        user.setNickname("用户" + phone.substring(7));
        user.setUserStatus(UserStatusEnum.NORMAL.getCode());
        user.setRegisterTime(LocalDateTime.now());
        user.setIsDeleted(0);
        user.setIsPrivacyAgreed(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        mallUserMapper.insert(user);
        Long userId = user.getId();
        log.info("用户注册成功, userId={}, phone={}", userId, phone);

        initMemberInfo(userId);
        initPointsAccount(userId);

        return String.valueOf(userId);
    }

    private void initMemberInfo(Long userId) {
        MallUserMemberDO member = new MallUserMemberDO();
        member.setUserId(userId);
        member.setLevelId(1L);
        member.setGrowth(0);
        member.setTotalGrowth(0);
        member.setLevelStartTime(LocalDateTime.now());
        member.setBecomeTime(LocalDateTime.now());
        member.setIsDeleted(0);
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());

        mallUserMemberMapper.insert(member);
        log.info("初始化会员信息成功, userId={}", userId);
    }

    private void initPointsAccount(Long userId) {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setUserId(userId);
        account.setTotalPoints(0);
        account.setAvailablePoints(0);
        account.setUsedPoints(0);
        account.setExpiredPoints(0);
        account.setIsDeleted(0);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());

        mallPointsAccountMapper.insert(account);
        log.info("初始化积分账户成功, userId={}", userId);
    }

    @Override
    public void updatePasswordById(String userId, String newPassword) {
        MallUserDO user = mallUserMapper.selectById(Long.parseLong(userId));
        if (user != null) {
            user.setPassword(newPassword);
            user.setUpdateTime(LocalDateTime.now());
            mallUserMapper.updateById(user);
            log.info("更新密码成功, userId={}", userId);
        }
    }

    @Override
    public void updatePhoneById(String userId, String newPhone, String newPhoneHash) {
        MallUserDO user = mallUserMapper.selectById(Long.parseLong(userId));
        if (user != null) {
            user.setPhone(newPhone);
            user.setPhoneHash(newPhoneHash);
            user.setUpdateTime(LocalDateTime.now());
            mallUserMapper.updateById(user);
            log.info("更新手机号成功, userId={}", userId);
        }
    }

    @Override
    public void updateUserStatusById(String userId, String userStatus) {
        MallUserDO user = mallUserMapper.selectById(Long.parseLong(userId));
        if (user != null) {
            user.setUserStatus(Integer.parseInt(userStatus));
            user.setUpdateTime(LocalDateTime.now());
            mallUserMapper.updateById(user);
            log.info("更新用户状态成功, userId={}, status={}", userId, userStatus);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
