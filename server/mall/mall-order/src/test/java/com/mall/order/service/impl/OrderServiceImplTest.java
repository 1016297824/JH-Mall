package com.mall.order.service.impl;

import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.CancelTypeEnum;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallCartDO;
import com.mall.order.DO.MallOrderAmountDO;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.DO.MallOrderItemDO;
import com.mall.order.config.MallOrderConfigProperties;
import com.mall.order.dto.request.CreateOrderRequest;
import com.mall.order.infrastructure.feign.RemoteMarketingAdapter;
import com.mall.order.infrastructure.feign.RemoteProductAdapter;
import com.mall.order.infrastructure.feign.RemoteUserAdapter;
import com.mall.order.infrastructure.outbox.OutboxPublisher;
import com.mall.order.mapper.MallCartMapper;
import com.mall.order.mapper.MallOrderAmountMapper;
import com.mall.order.mapper.MallOrderItemMapper;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderServiceImpl 发货 / 揽收链路单元测试
 *
 * <p>覆盖设计文档 §6.3 的两条死事件：
 * {@code PAID --SELLER_DELIVER--> WAIT_DELIVER --LOGISTICS_PICK--> WAIT_RECEIVE}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String COMPANY = "顺丰速运";

    private static final String TRACKING_NO = "SF123456789";

    @Mock private MallCartMapper cartMapper;

    @Mock private MallOrderMapper orderMapper;

    @Mock private MallOrderItemMapper orderItemMapper;

    @Mock private MallOrderAmountMapper orderAmountMapper;

    /** 用真实状态机：发货的前置校验（物流是否已填）正是本次要验证的契约 */
    @Spy private OrderStateMachine stateMachine = new OrderStateMachine();

    @Mock private RemoteProductAdapter productAdapter;

    @Mock private RemoteUserAdapter userAdapter;

    @Mock private RemoteMarketingAdapter marketingAdapter;

    @Mock private OutboxPublisher outboxPublisher;

    @Mock private MallOrderConfigProperties config;

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // mock 的 TransactionTemplate 默认不执行回调，会让 CAS 与 Outbox 写入全部落空。
        // lenient：抛异常的用例不会走到事务，无需该 stub。
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    /** 已支付、未发货的订单 */
    private MallOrderDO paidOrder() {
        MallOrderDO order = new MallOrderDO();
        order.setOrderNo(ORDER_NO);
        order.setUserId(12345L);
        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setVersion(1);
        order.setIsDeleted(0);
        return order;
    }

    /** 待支付且未过期的订单 */
    private MallOrderDO waitPayOrder() {
        MallOrderDO order = new MallOrderDO();
        order.setOrderNo(ORDER_NO);
        order.setUserId(12345L);
        order.setOrderStatus(OrderStatusEnum.WAIT_PAY.getCode());
        order.setVersion(1);
        order.setIsDeleted(0);
        order.setPayAmount(10000L);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(30));
        return order;
    }

    @Test
    @DisplayName("支付回调：必须在同一事务补写 pay_time（对账扫描与订单详情都依赖该列）")
    void payCallbackShouldMarkPayTime() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(waitPayOrder());
        when(orderMapper.updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.PAID.getCode()),
                eq(OrderStatusEnum.WAIT_PAY.getCode()), eq(1), isNull())).thenReturn(1);

        orderService.payCallback(ORDER_NO);

        // updateStatusCas 只推进状态，时间线字段必须单独补齐，否则 pay_time 永远是 NULL
        verify(orderMapper).markPayTime(ORDER_NO);
    }

    @Test
    @DisplayName("用户取消：必须补写 cancel_time 与 cancel_type")
    void cancelOrderShouldMarkCancelTime() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(waitPayOrder());
        when(orderMapper.updateStatusCas(any(), any(), any(), any(), any())).thenReturn(1);

        orderService.cancelOrder(12345L, ORDER_NO);

        // 取消类型取 CancelTypeEnum 的码值，避免与超时关单各写一套词汇
        verify(orderMapper).markCancelTime(ORDER_NO, CancelTypeEnum.USER_CANCEL.getCode());
    }

    @Test
    @DisplayName("确认收货：必须补写 complete_time")
    void confirmReceiptShouldMarkCompleteTime() {
        MallOrderDO order = paidOrder();
        order.setOrderStatus(OrderStatusEnum.WAIT_RECEIVE.getCode());
        order.setPayAmount(10000L);
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.updateStatusCas(any(), any(), any(), any(), any())).thenReturn(1);

        orderService.confirmReceipt(12345L, ORDER_NO);

        verify(orderMapper).markCompleteTime(ORDER_NO);
    }

    @Test
    @DisplayName("发货：先落物流信息再推进状态，并投递 order:delivered")
    void deliverShouldPersistLogisticsBeforeTransition() {
        MallOrderDO order = paidOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.updateLogistics(ORDER_NO, COMPANY, TRACKING_NO)).thenReturn(1);
        when(orderMapper.updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.WAIT_DELIVER.getCode()),
                eq(OrderStatusEnum.PAID.getCode()), eq(1), isNull())).thenReturn(1);

        orderService.deliver(ORDER_NO, COMPANY, TRACKING_NO);

        // 顺序不可颠倒：状态机的前置条件是物流已填写
        InOrder ordered = inOrder(orderMapper);
        ordered.verify(orderMapper).updateLogistics(ORDER_NO, COMPANY, TRACKING_NO);
        ordered.verify(orderMapper).updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.WAIT_DELIVER.getCode()),
                eq(OrderStatusEnum.PAID.getCode()), eq(1), isNull());
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxPublisher).publish(eq(MqTopicConstants.Order.DELIVERED), eq("OrderDelivered"),
                eq(ORDER_NO), payloadCaptor.capture());
        // payload 必须带齐物流信息，否则下游"通知用户"拿不到单号
        assertThat(payloadCaptor.getValue())
                .containsEntry("orderNo", ORDER_NO)
                .containsEntry("logisticsCompany", COMPANY)
                .containsEntry("logisticsNo", TRACKING_NO);
    }

    @Test
    @DisplayName("物流信息为空：状态机拒绝发货（不能发出没有单号的货）")
    void deliverShouldThrowWhenLogisticsMissing() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(paidOrder());

        assertThatThrownBy(() -> orderService.deliver(ORDER_NO, "  ", ""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACTION_DENIED.getCode());

        verify(orderMapper, never()).updateLogistics(any(), any(), any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    @DisplayName("订单不存在：抛业务异常")
    void deliverShouldThrowWhenOrderMissing() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(null);

        assertThatThrownBy(() -> orderService.deliver(ORDER_NO, COMPANY, TRACKING_NO))
                .isInstanceOf(BusinessException.class);

        verify(orderMapper, never()).updateLogistics(any(), any(), any());
    }

    @Test
    @DisplayName("揽收：推进到待收货，且不投递领域事件")
    void logisticsPickShouldTransitionWithoutEvent() {
        MallOrderDO order = paidOrder();
        order.setOrderStatus(OrderStatusEnum.WAIT_DELIVER.getCode());
        order.setLogisticsCompany(COMPANY);
        order.setLogisticsNo(TRACKING_NO);
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.WAIT_RECEIVE.getCode()),
                eq(OrderStatusEnum.WAIT_DELIVER.getCode()), eq(1), isNull())).thenReturn(1);

        orderService.logisticsPick(ORDER_NO);

        verify(orderMapper).updateStatusCas(eq(ORDER_NO), eq(OrderStatusEnum.WAIT_RECEIVE.getCode()),
                eq(OrderStatusEnum.WAIT_DELIVER.getCode()), eq(1), isNull());
        // 揽收无下游消费者，不应产生领域事件（否则 Outbox 会堆无人消费的消息）
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    @DisplayName("未发货就揽收：状态机拒绝（PAID 不能直接跳到待收货）")
    void logisticsPickShouldThrowWhenNotDelivered() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(paidOrder());

        assertThatThrownBy(() -> orderService.logisticsPick(ORDER_NO))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());

        verify(orderMapper, never()).updateStatusCas(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("确认收货：完成事件必须带 orderAmount 与 points，否则 mall-user 静默不发积分")
    void confirmReceiptShouldIncludeRewardFields() {
        MallOrderDO order = paidOrder();
        order.setOrderStatus(OrderStatusEnum.WAIT_RECEIVE.getCode());
        order.setPayAmount(10000L);
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.updateStatusCas(any(), any(), any(), any(), any())).thenReturn(1);

        orderService.confirmReceipt(12345L, ORDER_NO);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(outboxPublisher).publish(eq(MqTopicConstants.Order.COMPLETED), eq("OrderCompleted"),
                eq(ORDER_NO), captor.capture());
        // 1 元 = 1 积分/成长值：10000 分 = 100 元 → 100
        assertThat(captor.getValue())
                .containsEntry("orderAmount", 10000L)
                .containsEntry("points", 100L);
    }

    @Test
    @DisplayName("下单：订单项与金额快照的 NOT NULL 字段必须写全（缺任一项都会让下单失败）")
    void createOrderShouldPersistCompleteSnapshots() {
        Long userId = 12345L;
        MallCartDO cart = new MallCartDO();
        cart.setSkuId(101L);
        cart.setSpuId(1L);
        cart.setSkuCode("IP15PM-256-BLUE");
        cart.setSkuName("256GB 蓝色");
        cart.setMainImage("http://img/1.png");
        cart.setPrice(899900L);
        cart.setQuantity(2);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(cartMapper.selectSelectedByUserId(userId)).thenReturn(List.of(cart));
        when(userAdapter.validateAddress(userId, 1L)).thenReturn(true);

        ProductSkuDTO sku = new ProductSkuDTO();
        sku.setSkuId(101L);
        sku.setSpuId(1L);
        sku.setSkuName("256GB 蓝色");
        sku.setSpuName("iPhone 15 Pro Max");
        sku.setPrice(899900L);
        sku.setIsOnSale(true);
        sku.setAvailableQty(500);
        when(productAdapter.batchGetSkuSafely(List.of(101L))).thenReturn(Map.of(101L, sku));

        CalculationResp calculation = new CalculationResp();
        calculation.setOriginalAmount(1799800L);
        calculation.setCouponDiscount(0L);
        calculation.setPromotionDiscount(0L);
        calculation.setFinalAmount(1799800L);
        when(marketingAdapter.calculate(eq(userId), anyList(), isNull())).thenReturn(calculation);
        when(productAdapter.reserveStock(anyString(), anyList())).thenReturn(true);
        when(config.getPayExpireMinutes()).thenReturn(30);
        // insert 后 MyBatis 会回填自增主键，订单项依赖它，测试里手动补上
        doAnswer(invocation -> {
            ((MallOrderDO) invocation.getArgument(0)).setId(9001L);
            return 1;
        }).when(orderMapper).insert(any(MallOrderDO.class));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);
        String orderNo = orderService.createOrder(userId, "key-1", req);

        assertThat(orderNo).isNotBlank();
        ArgumentCaptor<List<MallOrderItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderItemMapper).batchInsert(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getSpuName()).isEqualTo("iPhone 15 Pro Max");

        ArgumentCaptor<MallOrderAmountDO> amountCaptor = ArgumentCaptor.forClass(MallOrderAmountDO.class);
        verify(orderAmountMapper).insert(amountCaptor.capture());
        // items_json 是 NOT NULL 列，设计文档约定其内容为售后退款计算所需的明细
        assertThat(amountCaptor.getValue().getItemsJson())
                .isNotBlank()
                .contains("\"skuCode\":\"IP15PM-256-BLUE\"")
                .contains("\"quantity\":2")
                .contains("\"totalPrice\":1799800");
    }
}
