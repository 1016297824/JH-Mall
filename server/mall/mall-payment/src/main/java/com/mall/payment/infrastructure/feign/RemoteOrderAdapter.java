package com.mall.payment.infrastructure.feign;

import com.mall.api.feign.RemoteOrderService;
import com.mall.api.feign.RemoteOrderService.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-order Feign 适配器
 *
 * <p>支付发起前需向订单服务核实「订单是否处于可支付状态、应付金额是多少」。</p>
 *
 * <p><b>刻意不降级</b>：订单校验失败必须让支付流程感知 —— 若降级为「放行」，
 * 会出现已取消 / 已支付的订单仍能发起支付。订单不存在时返回 {@code null}，
 * 由调用方转成 {@code A0701}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteOrderAdapter {

    private final RemoteOrderService remoteOrderService;

    /**
     * 查询订单状态与金额快照
     *
     * @param orderNo 订单号
     * @return 订单快照，订单不存在返回 null
     */
    public OrderDTO queryOrder(String orderNo) {
        return remoteOrderService.queryOrder(orderNo);
    }
}
