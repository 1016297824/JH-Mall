package com.mall.user.service;

import java.util.List;
import com.mall.user.domain.MallUserPointsLog;

/**
 * 积分流水Service接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface IMallUserPointsLogService 
{
    /**
     * 查询积分流水
     * 
     * @param id 积分流水主键
     * @return 积分流水
     */
    MallUserPointsLog selectMallUserPointsLogById(String id);

    /**
     * 查询积分流水列表
     * 
     * @param mallUserPointsLog 积分流水
     * @return 积分流水集合
     */
    List<MallUserPointsLog> selectMallUserPointsLogList(MallUserPointsLog mallUserPointsLog);

    /**
     * 新增积分流水
     * 
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    int insertMallUserPointsLog(MallUserPointsLog mallUserPointsLog);

    /**
     * 修改积分流水
     * 
     * @param mallUserPointsLog 积分流水
     * @return 结果
     */
    int updateMallUserPointsLog(MallUserPointsLog mallUserPointsLog);

    /**
     * 批量删除积分流水
     * 
     * @param ids 需要删除的积分流水主键集合
     * @return 结果
     */
    int deleteMallUserPointsLogByIds(String[] ids);

    /**
     * 删除积分流水信息
     * 
     * @param id 积分流水主键
     * @return 结果
     */
    int deleteMallUserPointsLogById(String id);
}
