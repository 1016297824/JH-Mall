package com.mall.order.controller.inner;

import com.mall.api.feign.RemoteOrderService;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.service.AfterSaleService;
import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单内部Feign 端点
 *
 * <p>⚠️ {@code InnerSignatureFilter} 当前<b>未装配</b>（它位于 {@code com.mall.api} 包下，
 * 而各模块 {@code scanBasePackages} 不含该包，mall-api 也无 {@code AutoConfiguration.imports}），
 * 因此本端点的安全性实际只依赖网关 {@code InternalApiBlockFilter} 拦截 + 服务端口内网隔离。
 * 装配工作见 {@code docs/开发/07_项目完成度盘点.md} P3-1。</p>
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

    private final OrderService orderService;

    private final AfterSaleService afterSaleService;

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

    /**
     * 卖家发货（管理端填写物流信息并确认发货）
     *
     * @param orderNo          订单号
     * @param logisticsCompany 物流公司
     * @param logisticsNo      物流单号
     */
    @PostMapping("/deliver")
    public void deliver(@RequestParam("orderNo") String orderNo,
                        @RequestParam("logisticsCompany") String logisticsCompany,
                        @RequestParam("logisticsNo") String logisticsNo) {
        orderService.deliver(orderNo, logisticsCompany, logisticsNo);
    }

    /**
     * 物流揽收
     *
     * @param orderNo 订单号
     */
    @PostMapping("/logistics-pick")
    public void logisticsPick(@RequestParam("orderNo") String orderNo) {
        orderService.logisticsPick(orderNo);
    }

    /**
     * 强制取消订单（管理端客服操作）
     *
     * @param orderNo      订单号
     * @param cancelReason 客服填写的取消原因，可省略
     */
    @PostMapping("/force-cancel")
    public void forceCancel(@RequestParam("orderNo") String orderNo,
                            @RequestParam(value = "cancelReason", required = false) String cancelReason) {
        orderService.forceCancel(orderNo, cancelReason);
    }

    /**
     * 售后审核通过（管理端客服操作，触发退款）
     *
     * @param afterSaleId 售后单 ID
     * @param remark      审核意见
     */
    @PostMapping("/after-sale/approve")
    public void approveAfterSale(@RequestParam("afterSaleId") Long afterSaleId,
                                 @RequestParam(value = "remark", required = false) String remark) {
        afterSaleService.approve(afterSaleId, remark);
    }

    /**
     * 售后审核驳回
     *
     * @param afterSaleId 售后单 ID
     * @param remark      驳回原因
     */
    @PostMapping("/after-sale/reject")
    public void rejectAfterSale(@RequestParam("afterSaleId") Long afterSaleId,
                                @RequestParam(value = "remark", required = false) String remark) {
        afterSaleService.reject(afterSaleId, remark);
    }
}