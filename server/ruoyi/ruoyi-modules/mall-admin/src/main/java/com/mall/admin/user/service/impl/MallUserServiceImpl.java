package com.mall.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
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
}
