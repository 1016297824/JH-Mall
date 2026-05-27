package com.mall.admin.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.admin.user.mapper.MallUserGrowthLogMapper;
import com.mall.admin.user.domain.MallUserGrowthLog;
import com.mall.admin.user.service.IMallUserGrowthLogService;

/**
 * 成长值流水Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserGrowthLogServiceImpl implements IMallUserGrowthLogService
{
    @Autowired
    private MallUserGrowthLogMapper mallUserGrowthLogMapper;

    /**
     * 查询成长值流水
     *
     * @param id 成长值流水主键
     * @return 成长值流水
     */
    @Override
    public MallUserGrowthLog selectMallUserGrowthLogById(String id)
    {
        return mallUserGrowthLogMapper.selectMallUserGrowthLogById(id);
    }

    /**
     * 查询成长值流水列表
     *
     * @param mallUserGrowthLog 成长值流水
     * @return 成长值流水
     */
    @Override
    public List<MallUserGrowthLog> selectMallUserGrowthLogList(MallUserGrowthLog mallUserGrowthLog)
    {
        return mallUserGrowthLogMapper.selectMallUserGrowthLogList(mallUserGrowthLog);
    }

    /**
     * 新增成长值流水
     *
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    @Override
    public int insertMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog)
    {
        mallUserGrowthLog.setCreateTime(DateUtils.getNowDate());
        return mallUserGrowthLogMapper.insertMallUserGrowthLog(mallUserGrowthLog);
    }

    /**
     * 修改成长值流水
     *
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    @Override
    public int updateMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog)
    {
        mallUserGrowthLog.setUpdateTime(DateUtils.getNowDate());
        return mallUserGrowthLogMapper.updateMallUserGrowthLog(mallUserGrowthLog);
    }

    /**
     * 批量删除成长值流水
     *
     * @param ids 需要删除的成长值流水主键
     * @return 结果
     */
    @Override
    public int deleteMallUserGrowthLogByIds(String[] ids)
    {
        return mallUserGrowthLogMapper.deleteMallUserGrowthLogByIds(ids);
    }

    /**
     * 删除成长值流水信息
     *
     * @param id 成长值流水主键
     * @return 结果
     */
    @Override
    public int deleteMallUserGrowthLogById(String id)
    {
        return mallUserGrowthLogMapper.deleteMallUserGrowthLogById(id);
    }
}
