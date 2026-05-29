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
 * <p>提供用户注册、查询、密码更新、手机号更新、状态管理等核心功能。
 * 手机号存储前做 SHA-256 哈希，注册时同步初始化会员信息和积分账户</p>
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

    /**
     * 根据手机号查询用户
     *
     * <p>先将手机号做 SHA-256 哈希，再查 phone_hash 字段</p>
     *
     * @param phone 明文手机号
     * @return 用户 DO，未找到返回 null
     */
    @Override
    public MallUserDO selectByPhone(String phone) {
        String phoneHash = sha256(phone);
        return selectByPhoneHash(phoneHash);
    }

    /**
     * 根据手机号哈希查询用户
     *
     * @param phoneHash SHA-256 哈希后的手机号
     * @return 用户 DO，未找到返回 null
     */
    @Override
    public MallUserDO selectByPhoneHash(String phoneHash) {
        LambdaQueryWrapper<MallUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserDO::getPhoneHash, phoneHash)
               .eq(MallUserDO::getIsDeleted, 0);
        return mallUserMapper.selectOne(wrapper);
    }

    /**
     * 根据用户 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户 DO，未找到返回 null
     */
    @Override
    public MallUserDO selectById(Long userId) {
        return mallUserMapper.selectById(userId);
    }

    /**
     * 手机号注册新用户
     *
     * <p>在一个事务内完成：插入用户记录 → 初始化会员信息 → 初始化积分账户</p>
     *
     * @param phone     明文手机号
     * @param phoneHash SHA-256 哈希后的手机号
     * @param password  加密后的密码
     * @return 新用户 ID 字符串
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerByPhone(String phone, String phoneHash, String password) {
        MallUserDO user = new MallUserDO();
        user.setPhone(phone);
        user.setPhoneHash(phoneHash);
        user.setPassword(password);
        // 默认昵称取手机号后 4 位，注册后可修改
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

        // 同步初始化会员信息和积分账户
        initMemberInfo(userId);
        initPointsAccount(userId);

        return String.valueOf(userId);
    }

    /**
     * 初始化会员信息
     *
     * <p>创建默认会员记录，等级 ID 为 1（普通会员），成长值为 0</p>
     *
     * @param userId 用户 ID
     */
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

    /**
     * 初始化积分账户
     *
     * <p>创建积分账户记录，各项积分数值均为 0</p>
     *
     * @param userId 用户 ID
     */
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

    /**
     * 更新用户密码
     *
     * @param userId      用户 ID 字符串
     * @param newPassword 新密码（加密后）
     */
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

    /**
     * 更新用户手机号
     *
     * @param userId       用户 ID 字符串
     * @param newPhone     新手机号明文
     * @param newPhoneHash 新手机号 SHA-256 哈希
     */
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

    /**
     * 更新用户状态
     *
     * @param userId     用户 ID 字符串
     * @param userStatus 目标状态编码
     */
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

    /**
     * SHA-256 哈希
     *
     * @param input 待哈希的原始字符串
     * @return 哈希后的十六进制小写字符串
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // 将字节数组转为十六进制小写字符串
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
