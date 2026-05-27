package com.mall.order.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.order.mapper.MallOrderAfterSaleMapper;
import com.mall.order.DO.MallOrderAfterSale;
import com.mall.order.service.IMallOrderAfterSaleService;

/**
 * 售后管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallOrderAfterSaleServiceImpl implements IMallOrderAfterSaleService
{
    @Autowired
    private MallOrderAfterSaleMapper mallOrderAfterSaleMapper;

    /**
     * 查询售后管理
     *
     * @param id 售后管理主键
     * @return 售后管理
     */
    @Override
    public MallOrderAfterSale selectMallOrderAfterSaleById(String id)
    {
        return mallOrderAfterSaleMapper.selectMallOrderAfterSaleById(id);
    }

    /**
     * 查询售后管理列表
     *
     * @param mallOrderAfterSale 售后管理
     * @return 售后管理
     */
    @Override
    public List<MallOrderAfterSale> selectMallOrderAfterSaleList(MallOrderAfterSale mallOrderAfterSale)
    {
        return mallOrderAfterSaleMapper.selectMallOrderAfterSaleList(mallOrderAfterSale);
    }

    /**
     * 新增售后管理
     *
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    @Override
    public int insertMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale)
    {
        mallOrderAfterSale.setCreateTime(DateUtils.getNowDate());
        return mallOrderAfterSaleMapper.insertMallOrderAfterSale(mallOrderAfterSale);
    }

    /**
     * 修改售后管理
     *
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    @Override
    public int updateMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale)
    {
        mallOrderAfterSale.setUpdateTime(DateUtils.getNowDate());
        return mallOrderAfterSaleMapper.updateMallOrderAfterSale(mallOrderAfterSale);
    }

    /**
     * 批量删除售后管理
     *
     * @param ids 需要删除的售后管理主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderAfterSaleByIds(String[] ids)
    {
        return mallOrderAfterSaleMapper.deleteMallOrderAfterSaleByIds(ids);
    }

    /**
     * 删除售后管理信息
     *
     * @param id 售后管理主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderAfterSaleById(String id)
    {
        return mallOrderAfterSaleMapper.deleteMallOrderAfterSaleById(id);
    }
}
