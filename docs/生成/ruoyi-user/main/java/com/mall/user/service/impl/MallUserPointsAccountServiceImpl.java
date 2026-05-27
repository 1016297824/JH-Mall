package com.mall.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import com.mall.user.DO.MallUserPointsAccount;
import com.mall.user.service.IMallUserPointsAccountService;

/**
 * 积分账户Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserPointsAccountServiceImpl implements IMallUserPointsAccountService
{
    @Autowired
    private MallUserPointsAccountMapper mallUserPointsAccountMapper;

    /**
     * 查询积分账户
     *
     * @param id 积分账户主键
     * @return 积分账户
     */
    @Override
    public MallUserPointsAccount selectMallUserPointsAccountById(String id)
    {
        return mallUserPointsAccountMapper.selectMallUserPointsAccountById(id);
    }

    /**
     * 查询积分账户列表
     *
     * @param mallUserPointsAccount 积分账户
     * @return 积分账户
     */
    @Override
    public List<MallUserPointsAccount> selectMallUserPointsAccountList(MallUserPointsAccount mallUserPointsAccount)
    {
        return mallUserPointsAccountMapper.selectMallUserPointsAccountList(mallUserPointsAccount);
    }

    /**
     * 新增积分账户
     *
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    @Override
    public int insertMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount)
    {
        mallUserPointsAccount.setCreateTime(DateUtils.getNowDate());
        return mallUserPointsAccountMapper.insertMallUserPointsAccount(mallUserPointsAccount);
    }

    /**
     * 修改积分账户
     *
     * @param mallUserPointsAccount 积分账户
     * @return 结果
     */
    @Override
    public int updateMallUserPointsAccount(MallUserPointsAccount mallUserPointsAccount)
    {
        mallUserPointsAccount.setUpdateTime(DateUtils.getNowDate());
        return mallUserPointsAccountMapper.updateMallUserPointsAccount(mallUserPointsAccount);
    }

    /**
     * 批量删除积分账户
     *
     * @param ids 需要删除的积分账户主键
     * @return 结果
     */
    @Override
    public int deleteMallUserPointsAccountByIds(String[] ids)
    {
        return mallUserPointsAccountMapper.deleteMallUserPointsAccountByIds(ids);
    }

    /**
     * 删除积分账户信息
     *
     * @param id 积分账户主键
     * @return 结果
     */
    @Override
    public int deleteMallUserPointsAccountById(String id)
    {
        return mallUserPointsAccountMapper.deleteMallUserPointsAccountById(id);
    }
}
