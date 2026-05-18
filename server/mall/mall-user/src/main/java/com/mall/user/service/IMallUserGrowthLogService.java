package com.mall.user.service;

import java.util.List;
import com.mall.user.domain.MallUserGrowthLog;

/**
 * 成长值流水Service接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface IMallUserGrowthLogService 
{
    /**
     * 查询成长值流水
     * 
     * @param id 成长值流水主键
     * @return 成长值流水
     */
    public MallUserGrowthLog selectMallUserGrowthLogById(String id);

    /**
     * 查询成长值流水列表
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 成长值流水集合
     */
    public List<MallUserGrowthLog> selectMallUserGrowthLogList(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 新增成长值流水
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    public int insertMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 修改成长值流水
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    public int updateMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 批量删除成长值流水
     * 
     * @param ids 需要删除的成长值流水主键集合
     * @return 结果
     */
    public int deleteMallUserGrowthLogByIds(String[] ids);

    /**
     * 删除成长值流水信息
     * 
     * @param id 成长值流水主键
     * @return 结果
     */
    public int deleteMallUserGrowthLogById(String id);
}
