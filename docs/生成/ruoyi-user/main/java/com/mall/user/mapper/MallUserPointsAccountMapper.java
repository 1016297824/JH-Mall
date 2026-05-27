package com.mall.user.mapper;

import java.util.List;
import com.mall.user.DO.MallUserPointsAccount;

/**
 * 积分账户Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserPointsAccountMapper
{
    /**
     * 查询积分账户
     *
     * @param id 积分账户主键
     * @return 积分账户
     */
    MallUserPointsAccount selectMallUserPointsAccountById(String id);

    /**
     * 查询积分账户列表
     *
     * @param mallUserPointsAccount 积分账户
     * @return 积分账户集合
     */
    List<MallUserPointsAccount> selectMallUserPointsAccountList(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 新增积分账户
     *
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    int insertMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 修改积分账户
     *
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    int updateMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 删除积分账户
     *
     * @param id 积分账户主键
     * @return 结果
     */
    int deleteMallUserPointsAccountById(String id);

    /**
     * 批量删除积分账户
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallUserPointsAccountByIds(String[] ids);
}
