package com.mall.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.DO.MallUserMember;
import com.mall.user.service.IMallUserMemberService;

/**
 * 用户会员信息Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserMemberServiceImpl implements IMallUserMemberService
{
    @Autowired
    private MallUserMemberMapper mallUserMemberMapper;

    /**
     * 查询用户会员信息
     *
     * @param id 用户会员信息主键
     * @return 用户会员信息
     */
    @Override
    public MallUserMember selectMallUserMemberById(String id)
    {
        return mallUserMemberMapper.selectMallUserMemberById(id);
    }

    /**
     * 查询用户会员信息列表
     *
     * @param mallUserMember 用户会员信息
     * @return 用户会员信息
     */
    @Override
    public List<MallUserMember> selectMallUserMemberList(MallUserMember mallUserMember)
    {
        return mallUserMemberMapper.selectMallUserMemberList(mallUserMember);
    }

    /**
     * 新增用户会员信息
     *
     * @param mallUserMember 用户会员信息
     * @return 结果
     */
    @Override
    public int insertMallUserMember(MallUserMember mallUserMember)
    {
        mallUserMember.setCreateTime(DateUtils.getNowDate());
        return mallUserMemberMapper.insertMallUserMember(mallUserMember);
    }

    /**
     * 修改用户会员信息
     *
     * @param mallUserMember 用户会员信息
     * @return 结果
     */
    @Override
    public int updateMallUserMember(MallUserMember mallUserMember)
    {
        mallUserMember.setUpdateTime(DateUtils.getNowDate());
        return mallUserMemberMapper.updateMallUserMember(mallUserMember);
    }

    /**
     * 批量删除用户会员信息
     *
     * @param ids 需要删除的用户会员信息主键
     * @return 结果
     */
    @Override
    public int deleteMallUserMemberByIds(String[] ids)
    {
        return mallUserMemberMapper.deleteMallUserMemberByIds(ids);
    }

    /**
     * 删除用户会员信息信息
     *
     * @param id 用户会员信息主键
     * @return 结果
     */
    @Override
    public int deleteMallUserMemberById(String id)
    {
        return mallUserMemberMapper.deleteMallUserMemberById(id);
    }
}
