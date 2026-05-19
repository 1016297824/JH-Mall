package com.mall.product.mapper;

import java.util.List;
import com.mall.product.domain.MallProductSkuStock;

/**
 * 库存管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallProductSkuStockMapper 
{
    /**
     * 查询库存管理
     * 
     * @param id 库存管理主键
     * @return 库存管理
     */
    MallProductSkuStock selectMallProductSkuStockById(String id);

    /**
     * 查询库存管理列表
     * 
     * @param mallProductSkuStock 库存管理
     * @return 库存管理集合
     */
    List<MallProductSkuStock> selectMallProductSkuStockList(MallProductSkuStock mallProductSkuStock);

    /**
     * 新增库存管理
     * 
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    int insertMallProductSkuStock(MallProductSkuStock mallProductSkuStock);

    /**
     * 修改库存管理
     * 
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    int updateMallProductSkuStock(MallProductSkuStock mallProductSkuStock);

    /**
     * 删除库存管理
     * 
     * @param id 库存管理主键
     * @return 结果
     */
    int deleteMallProductSkuStockById(String id);

    /**
     * 批量删除库存管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallProductSkuStockByIds(String[] ids);
}
