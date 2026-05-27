package com.mall.order.service;

import java.util.List;
import com.mall.order.DO.MallOrder;

/**
 * 订单管理Service接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallOrderService
{
    /**
     * 查询订单管理
     *
     * @param id 订单管理主键
     * @return 订单管理
     */
    public MallOrder selectMallOrderById(String id);

    /**
     * 查询订单管理列表
     *
     * @param mallOrder 订单管理
     * @return 订单管理集合
     */
    public List<MallOrder> selectMallOrderList(MallOrder mallOrder);

    /**
     * 新增订单管理
     *
     * @param mallOrder 订单管理
     * @return 结果
     */
    public int insertMallOrder(MallOrder mallOrder);

    /**
     * 修改订单管理
     *
     * @param mallOrder 订单管理
     * @return 结果
     */
    public int updateMallOrder(MallOrder mallOrder);

    /**
     * 批量删除订单管理
     *
     * @param ids 需要删除的订单管理主键集合
     * @return 结果
     */
    public int deleteMallOrderByIds(String[] ids);

    /**
     * 删除订单管理信息
     *
     * @param id 订单管理主键
     * @return 结果
     */
    public int deleteMallOrderById(String id);
}
