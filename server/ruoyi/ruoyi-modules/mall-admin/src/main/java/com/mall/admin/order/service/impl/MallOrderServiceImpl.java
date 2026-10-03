package com.mall.admin.order.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.admin.order.domain.MallOrderItem;
import com.mall.admin.order.mapper.MallOrderMapper;
import com.mall.admin.order.domain.MallOrder;
import com.mall.admin.order.service.IMallOrderService;
import com.mall.api.feign.RemoteOrderService;

/**
 * 订单管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallOrderServiceImpl implements IMallOrderService
{
    @Autowired
    private MallOrderMapper mallOrderMapper;

    /** mall-order 内部契约：发货必须经其状态机，不能直改本模块的订单表 */
    @Autowired
    private RemoteOrderService remoteOrderService;

    /**
     * 查询订单管理
     *
     * @param id 订单管理主键
     * @return 订单管理
     */
    @Override
    public MallOrder selectMallOrderById(String id)
    {
        return mallOrderMapper.selectMallOrderById(id);
    }

    /**
     * 查询订单管理列表
     *
     * @param mallOrder 订单管理
     * @return 订单管理
     */
    @Override
    public List<MallOrder> selectMallOrderList(MallOrder mallOrder)
    {
        return mallOrderMapper.selectMallOrderList(mallOrder);
    }

    /**
     * 新增订单管理
     *
     * @param mallOrder 订单管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMallOrder(MallOrder mallOrder)
    {
        mallOrder.setCreateTime(DateUtils.getNowDate());
        int rows = mallOrderMapper.insertMallOrder(mallOrder);
        insertMallOrderItem(mallOrder);
        return rows;
    }

    /**
     * 修改订单管理
     *
     * @param mallOrder 订单管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallOrder(MallOrder mallOrder)
    {
        mallOrder.setUpdateTime(DateUtils.getNowDate());
        mallOrderMapper.deleteMallOrderItemByOrderId(mallOrder.getId());
        insertMallOrderItem(mallOrder);
        return mallOrderMapper.updateMallOrder(mallOrder);
    }

    /**
     * 批量删除订单管理
     *
     * @param ids 需要删除的订单管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallOrderByIds(String[] ids)
    {
        mallOrderMapper.deleteMallOrderItemByOrderIds(ids);
        return mallOrderMapper.deleteMallOrderByIds(ids);
    }

    /**
     * 删除订单管理信息
     *
     * @param id 订单管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallOrderById(String id)
    {
        mallOrderMapper.deleteMallOrderItemByOrderId(id);
        return mallOrderMapper.deleteMallOrderById(id);
    }

    /**
     * 新增订单项信息
     *
     * @param mallOrder 订单管理对象
     */
    public void insertMallOrderItem(MallOrder mallOrder)
    {
        List<MallOrderItem> mallOrderItemList = mallOrder.getMallOrderItemList();
        String id = mallOrder.getId();
        if (StringUtils.isNotNull(mallOrderItemList))
        {
            List<MallOrderItem> list = new ArrayList<MallOrderItem>();
            for (MallOrderItem mallOrderItem : mallOrderItemList)
            {
                mallOrderItem.setOrderId(id);
                list.add(mallOrderItem);
            }
            if (list.size() > 0)
            {
                mallOrderMapper.batchMallOrderItem(list);
            }
        }
    }

    /**
     * 发货：委托 mall-order 的状态机推进（PAID → WAIT_DELIVER）
     *
     * @param orderNo          订单号
     * @param logisticsCompany 物流公司
     * @param logisticsNo      物流单号
     */
    @Override
    public void deliverOrder(String orderNo, String logisticsCompany, String logisticsNo)
    {
        remoteOrderService.deliver(orderNo, logisticsCompany, logisticsNo);
    }

    /**
     * 物流揽收：委托 mall-order 的状态机推进（WAIT_DELIVER → WAIT_RECEIVE）
     *
     * @param orderNo 订单号
     */
    @Override
    public void logisticsPickOrder(String orderNo)
    {
        remoteOrderService.logisticsPick(orderNo);
    }

    /**
     * 强制取消：委托 mall-order 的状态机推进（PAID → CANCELLED）
     *
     * @param orderNo      订单号
     * @param cancelReason 客服填写的取消原因
     */
    @Override
    public void forceCancelOrder(String orderNo, String cancelReason)
    {
        remoteOrderService.forceCancel(orderNo, cancelReason);
    }
}
