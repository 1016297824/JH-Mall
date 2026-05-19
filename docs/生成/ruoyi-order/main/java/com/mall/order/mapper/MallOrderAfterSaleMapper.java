package com.mall.order.mapper;

import java.util.List;
import com.mall.order.domain.MallOrderAfterSale;

/**
 * 售后管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallOrderAfterSaleMapper 
{
    /**
     * 查询售后管理
     * 
     * @param id 售后管理主键
     * @return 售后管理
     */
    public MallOrderAfterSale selectMallOrderAfterSaleById(String id);

    /**
     * 查询售后管理列表
     * 
     * @param mallOrderAfterSale 售后管理
     * @return 售后管理集合
     */
    public List<MallOrderAfterSale> selectMallOrderAfterSaleList(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 新增售后管理
     * 
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    public int insertMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 修改售后管理
     * 
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    public int updateMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 删除售后管理
     * 
     * @param id 售后管理主键
     * @return 结果
     */
    public int deleteMallOrderAfterSaleById(String id);

    /**
     * 批量删除售后管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallOrderAfterSaleByIds(String[] ids);
}
