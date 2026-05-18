package com.mall.user.mapper;

import java.util.List;
import com.mall.user.domain.MallUser;

/**
 * 用户账号Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserMapper 
{
    /**
     * 查询用户账号
     * 
     * @param id 用户账号主键
     * @return 用户账号
     */
    public MallUser selectMallUserById(String id);

    /**
     * 查询用户账号列表
     * 
     * @param mallUser 用户账号
     * @return 用户账号集合
     */
    public List<MallUser> selectMallUserList(MallUser mallUser);

    /**
     * 新增用户账号
     * 
     * @param mallUser 用户账号
     * @return 结果
     */
    public int insertMallUser(MallUser mallUser);

    /**
     * 修改用户账号
     * 
     * @param mallUser 用户账号
     * @return 结果
     */
    public int updateMallUser(MallUser mallUser);

    /**
     * 删除用户账号
     * 
     * @param id 用户账号主键
     * @return 结果
     */
    public int deleteMallUserById(String id);

    /**
     * 批量删除用户账号
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallUserByIds(String[] ids);
}
