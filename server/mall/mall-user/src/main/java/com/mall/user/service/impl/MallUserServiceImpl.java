package com.mall.user.service.impl;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.ruoyi.common.core.utils.DateUtils;
import com.mall.common.enums.user.UserStatusEnum;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.domain.MallUser;
import com.mall.user.service.IMallUserService;

/**
 * 用户账号Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserServiceImpl implements IMallUserService 
{
    @Autowired
    private MallUserMapper mallUserMapper;

    /**
     * 查询用户账号
     * 
     * @param id 用户账号主键
     * @return 用户账号
     */
    @Override
    public MallUser selectMallUserById(String id)
    {
        return mallUserMapper.selectMallUserById(id);
    }

    /**
     * 查询用户账号列表
     * 
     * @param mallUser 用户账号
     * @return 用户账号
     */
    @Override
    public List<MallUser> selectMallUserList(MallUser mallUser)
    {
        return mallUserMapper.selectMallUserList(mallUser);
    }

    /**
     * 新增用户账号
     * 
     * @param mallUser 用户账号
     * @return 结果
     */
    @Override
    public int insertMallUser(MallUser mallUser)
    {
        mallUser.setCreateTime(DateUtils.getNowDate());
        return mallUserMapper.insertMallUser(mallUser);
    }

    /**
     * 修改用户账号
     * 
     * @param mallUser 用户账号
     * @return 结果
     */
    @Override
    public int updateMallUser(MallUser mallUser)
    {
        mallUser.setUpdateTime(DateUtils.getNowDate());
        return mallUserMapper.updateMallUser(mallUser);
    }

    /**
     * 批量删除用户账号
     * 
     * @param ids 需要删除的用户账号主键
     * @return 结果
     */
    @Override
    public int deleteMallUserByIds(String[] ids)
    {
        return mallUserMapper.deleteMallUserByIds(ids);
    }

    /**
     * 删除用户账号信息
     * 
     * @param id 用户账号主键
     * @return 结果
     */
    @Override
    public int deleteMallUserById(String id)
    {
        return mallUserMapper.deleteMallUserById(id);
    }

    @Override
    public MallUser selectByPhoneHash(String phoneHash)
    {
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUser selectByPhone(String phone)
    {
        String phoneHash = DigestUtils.sha256Hex(phone);
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUser selectByWechatOpenId(String openId)
    {
        return null;
    }

    @Override
    public String registerByPhone(String phone, String phoneHash, String passwordHash)
    {
        MallUser user = new MallUser();
        user.setId(UUID.randomUUID().toString());
        user.setPhone(phone);
        user.setPhoneHash(phoneHash);
        user.setPassword(passwordHash);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));
        user.setRegisterType("phone");
        user.setIsPrivacyAgreed("1");
        Date now = DateUtils.getNowDate();
        user.setRegisterTime(now);
        user.setCreateTime(now);
        user.setPrivacyAgreedTime(now);
        mallUserMapper.insertMallUser(user);
        return user.getId();
    }

    @Override
    public int updatePasswordById(String id, String newPasswordHash)
    {
        return mallUserMapper.updatePassword(id, newPasswordHash);
    }

    @Override
    public int updatePhoneById(String id, String newPhone, String newPhoneHash)
    {
        return mallUserMapper.updatePhone(id, newPhone, newPhoneHash);
    }

    @Override
    public int updateUserStatusById(String id, String userStatus)
    {
        return mallUserMapper.updateUserStatus(id, userStatus);
    }
}
