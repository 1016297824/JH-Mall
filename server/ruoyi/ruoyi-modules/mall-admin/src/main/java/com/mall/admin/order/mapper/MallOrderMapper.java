package com.mall.order.mapper;

import java.util.List;
import com.mall.order.domain.MallOrder;
import com.mall.order.domain.MallOrderItem;

/**
 * 订单管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallOrderMapper 
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
     * 删除订单管理
     * 
     * @param id 订单管理主键
     * @return 结果
     */
    public int deleteMallOrderById(String id);

    /**
     * 批量删除订单管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallOrderByIds(String[] ids);

    /**
     * 批量删除订单项
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallOrderItemByOrderIds(String[] ids);
    
    /**
     * 批量新增订单项
     * 
     * @param mallOrderItemList 订单项列表
     * @return 结果
     */
    public int batchMallOrderItem(List<MallOrderItem> mallOrderItemList);
    

    /**
     * 通过订单管理主键删除订单项信息
     * 
     * @param id 订单管理ID
     * @return 结果
     */
    public int deleteMallOrderItemByOrderId(String id);
}
