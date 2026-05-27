package com.mall.admin.product.service;

import java.util.List;
import com.mall.admin.product.domain.MallProductSkuStock;

/**
 * 库存管理Service接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallProductSkuStockService
{
    /**
     * 查询库存管理
     *
     * @param id 库存管理主键
     * @return 库存管理
     */
    public MallProductSkuStock selectMallProductSkuStockById(String id);

    /**
     * 查询库存管理列表
     *
     * @param mallProductSkuStock 库存管理
     * @return 库存管理集合
     */
    public List<MallProductSkuStock> selectMallProductSkuStockList(MallProductSkuStock mallProductSkuStock);

    /**
     * 新增库存管理
     *
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    public int insertMallProductSkuStock(MallProductSkuStock mallProductSkuStock);

    /**
     * 修改库存管理
     *
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    public int updateMallProductSkuStock(MallProductSkuStock mallProductSkuStock);

    /**
     * 批量删除库存管理
     *
     * @param ids 需要删除的库存管理主键集合
     * @return 结果
     */
    public int deleteMallProductSkuStockByIds(String[] ids);

    /**
     * 删除库存管理信息
     *
     * @param id 库存管理主键
     * @return 结果
     */
    public int deleteMallProductSkuStockById(String id);
}
