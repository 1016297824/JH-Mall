package com.mall.user.mapper;

import java.util.List;
import com.mall.user.domain.MallUser;
import org.apache.ibatis.annotations.Param;

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
     * 删除用户账号
     * 
     * @param id 用户账号主键
     * @return 结果
     */
    int deleteMallUserById(String id);

    /**
     * 批量删除用户账号
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallUserByIds(String[] ids);

    /**
     * 按 phone_hash 查询用户
     */
    MallUser selectByPhoneHash(@Param("phoneHash") String phoneHash);

    /**
     * 更新密码
     */
    int updatePassword(@Param("id") String id, @Param("password") String password);

    /**
     * 更新手机号
     */
    int updatePhone(@Param("id") String id, @Param("phone") String phone, @Param("phoneHash") String phoneHash);

    /**
     * 更新用户状态
     */
    int updateUserStatus(@Param("id") String id, @Param("userStatus") String userStatus);
}
