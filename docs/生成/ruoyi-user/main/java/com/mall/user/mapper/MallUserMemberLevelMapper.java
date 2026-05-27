package com.mall.user.mapper;

import java.util.List;
import com.mall.user.DO.MallUserMemberLevel;

/**
 * 会员等级定义Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserMemberLevelMapper
{
    /**
     * 查询会员等级定义
     *
     * @param id 会员等级定义主键
     * @return 会员等级定义
     */
    MallUserMemberLevel selectMallUserMemberLevelById(String id);

    /**
     * 查询会员等级定义列表
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 会员等级定义集合
     */
    List<MallUserMemberLevel> selectMallUserMemberLevelList(MallUserMemberLevel mallUserMemberLevel);

    /**
     * 新增会员等级定义
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 结果
     */
    int insertMallUserMemberLevel(MallUserMemberLevel mallUserMemberLevel);

    /**
     * 修改会员等级定义
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 结果
     */
    int updateMallUserMemberLevel(MallUserMemberLevel mallUserMemberLevel);

    /**
     * 删除会员等级定义
     *
     * @param id 会员等级定义主键
     * @return 结果
     */
    int deleteMallUserMemberLevelById(String id);

    /**
     * 批量删除会员等级定义
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallUserMemberLevelByIds(String[] ids);
}
