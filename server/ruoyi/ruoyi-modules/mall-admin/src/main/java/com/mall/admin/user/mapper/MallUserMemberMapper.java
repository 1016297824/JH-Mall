package com.mall.admin.user.mapper;

import java.util.List;
import com.mall.admin.user.domain.MallUserMember;

/**
 * 用户会员信息Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserMemberMapper
{
    /**
     * 查询用户会员信息
     *
     * @param id 用户会员信息主键
     * @return 用户会员信息
     */
    MallUserMember selectMallUserMemberById(String id);

    /**
     * 查询用户会员信息列表
     *
     * @param mallUserMember 用户会员信息
     * @return 用户会员信息集合
     */
    List<MallUserMember> selectMallUserMemberList(MallUserMember mallUserMember);

    /**
     * 新增用户会员信息
     *
     * @param mallUserMember 用户会员信息
     * @return 结果
     */
    int insertMallUserMember(MallUserMember mallUserMember);

    /**
     * 修改用户会员信息
     *
     * @param mallUserMember 用户会员信息
     * @return 结果
     */
    int updateMallUserMember(MallUserMember mallUserMember);

    /**
     * 删除用户会员信息
     *
     * @param id 用户会员信息主键
     * @return 结果
     */
    int deleteMallUserMemberById(String id);

    /**
     * 批量删除用户会员信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallUserMemberByIds(String[] ids);
}
