package com.mall.user.service;

import com.mall.user.DO.MallUserDO;

/**
 * 用户服务接口
 *
 * <p>提供用户注册、查询、密码更新、手机号更新、状态管理等核心功能</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface IMallUserService {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体（不存在返回 null）
     */
    MallUserDO selectByPhone(String phone);

    /**
     * 根据手机号哈希查询用户（包含密码）
     *
     * @param phoneHash 手机号 SHA-256 哈希
     * @return 用户实体（不存在返回 null）
     */
    MallUserDO selectByPhoneHash(String phoneHash);

    /**
     * 根据用户 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户实体（不存在返回 null）
     */
    MallUserDO selectById(Long userId);

    /**
     * 手机号注册用户
     *
     * @param phone     手机号
     * @param phoneHash 手机号 SHA-256 哈希
     * @param password  密码 BCrypt 哈希
     * @return 新建用户 ID
     */
    String registerByPhone(String phone, String phoneHash, String password);

    /**
     * 更新用户密码
     *
     * @param userId       用户 ID
     * @param newPassword  新密码 BCrypt 哈希
     */
    void updatePasswordById(String userId, String newPassword);

    /**
     * 更新用户手机号
     *
     * @param userId      用户 ID
     * @param newPhone    新手机号
     * @param newPhoneHash 新手机号 SHA-256 哈希
     */
    void updatePhoneById(String userId, String newPhone, String newPhoneHash);

    /**
     * 更新用户状态
     *
     * @param userId     用户 ID
     * @param userStatus 用户状态
     */
    void updateUserStatusById(String userId, String userStatus);

    /**
     * 递增用户 token_version（原子操作，InnoDB 行锁保证）
     *
     * @param userId 用户 ID
     * @return 受影响行数（0 表示用户不存在）
     */
    int incrementTokenVersion(Long userId);

    /**
     * 查询用户 token_version
     *
     * @param userId 用户 ID
     * @return token_version 值，用户不存在返回 null
     */
    Integer getTokenVersion(Long userId);
}
