package com.mall.payment.service.impl;

import com.mall.api.feign.RemoteOrderService.OrderDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.config.MallPaymentConfigProperties;
import com.mall.payment.dto.request.PayRequestDTO;
import com.mall.payment.dto.response.PayResultDTO;
import com.mall.payment.infrastructure.channel.PayResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.feign.RemoteOrderAdapter;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.vo.PaymentVO;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentServiceImpl 单元测试
 *
 * <p>覆盖设计文档 §4 发起支付流程：幂等、订单校验、渠道路由、回填渠道单号。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl 支付服务")
class PaymentServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String CHANNEL_CODE = "wechat";

    private static final String CHANNEL_PAYMENT_NO = "MOCKPAY" + PAYMENT_NO;

    private static final String CALLBACK_BASE_URL = "http://localhost:9305";

    @Mock
    private MallPaymentMapper paymentMapper;

    @Mock
    private MallPaymentChannelMapper channelMapper;

    @Mock
    private RemoteOrderAdapter remoteOrderAdapter;

    @Mock
    private PaymentChannelFactory channelFactory;

    @Mock
    private PaymentChannelAdapter channelAdapter;

    @Mock
    private MallPaymentConfigProperties configProperties;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentMapper, channelMapper, remoteOrderAdapter,
                channelFactory, configProperties);
    }

    @Nested
    @DisplayName("发起支付")
    class CreatePayment {

        @Test
        @DisplayName("正常流程：落 UNPAID 支付单，返回支付单号与前端调起参数")
        void createsUnpaidPayment() {
            stubHappyPath();

            PayResultDTO result = paymentService.createPayment(USER_ID, request());

            assertThat(result.getPaymentNo()).isNotBlank();
            assertThat(result.getPayParams()).containsKey("package");

            ArgumentCaptor<MallPaymentDO> captor = ArgumentCaptor.forClass(MallPaymentDO.class);
            verify(paymentMapper).insert(captor.capture());
            MallPaymentDO inserted = captor.getValue();
            assertThat(inserted.getPaymentStatus()).isEqualTo(PaymentStatusEnum.UNPAID.getCode());
            assertThat(inserted.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(inserted.getUserId()).isEqualTo(USER_ID);
            assertThat(inserted.getChannelCode()).isEqualTo(CHANNEL_CODE);
            // 金额取订单快照，不重新计算优惠
            assertThat(inserted.getPayAmount()).isEqualTo(89900L);
        }

        @Test
        @DisplayName("幂等键格式为 userId_orderNo_channelCode")
        void buildsIdempotentKey() {
            stubHappyPath();

            paymentService.createPayment(USER_ID, request());

            ArgumentCaptor<MallPaymentDO> captor = ArgumentCaptor.forClass(MallPaymentDO.class);
            verify(paymentMapper).insert(captor.capture());
            assertThat(captor.getValue().getIdempotentKey())
                    .isEqualTo(USER_ID + "_" + ORDER_NO + "_" + CHANNEL_CODE);
        }

        @Test
        @DisplayName("过期时间取订单的 payExpireTime")
        void copiesExpireTimeFromOrder() {
            // 期望值必须绑定到夹具订单本身：另起一次 now() 会因纳秒不同而必然不等
            OrderDTO order = waitPayOrder();
            stubHappyPath(order);
            LocalDateTime expected = LocalDateTime.parse(order.getPayExpireTime(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            paymentService.createPayment(USER_ID, request());

            ArgumentCaptor<MallPaymentDO> captor = ArgumentCaptor.forClass(MallPaymentDO.class);
            verify(paymentMapper).insert(captor.capture());
            assertThat(captor.getValue().getExpireTime()).isEqualTo(expected);
        }

        @Test
        @DisplayName("回填渠道单号与 notify_url（base + /callback/payment/{channel}）")
        void backfillsChannelNoAndNotifyUrl() {
            stubHappyPath();

            paymentService.createPayment(USER_ID, request());

            ArgumentCaptor<String> notifyCaptor = ArgumentCaptor.forClass(String.class);
            verify(paymentMapper).updateChannelPaymentNo(anyString(), anyString(), notifyCaptor.capture());
            assertThat(notifyCaptor.getValue())
                    .isEqualTo(CALLBACK_BASE_URL + "/callback/payment/" + CHANNEL_CODE);
        }

        @Test
        @DisplayName("幂等命中：复用已有支付单，不新建")
        void idempotentReusesExistingPayment() {
            MallPaymentDO existing = payment(PaymentStatusEnum.UNPAID);
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(existing);
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokePay(any(MallPaymentDO.class), any(MallPaymentChannelDO.class), any()))
                    .thenReturn(payResult());
            when(configProperties.getCallbackBaseUrl()).thenReturn(CALLBACK_BASE_URL);
            when(paymentMapper.updateChannelPaymentNo(any(), any(), any())).thenReturn(1);

            PayResultDTO result = paymentService.createPayment(USER_ID, request());

            assertThat(result.getPaymentNo()).isEqualTo(PAYMENT_NO);
            verify(paymentMapper, never()).insert(any(MallPaymentDO.class));
        }

        @Test
        @DisplayName("幂等命中时不再调订单服务（省一次远程调用）")
        void idempotentSkipsOrderQuery() {
            when(paymentMapper.selectByIdempotentKey(anyString()))
                    .thenReturn(payment(PaymentStatusEnum.UNPAID));
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokePay(any(MallPaymentDO.class), any(MallPaymentChannelDO.class), any()))
                    .thenReturn(payResult());
            when(configProperties.getCallbackBaseUrl()).thenReturn(CALLBACK_BASE_URL);
            when(paymentMapper.updateChannelPaymentNo(any(), any(), any())).thenReturn(1);

            paymentService.createPayment(USER_ID, request());

            verify(remoteOrderAdapter, never()).queryOrder(anyString());
        }

        @Test
        @DisplayName("订单不存在 → A0701")
        void orderNotFound() {
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(null);

            assertErrorCode(ErrorCode.ORDER_NOT_FOUND.getCode(), () -> paymentService.createPayment(USER_ID, request()));
        }

        @Test
        @DisplayName("订单归属他人 → A0501（与「不存在」同码，不泄露归属）")
        void orderOwnedByAnotherUser() {
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            OrderDTO order = waitPayOrder();
            order.setUserId(USER_ID + 100);
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(order);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> paymentService.createPayment(USER_ID, request()));
            verify(paymentMapper, never()).insert(any(MallPaymentDO.class));
        }

        @Test
        @DisplayName("订单非待支付 → A0702")
        void orderNotWaitPay() {
            OrderDTO order = waitPayOrder();
            order.setStatus(OrderStatusEnum.PAID.getCode());
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(order);

            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> paymentService.createPayment(USER_ID, request()));
        }

        @Test
        @DisplayName("订单已过期 → A0702")
        void orderExpired() {
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            OrderDTO order = waitPayOrder();
            order.setPayExpireTime(LocalDateTime.now().minusMinutes(1).toString());
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(order);

            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> paymentService.createPayment(USER_ID, request()));
        }

        @Test
        @DisplayName("订单应付金额非正 → A0602")
        void amountNotPositive() {
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            OrderDTO order = waitPayOrder();
            order.setPayAmount(0L);
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(order);

            assertErrorCode(ErrorCode.AMOUNT_EXCEED_LIMIT.getCode(),
                    () -> paymentService.createPayment(USER_ID, request()));
        }

        @Test
        @DisplayName("渠道发起失败 → 不落支付单，抛 C0210")
        void channelFailureAborts() {
            when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(waitPayOrder());
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokePay(any(MallPaymentDO.class), any(MallPaymentChannelDO.class), any()))
                    .thenReturn(failedPayResult());

            assertErrorCode(ErrorCode.PAYMENT_SERVICE_ERROR.getCode(),
                    () -> paymentService.createPayment(USER_ID, request()));
            verify(paymentMapper, never()).insert(any(MallPaymentDO.class));
        }
    }

    @Nested
    @DisplayName("查询支付单")
    class GetPayment {

        @Test
        @DisplayName("本人支付单：返回视图并填充状态文本")
        void returnsOwnPayment() {
            MallPaymentDO existing = payment(PaymentStatusEnum.PAID);
            existing.setId(1L);
            when(paymentMapper.selectById(1L)).thenReturn(existing);

            PaymentVO vo = paymentService.getPayment(USER_ID, 1L);

            assertThat(vo.getPaymentNo()).isEqualTo(PAYMENT_NO);
            assertThat(vo.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PAID.getCode());
            assertThat(vo.getPaymentStatusText()).isEqualTo(PaymentStatusEnum.PAID.getDescription());
        }

        @Test
        @DisplayName("他人支付单 → A0501（与「不存在」同码，不泄露归属）")
        void rejectsOthersPayment() {
            MallPaymentDO existing = payment(PaymentStatusEnum.UNPAID);
            existing.setId(1L);
            existing.setUserId(USER_ID + 1);
            when(paymentMapper.selectById(1L)).thenReturn(existing);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> paymentService.getPayment(USER_ID, 1L));
        }

        @Test
        @DisplayName("支付单不存在 → A0501")
        void notFound() {
            when(paymentMapper.selectById(1L)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> paymentService.getPayment(USER_ID, 1L));
        }
    }

    // ======================== 夹具与断言辅助 ========================

    /**
     * 打桩「幂等未命中 + 订单可支付 + 渠道正常」的完整正常路径
     */
    private void stubHappyPath() {
        stubHappyPath(waitPayOrder());
    }

    /**
     * 同上，但使用指定的订单快照
     *
     * <p>供需要精确引用订单字段（而非另起一次 {@code now()}）的用例使用。</p>
     *
     * @param order 订单快照
     */
    private void stubHappyPath(OrderDTO order) {
        when(paymentMapper.selectByIdempotentKey(anyString())).thenReturn(null);
        when(remoteOrderAdapter.queryOrder(ORDER_NO)).thenReturn(order);
        when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
        when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
        when(channelAdapter.invokePay(any(MallPaymentDO.class), any(MallPaymentChannelDO.class), any()))
                .thenReturn(payResult());
        when(paymentMapper.insert(any(MallPaymentDO.class))).thenReturn(1);
        when(configProperties.getCallbackBaseUrl()).thenReturn(CALLBACK_BASE_URL);
        when(paymentMapper.updateChannelPaymentNo(any(), any(), any())).thenReturn(1);
    }

    /**
     * 构造可支付订单快照（待支付、金额 89900 分、30 分钟后过期）
     *
     * @return 订单快照
     */
    private static OrderDTO waitPayOrder() {
        OrderDTO order = new OrderDTO();
        order.setOrderNo(ORDER_NO);
        order.setUserId(USER_ID);
        // 用枚举取值而非硬编码：OrderStatusEnum.WAIT_PAY = 0
        order.setStatus(OrderStatusEnum.WAIT_PAY.getCode());
        order.setPayAmount(89900L);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(30)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return order;
    }

    /**
     * 构造支付单夹具
     *
     * @param status 支付单状态
     * @return 支付单
     */
    private static MallPaymentDO payment(PaymentStatusEnum status) {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo(ORDER_NO);
        payment.setUserId(USER_ID);
        payment.setPayAmount(89900L);
        payment.setChannelCode(CHANNEL_CODE);
        payment.setPaymentStatus(status.getCode());
        payment.setVersion(0);
        return payment;
    }

    /**
     * 构造渠道配置夹具
     *
     * @return 渠道配置
     */
    private static MallPaymentChannelDO channel() {
        MallPaymentChannelDO channel = new MallPaymentChannelDO();
        channel.setChannelCode(CHANNEL_CODE);
        channel.setChannelName("微信支付");
        channel.setConfigJson("{}");
        return channel;
    }

    /**
     * 构造渠道发起成功结果
     *
     * @return 发起结果
     */
    private static PayResult payResult() {
        PayResult result = new PayResult();
        result.setSuccess(true);
        result.setChannelPaymentNo(CHANNEL_PAYMENT_NO);
        result.setPayParams(Map.of("package", "prepay_id=" + CHANNEL_PAYMENT_NO));
        result.setChannelPayStatus("NOTPAY");
        return result;
    }

    /**
     * 构造渠道发起失败结果
     *
     * @return 发起结果
     */
    private static PayResult failedPayResult() {
        PayResult result = new PayResult();
        result.setSuccess(false);
        result.setFailReason("渠道连接超时");
        return result;
    }

    /**
     * 构造发起支付请求
     *
     * @return 请求
     */
    private static PayRequestDTO request() {
        PayRequestDTO req = new PayRequestDTO();
        req.setOrderNo(ORDER_NO);
        req.setChannelCode(CHANNEL_CODE);
        req.setOpenid("openid-1");
        return req;
    }

    /**
     * 断言执行结果抛出的业务异常错误码
     *
     * @param expectedCode 期望错误码
     * @param callable     待执行逻辑
     */
    private static void assertErrorCode(String expectedCode, ThrowingCallable callable) {
        BusinessException ex = catchThrowableOfType(callable, BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
    }
}
