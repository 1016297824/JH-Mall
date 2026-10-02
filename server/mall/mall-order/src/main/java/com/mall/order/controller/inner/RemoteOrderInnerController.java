package com.mall.order.controller.inner;

import com.mall.api.feign.RemoteOrderService;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.mapper.MallOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单内部Feign 端点
 *
 * <p>路径 {@code /inner/order/**} 会被 {@code InnerSignatureFilter} 拦截并校验
 * {@code X-Internal-*} 签名头，仅供服务间调用，不可从网关外部直达。</p>
 *
 * <p>实现 {@link RemoteOrderService} 契约，供 mall-payment / mall-marketing 使用。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/inner/order")
@RequiredArgsConstructor
public class RemoteOrderInnerController {

    private final MallOrderMapper orderMapper;

    /**
     * 查询订单快照
     *
     * @param orderNo 订单号
     * @return 订单快照，不存在返回 null
     */
    @GetMapping("/query")
    public RemoteOrderService.OrderDTO queryOrder(@RequestParam("orderNo") String orderNo) {
        MallOrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        RemoteOrderService.OrderDTO dto = new RemoteOrderService.OrderDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getOrderStatus());
        dto.setPayAmount(order.getPayAmount());
        dto.setPayExpireTime(order.getPayExpireTime() == null
                ? null : order.getPayExpireTime().toString());
        dto.setCancelType(order.getCancelType());
        return dto;
    }
}