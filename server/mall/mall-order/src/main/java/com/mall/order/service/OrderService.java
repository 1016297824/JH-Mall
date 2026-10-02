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
}