package com.mall.admin.order.service;

import java.util.List;
import com.mall.admin.order.domain.MallOrder;

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

    /**
     * 发货：调 mall-order 状态机推进 PAID → WAIT_DELIVER
     *
     * <p><b>必须走 Feign 而非本模块 Mapper 直改状态</b>：状态机是订单状态变更的唯一入口，
     * 直连 DB 会绕过前置校验（物流已填、状态为 PAID），且不会投递
     * {@code mall:order:delivered} 事件。</p>
     *
     * @param orderNo          订单号
     * @param logisticsCompany 物流公司
     * @param logisticsNo      物流单号
     */
    public void deliverOrder(String orderNo, String logisticsCompany, String logisticsNo);

    /**
     * 物流揽收：调 mall-order 状态机推进 WAIT_DELIVER → WAIT_RECEIVE
     *
     * <p>真实场景由快递公司回调触发，当前未对接任何快递平台，故提供管理端手工入口；
     * 将来接平台时把回调适配到同一个内部端点即可。</p>
     *
     * @param orderNo 订单号
     */
    public void logisticsPickOrder(String orderNo);

    /**
     * 强制取消：调 mall-order 状态机推进 PAID → CANCELLED
     *
     * <p>仅适用于已支付、未发货且实付金额为 0 的异常订单；有金额的订单会被
     * mall-order 状态机拒绝（A0703），必须走退款流程。</p>
     *
     * @param orderNo      订单号
     * @param cancelReason 客服填写的取消原因
     */
    public void forceCancelOrder(String orderNo, String cancelReason);
}
