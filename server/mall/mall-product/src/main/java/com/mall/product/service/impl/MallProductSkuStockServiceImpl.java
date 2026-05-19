package com.mall.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.product.mapper.MallProductSkuStockMapper;
import com.mall.product.domain.MallProductSkuStock;
import com.mall.product.service.IMallProductSkuStockService;

/**
 * 库存管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallProductSkuStockServiceImpl implements IMallProductSkuStockService 
{
    @Autowired
    private MallProductSkuStockMapper mallProductSkuStockMapper;

    /**
     * 查询库存管理
     * 
     * @param id 库存管理主键
     * @return 库存管理
     */
    @Override
    public MallProductSkuStock selectMallProductSkuStockById(String id)
    {
        return mallProductSkuStockMapper.selectMallProductSkuStockById(id);
    }

    /**
     * 查询库存管理列表
     * 
     * @param mallProductSkuStock 库存管理
     * @return 库存管理
     */
    @Override
    public List<MallProductSkuStock> selectMallProductSkuStockList(MallProductSkuStock mallProductSkuStock)
    {
        return mallProductSkuStockMapper.selectMallProductSkuStockList(mallProductSkuStock);
    }

    /**
     * 新增库存管理
     * 
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    @Override
    public int insertMallProductSkuStock(MallProductSkuStock mallProductSkuStock)
    {
        mallProductSkuStock.setCreateTime(DateUtils.getNowDate());
        return mallProductSkuStockMapper.insertMallProductSkuStock(mallProductSkuStock);
    }

    /**
     * 修改库存管理
     * 
     * @param mallProductSkuStock 库存管理
     * @return 结果
     */
    @Override
    public int updateMallProductSkuStock(MallProductSkuStock mallProductSkuStock)
    {
        mallProductSkuStock.setUpdateTime(DateUtils.getNowDate());
        return mallProductSkuStockMapper.updateMallProductSkuStock(mallProductSkuStock);
    }

    /**
     * 批量删除库存管理
     * 
     * @param ids 需要删除的库存管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSkuStockByIds(String[] ids)
    {
        return mallProductSkuStockMapper.deleteMallProductSkuStockByIds(ids);
    }

    /**
     * 删除库存管理信息
     * 
     * @param id 库存管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSkuStockById(String id)
    {
        return mallProductSkuStockMapper.deleteMallProductSkuStockById(id);
    }
}
