package com.mall.order.mapper;

import java.util.List;
import com.mall.order.domain.MallOrderAmount;

/**
 * 金额快照Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallOrderAmountMapper 
{
    /**
     * 查询金额快照
     * 
     * @param id 金额快照主键
     * @return 金额快照
     */
    MallOrderAmount selectMallOrderAmountById(String id);

    /**
     * 查询金额快照列表
     * 
     * @param mallOrderAmount 金额快照
     * @return 金额快照集合
     */
    List<MallOrderAmount> selectMallOrderAmountList(MallOrderAmount mallOrderAmount);

    /**
     * 新增金额快照
     * 
     * @param mallOrderAmount 金额快照
     * @return 结果
     */
    int insertMallOrderAmount(MallOrderAmount mallOrderAmount);

    /**
     * 修改金额快照
     * 
     * @param mallOrderAmount 金额快照
     * @return 结果
     */
    int updateMallOrderAmount(MallOrderAmount mallOrderAmount);

    /**
     * 删除金额快照
     * 
     * @param id 金额快照主键
     * @return 结果
     */
    int deleteMallOrderAmountById(String id);

    /**
     * 批量删除金额快照
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallOrderAmountByIds(String[] ids);
}
