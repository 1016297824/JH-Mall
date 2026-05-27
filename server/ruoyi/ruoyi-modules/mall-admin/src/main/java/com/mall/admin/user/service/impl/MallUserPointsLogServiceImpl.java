package com.mall.admin.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.admin.user.mapper.MallUserPointsLogMapper;
import com.mall.admin.user.domain.MallUserPointsLog;
import com.mall.admin.user.service.IMallUserPointsLogService;

/**
 * 积分流水Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserPointsLogServiceImpl implements IMallUserPointsLogService
{
    @Autowired
    private MallUserPointsLogMapper mallUserPointsLogMapper;

    /**
     * 查询积分流水
     *
     * @param id 积分流水主键
     * @return 积分流水
     */
    @Override
    public MallUserPointsLog selectMallUserPointsLogById(String id)
    {
        return mallUserPointsLogMapper.selectMallUserPointsLogById(id);
    }

    /**
     * 查询积分流水列表
     *
     * @param mallUserPointsLog 积分流水
     * @return 积分流水
     */
    @Override
    public List<MallUserPointsLog> selectMallUserPointsLogList(MallUserPointsLog mallUserPointsLog)
    {
        return mallUserPointsLogMapper.selectMallUserPointsLogList(mallUserPointsLog);
    }

    /**
     * 新增积分流水
     *
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    @Override
    public int insertMallUserPointsLog(MallUserPointsLog mallUserPointsLog)
    {
        mallUserPointsLog.setCreateTime(DateUtils.getNowDate());
        return mallUserPointsLogMapper.insertMallUserPointsLog(mallUserPointsLog);
    }

    /**
     * 修改积分流水
     *
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    @Override
    public int updateMallUserPointsLog(MallUserPointsLog mallUserPointsLog)
    {
        mallUserPointsLog.setUpdateTime(DateUtils.getNowDate());
        return mallUserPointsLogMapper.updateMallUserPointsLog(mallUserPointsLog);
    }

    /**
     * 批量删除积分流水
     *
     * @param ids 需要删除的积分流水主键
     * @return 结果
     */
    @Override
    public int deleteMallUserPointsLogByIds(String[] ids)
    {
        return mallUserPointsLogMapper.deleteMallUserPointsLogByIds(ids);
    }

    /**
     * 删除积分流水信息
     *
     * @param id 积分流水主键
     * @return 结果
     */
    @Override
    public int deleteMallUserPointsLogById(String id)
    {
        return mallUserPointsLogMapper.deleteMallUserPointsLogById(id);
    }
}
