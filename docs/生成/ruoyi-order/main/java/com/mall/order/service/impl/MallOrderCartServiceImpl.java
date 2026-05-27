package com.mall.order.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.order.mapper.MallOrderCartMapper;
import com.mall.order.DO.MallOrderCart;
import com.mall.order.service.IMallOrderCartService;

/**
 * 购物车Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallOrderCartServiceImpl implements IMallOrderCartService
{
    @Autowired
    private MallOrderCartMapper mallOrderCartMapper;

    /**
     * 查询购物车
     *
     * @param id 购物车主键
     * @return 购物车
     */
    @Override
    public MallOrderCart selectMallOrderCartById(String id)
    {
        return mallOrderCartMapper.selectMallOrderCartById(id);
    }

    /**
     * 查询购物车列表
     *
     * @param mallOrderCart 购物车
     * @return 购物车
     */
    @Override
    public List<MallOrderCart> selectMallOrderCartList(MallOrderCart mallOrderCart)
    {
        return mallOrderCartMapper.selectMallOrderCartList(mallOrderCart);
    }

    /**
     * 新增购物车
     *
     * @param mallOrderCart 购物车
     * @return 结果
     */
    @Override
    public int insertMallOrderCart(MallOrderCart mallOrderCart)
    {
        mallOrderCart.setCreateTime(DateUtils.getNowDate());
        return mallOrderCartMapper.insertMallOrderCart(mallOrderCart);
    }

    /**
     * 修改购物车
     *
     * @param mallOrderCart 购物车
     * @return 结果
     */
    @Override
    public int updateMallOrderCart(MallOrderCart mallOrderCart)
    {
        mallOrderCart.setUpdateTime(DateUtils.getNowDate());
        return mallOrderCartMapper.updateMallOrderCart(mallOrderCart);
    }

    /**
     * 批量删除购物车
     *
     * @param ids 需要删除的购物车主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderCartByIds(String[] ids)
    {
        return mallOrderCartMapper.deleteMallOrderCartByIds(ids);
    }

    /**
     * 删除购物车信息
     *
     * @param id 购物车主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderCartById(String id)
    {
        return mallOrderCartMapper.deleteMallOrderCartById(id);
    }
}
