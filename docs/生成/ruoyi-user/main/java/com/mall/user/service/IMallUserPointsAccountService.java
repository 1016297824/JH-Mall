package com.mall.user.service;

import java.util.List;
import com.mall.user.domain.MallUserPointsAccount;

/**
 * 积分账户Service接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface IMallUserPointsAccountService 
{
    /**
     * 查询积分账户
     * 
     * @param id 积分账户主键
     * @return 积分账户
     */
    public MallUserPointsAccount selectMallUserPointsAccountById(String id);

    /**
     * 查询积分账户列表
     * 
     * @param mallUserPointsAccount 积分账户
     * @return 积分账户集合
     */
    public List<MallUserPointsAccount> selectMallUserPointsAccountList(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 新增积分账户
     * 
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    public int insertMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 修改积分账户
     * 
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    public int updateMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount);

    /**
     * 批量删除积分账户
     * 
     * @param ids 需要删除的积分账户主键集合
     * @return 结果
     */
    public int deleteMallUserPointsAccountByIds(String[] ids);

    /**
     * 删除积分账户信息
     * 
     * @param id 积分账户主键
     * @return 结果
     */
    public int deleteMallUserPointsAccountById(String id);
}
