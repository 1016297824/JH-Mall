package com.mall.user.service;

import java.util.List;
import com.mall.user.DO.MallUser;

/**
 * 用户账号Service接口
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public interface IMallUserService
{
    /**
     * 查询用户账号
     *
     * @param id 用户账号主键
     * @return 用户账号
     */
    MallUser selectMallUserById(String id);

    /**
     * 查询用户账号列表
     *
     * @param mallUser 用户账号
     * @return 用户账号集合
     */
    List<MallUser> selectMallUserList(MallUser mallUser);

    /**
     * 新增用户账号
     *
     * @param mallUser 用户账号
     * @return 结果
     */
    int insertMallUser(MallUser mallUser);

    /**
     * 修改用户账号
     *
     * @param mallUser 用户账号
     * @return 结果
     */
    int updateMallUser(MallUser mallUser);

    /**
     * 批量删除用户账号
     *
     * @param ids 需要删除的用户账号主键集合
     * @return 结果
     */
    int deleteMallUserByIds(String[] ids);

    /**
     * 删除用户账号信息
     *
     * @param id 用户账号主键
     * @return 结果
     */
    int deleteMallUserById(String id);
}
