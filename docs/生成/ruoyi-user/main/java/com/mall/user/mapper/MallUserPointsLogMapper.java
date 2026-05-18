package com.mall.user.mapper;

import java.util.List;
import com.mall.user.domain.MallUserPointsLog;

/**
 * 积分流水Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserPointsLogMapper 
{
    /**
     * 查询积分流水
     * 
     * @param id 积分流水主键
     * @return 积分流水
     */
    public MallUserPointsLog selectMallUserPointsLogById(String id);

    /**
     * 查询积分流水列表
     * 
     * @param mallUserPointsLog 积分流水
     * @return 积分流水集合
     */
    public List<MallUserPointsLog> selectMallUserPointsLogList(MallUserPointsLog mallUserPointsLog);

    /**
     * 新增积分流水
     * 
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    public int insertMallUserPointsLog(MallUserPointsLog mallUserPointsLog);

    /**
     * 修改积分流水
     * 
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    public int updateMallUserPointsLog(MallUserPointsLog mallUserPointsLog);

    /**
     * 删除积分流水
     * 
     * @param id 积分流水主键
     * @return 结果
     */
    public int deleteMallUserPointsLogById(String id);

    /**
     * 批量删除积分流水
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallUserPointsLogByIds(String[] ids);
}
