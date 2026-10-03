package com.mall.order.service;

import com.mall.order.VO.OrderVO;
import com.mall.order.dto.request.CreateOrderRequest;

import java.util.List;

/**
 * 订单服务
 *
 * <p>对应设计文档 §5（下单）、§6（状态推进）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * <p>流程见设计文档 §5.1：幂等校验 → 参数校验 → 锁库存 → 锁优惠 → 创建订单+Outbox。
     * 远端资源（库存/优惠券）不回滚，失败时由本方法显式补偿（§5.7）。</p>
     *
     * <p><b>前置依赖</b>：本方法强依赖 mall-marketing 的优惠试算接口。
     * mall-marketing 未实现前调用会因 Feign 404 而失败（不会静默降级算错金额）。</p>
     *
     * @param userId        用户 ID
     * @param idempotentKey 客户端幂等键（请求头 Idempotent-Key），为空则拒绝下单
     * @param req           下单请求
     * @return 订单号
     */
    String createOrder(Long userId, String idempotentKey, CreateOrderRequest req);

    /**
     * 查询订单详情
     *
     * @param userId  用户 ID（用于越权校验）
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getDetail(Long userId, String orderNo);

    /**
     * 查询我的订单列表
     *
     * @param userId 用户 ID
     * @param status 订单状态过滤，null=全部
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 订单列表
     */
    List<OrderVO> listOrders(Long userId, Integer status, int page, int size);

    /**
     * 取消订单
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     */
    void cancelOrder(Long userId, String orderNo);

    /**
     * 确认收货
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     */
    void confirmReceipt(Long userId, String orderNo);

    /**
     * 支付成功回调（由 MQ 消费者调用，非用户触发）
     *
     * <p>幂等：重复投递时状态机报 A0702 或乐观锁拦截，不会重复推进。</p>
     *
     * @param orderNo 订单号
     */
    void payCallback(String orderNo);

    /**
     * 卖家发货（管理端填写物流信息并确认发货，推进 PAID → WAIT_DELIVER）
     *
     * <p>先落物流信息再走状态机：{@code SELLER_DELIVER} 的前置条件是
     * 「物流公司 + 单号已填写」（见 {@code OrderStateMachine#logisticsFilled}），
     * 顺序颠倒会因物流缺失抛 A0703。</p>
     *
     * @param orderNo          订单号
     * @param logisticsCompany 物流公司
     * @param logisticsNo      物流单号
     */
    void deliver(String orderNo, String logisticsCompany, String logisticsNo);

    /**
     * 物流揽收（推进 WAIT_DELIVER → WAIT_RECEIVE）
     *
     * <p>由快递公司揽收回调触发。该流转不产生下游领域事件，故不发 Outbox 消息。</p>
     *
     * @param orderNo 订单号
     */
    void logisticsPick(String orderNo);
}