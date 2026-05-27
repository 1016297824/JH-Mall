package com.mall.order.service;

import java.util.List;
import com.mall.order.DO.MallOrderCart;

/**
 * 购物车Service接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallOrderCartService
{
    /**
     * 查询购物车
     *
     * @param id 购物车主键
     * @return 购物车
     */
    public MallOrderCart selectMallOrderCartById(String id);

    /**
     * 查询购物车列表
     *
     * @param mallOrderCart 购物车
     * @return 购物车集合
     */
    public List<MallOrderCart> selectMallOrderCartList(MallOrderCart mallOrderCart);

    /**
     * 新增购物车
     *
     * @param mallOrderCart 购物车
     * @return 结果
     */
    public int insertMallOrderCart(MallOrderCart mallOrderCart);

    /**
     * 修改购物车
     *
     * @param mallOrderCart 购物车
     * @return 结果
     */
    public int updateMallOrderCart(MallOrderCart mallOrderCart);

    /**
     * 批量删除购物车
     *
     * @param ids 需要删除的购物车主键集合
     * @return 结果
     */
    public int deleteMallOrderCartByIds(String[] ids);

    /**
     * 删除购物车信息
     *
     * @param id 购物车主键
     * @return 结果
     */
    public int deleteMallOrderCartById(String id);
}
