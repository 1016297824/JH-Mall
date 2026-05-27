package com.mall.user.mapper;

import java.util.List;
import com.mall.user.domain.MallUserGrowthLog;

/**
 * 成长值流水Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface MallUserGrowthLogMapper 
{
    /**
     * 查询成长值流水
     * 
     * @param id 成长值流水主键
     * @return 成长值流水
     */
    MallUserGrowthLog selectMallUserGrowthLogById(String id);

    /**
     * 查询成长值流水列表
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 成长值流水集合
     */
    List<MallUserGrowthLog> selectMallUserGrowthLogList(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 新增成长值流水
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    int insertMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 修改成长值流水
     * 
     * @param mallUserGrowthLog 成长值流水
     * @return 结果
     */
    int updateMallUserGrowthLog(MallUserGrowthLog mallUserGrowthLog);

    /**
     * 删除成长值流水
     * 
     * @param id 成长值流水主键
     * @return 结果
     */
    int deleteMallUserGrowthLogById(String id);

    /**
     * 批量删除成长值流水
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallUserGrowthLogByIds(String[] ids);
}
