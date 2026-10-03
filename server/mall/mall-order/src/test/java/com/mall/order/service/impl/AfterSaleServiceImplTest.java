package com.mall.order.service.impl;

import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.order.DO.MallAfterSaleDO;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.config.MallOrderConfigProperties;
import com.mall.order.infrastructure.feign.RemotePaymentAdapter;
import com.mall.order.infrastructure.feign.RemoteProductAdapter;
import com.mall.order.mapper.MallAfterSaleMapper;
import com.mall.order.mapper.MallOrderItemMapper;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.statemachine.OrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AfterSaleServiceImpl 退款回调单元测试
 *
 * <p>核心约束：售后单完成的同时，订单必须同步推进到 {@code REFUNDED}——
 * 否则订单永久停在 {@code REFUNDING}，{@code pre_refund_status} 的回退逻辑永远触发不了。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {

    private static final String AFTER_SALE_NO = "AS20261003000001";

    private static final String ORDER_NO = "ORD20261003000001";

    @Mock private MallAfterSaleMapper afterSaleMapper;

    @Mock private MallOrderItemMapper orderItemMapper;

    @Mock private MallOrderMapper orderMapper;

    /** 用真实状态机：REFUNDING → REFUNDED 的合法性正是本次要验证的契约 */
    @Spy private OrderStateMachine stateMachine = new OrderStateMachine();

    @Mock private RemotePaymentAdapter paymentAdapter;

    @Mock private RemoteProductAdapter productAdapter;

    @Mock private MallOrderConfigProperties config;

    @InjectMocks private AfterSaleServiceImpl afterSaleService;

    @Test
    @DisplayName("退款回调：售后单完成的同时把订单推进到 REFUNDED")
    void refundCallbackShouldAdvanceOrderToRefunded() {
        MallAfterSaleDO afterSale = new MallAfterSaleDO();
        afterSale.setId(1L);
        afterSale.setAfterSaleNo(AFTER_SALE_NO);
        afterSale.setOrderId(100L);
        // 非 COMPLETED：放行（否则会走"已完成直接跳过"分支）
        afterSale.setAfterSaleStatus(0);
        // 非退货退款：跳过 restock
        afterSale.setAfterSaleType(0);
        when(afterSaleMapper.selectByAfterSaleNo(AFTER_SALE_NO)).thenReturn(afterSale);

        MallOrderDO order = new MallOrderDO();
        order.setOrderNo(ORDER_NO);
        order.setOrderStatus(OrderStatusEnum.REFUNDING.getCode());
        order.setVersion(1);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(orderMapper.updateStatusCas(any(), any(), any(), any(), any())).thenReturn(1);

        afterSaleService.refundCallback(AFTER_SALE_NO, 1000L);

        verify(orderMapper).updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.REFUNDED.getCode()),
                eq(OrderStatusEnum.REFUNDING.getCode()), eq(1), any());
    }

    @Test
    @DisplayName("订单已非 REFUNDING：只记 warn，不让售后单完成失败")
    void refundCallbackShouldNotFailWhenOrderNotRefunding() {
        MallAfterSaleDO afterSale = new MallAfterSaleDO();
        afterSale.setId(1L);
        afterSale.setAfterSaleNo(AFTER_SALE_NO);
        afterSale.setOrderId(100L);
        afterSale.setAfterSaleStatus(0);
        afterSale.setAfterSaleType(0);
        when(afterSaleMapper.selectByAfterSaleNo(AFTER_SALE_NO)).thenReturn(afterSale);

        MallOrderDO order = new MallOrderDO();
        order.setOrderNo(ORDER_NO);
        // 订单已不在退款中（重复回调等）
        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setVersion(1);
        when(orderMapper.selectById(100L)).thenReturn(order);

        // 状态机拒绝该流转，但异常被吞掉：售后单已完成是既成事实，不能因此回滚
        afterSaleService.refundCallback(AFTER_SALE_NO, 1000L);

        verify(orderMapper, org.mockito.Mockito.never())
                .updateStatusCas(any(), any(), any(), any(), any());
    }
}
