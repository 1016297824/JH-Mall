package com.mall.payment.service.impl;

import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.channel.RefundResult;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.mapper.MallRefundMapper;
import com.mall.payment.statemachine.PaymentStateMachine;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RefundServiceImpl 单元测试
 *
 * <p>覆盖设计文档 §6 退款流程：幂等、金额校验、支付单状态推进、渠道即时结果处理。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundServiceImpl 退款服务")
class RefundServiceImplTest {

    private static final Long PAYMENT_ID = 1L;

    private static final Integer PAYMENT_VERSION = 0;

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String REFUND_NO = "REF20261003000001";

    private static final String CHANNEL_CODE = "wechat";

    private static final String AFTER_SALE_NO = "12345";

    private static final Long REFUND_AMOUNT = 10000L;

    private static final Long PAID_AMOUNT = 89900L;

    @Mock
    private MallRefundMapper refundMapper;

    @Mock
    private MallPaymentMapper paymentMapper;

    @Mock
    private MallPaymentChannelMapper channelMapper;

    @Mock
    private PaymentChannelFactory channelFactory;

    @Mock
    private PaymentChannelAdapter channelAdapter;

    private RefundServiceImpl refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundServiceImpl(refundMapper, paymentMapper, channelMapper,
                channelFactory, new PaymentStateMachine());
    }

    @Nested
    @DisplayName("创建退款单")
    class CreateRefund {

        @Test
        @DisplayName("正常流程：落 PROCESSING 退款单，支付单推进 REFUNDING")
        void createsRefundAndAdvancesPayment() {
            stubHappyPath();

            RefundResultDTO result = refundService.createRefund(refundDTO());

            assertThat(result.getRefundNo()).isNotBlank();
            assertThat(result.getRefundStatus()).isEqualTo(RefundStatusEnum.PROCESSING.getCode());

            ArgumentCaptor<MallRefundDO> captor = ArgumentCaptor.forClass(MallRefundDO.class);
            verify(refundMapper).insert(captor.capture());
            MallRefundDO inserted = captor.getValue();
            assertThat(inserted.getRefundStatus()).isEqualTo(RefundStatusEnum.PROCESSING.getCode());
            assertThat(inserted.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(inserted.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(inserted.getRefundAmount()).isEqualTo(REFUND_AMOUNT);
            assertThat(inserted.getChannelCode()).isEqualTo(CHANNEL_CODE);
            assertThat(inserted.getIdempotentKey()).isEqualTo(AFTER_SALE_NO + "_" + CHANNEL_CODE);

            // 支付单 CAS 推进到 REFUNDING
            verify(paymentMapper).markRefunding(PAYMENT_NO, PAYMENT_VERSION);
        }

        @Test
        @DisplayName("渠道受理后立即回填渠道退款单号（否则退款回调无法定位退款单）")
        void backfillsChannelRefundNo() {
            stubHappyPath();

            refundService.createRefund(refundDTO());

            verify(refundMapper).updateChannelRefundNo(anyString(),
                    eq("MOCKREF" + REFUND_NO), eq("PROCESSING"));
        }

        @Test
        @DisplayName("幂等键为 afterSaleNo_channelCode")
        void buildsIdempotentKey() {
            stubHappyPath();

            refundService.createRefund(refundDTO());

            verify(refundMapper).selectByIdempotentKey(AFTER_SALE_NO + "_" + CHANNEL_CODE);
        }

        @Test
        @DisplayName("幂等命中：复用已有退款单，不新建、不读渠道配置、不调渠道")
        void idempotentReusesExisting() {
            MallRefundDO existing = refund(RefundStatusEnum.PROCESSING);
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(existing);

            RefundResultDTO result = refundService.createRefund(refundDTO());

            assertThat(result.getRefundNo()).isEqualTo(REFUND_NO);
            verify(refundMapper, never()).insert(any(MallRefundDO.class));
            // 幂等短路应发生在任何渠道交互之前
            verify(channelMapper, never()).selectByChannelCode(anyString());
            verify(channelFactory, never()).getAdapter(anyString());
        }

        @Test
        @DisplayName("支付单不存在 → A0501")
        void paymentNotFound() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> refundService.createRefund(refundDTO()));
        }

        @Test
        @DisplayName("支付单未支付 → A0702（不可退款）")
        void paymentNotPaid() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            MallPaymentDO unpaid = payment(PaymentStatusEnum.UNPAID);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(unpaid);

            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> refundService.createRefund(refundDTO()));
            verify(refundMapper, never()).insert(any(MallRefundDO.class));
        }

        @Test
        @DisplayName("退款金额非正 → A0602")
        void refundAmountNotPositive() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            RefundDTO dto = refundDTO();
            dto.setRefundAmount(0L);

            assertErrorCode(ErrorCode.AMOUNT_EXCEED_LIMIT.getCode(),
                    () -> refundService.createRefund(dto));
        }

        @Test
        @DisplayName("累计退款超额 → A0602（已退 80000 + 本次 10000 > 支付 89900）")
        void refundExceedsPaidAmount() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(80000L);

            assertErrorCode(ErrorCode.AMOUNT_EXCEED_LIMIT.getCode(),
                    () -> refundService.createRefund(refundDTO()));
            verify(refundMapper, never()).insert(any(MallRefundDO.class));
        }

        @Test
        @DisplayName("累计退款恰好等于支付金额 → 允许（边界）")
        void refundExactlyEqualsPaidAmount() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(PAID_AMOUNT - REFUND_AMOUNT);
            when(refundMapper.insert(any(MallRefundDO.class))).thenReturn(1);
            when(paymentMapper.markRefunding(PAYMENT_NO, PAYMENT_VERSION)).thenReturn(1);
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokeRefund(any(), any(), any())).thenReturn(processingResult());

            RefundResultDTO result = refundService.createRefund(refundDTO());

            assertThat(result.getRefundNo()).isNotBlank();
        }

        @Test
        @DisplayName("渠道即时成功 → 退款单 SUCCESS，支付单 REFUNDED")
        void channelImmediateSuccess() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(0L);
            when(refundMapper.insert(any(MallRefundDO.class))).thenReturn(1);
            when(paymentMapper.markRefunding(PAYMENT_NO, PAYMENT_VERSION)).thenReturn(1);
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokeRefund(any(), any(), any())).thenReturn(successResult());
            when(refundMapper.markSuccess(anyString(), anyString(), anyString(), any())).thenReturn(1);
            when(paymentMapper.markRefunded(PAYMENT_NO, PAYMENT_VERSION)).thenReturn(1);

            RefundResultDTO result = refundService.createRefund(refundDTO());

            assertThat(result.getRefundStatus()).isEqualTo(RefundStatusEnum.SUCCESS.getCode());
            verify(refundMapper).markSuccess(anyString(), anyString(), anyString(), any());
            verify(paymentMapper).markRefunded(PAYMENT_NO, PAYMENT_VERSION);
        }

        @Test
        @DisplayName("渠道调用失败（未送达）→ 抛 C0211，且退款单不落库")
        void channelCallFailure() {
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(0L);
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokeRefund(any(), any(), any())).thenReturn(failedResult());

            assertErrorCode(ErrorCode.REFUND_SERVICE_ERROR.getCode(),
                    () -> refundService.createRefund(refundDTO()));
            verify(refundMapper, never()).insert(any(MallRefundDO.class));
        }
    }

    @Nested
    @DisplayName("按订单号退款")
    class RefundByOrderNo {

        @Test
        @DisplayName("由 orderNo 解析出已支付单并复用主流程")
        void resolvesPaymentByOrderNo() {
            when(paymentMapper.selectPaidByOrderNo(ORDER_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(0L);
            when(refundMapper.insert(any(MallRefundDO.class))).thenReturn(1);
            when(paymentMapper.markRefunding(PAYMENT_NO, PAYMENT_VERSION)).thenReturn(1);
            when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
            when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
            when(channelAdapter.invokeRefund(any(), any(), any())).thenReturn(processingResult());

            RefundResultDTO result = refundService.refundByOrderNo(ORDER_NO, REFUND_AMOUNT, AFTER_SALE_NO);

            assertThat(result.getRefundNo()).isNotBlank();
            // 幂等键直接用业务售后单号构成——回调带回的也是它，mall-order 才能按业务单号查回售后单
            verify(refundMapper).selectByIdempotentKey(AFTER_SALE_NO + "_" + CHANNEL_CODE);
        }

        @Test
        @DisplayName("订单下无已支付单 → A0501")
        void noPaidPaymentForOrder() {
            when(paymentMapper.selectPaidByOrderNo(ORDER_NO)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> refundService.refundByOrderNo(ORDER_NO, REFUND_AMOUNT, AFTER_SALE_NO));
        }
    }

    // ======================== 夹具与断言辅助 ========================

    /**
     * 打桩「幂等未命中 + 支付单已支付 + 渠道受理中」的正常路径
     */
    private void stubHappyPath() {
        when(refundMapper.selectByIdempotentKey(anyString())).thenReturn(null);
        when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(payment(PaymentStatusEnum.PAID));
        when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(0L);
        when(refundMapper.insert(any(MallRefundDO.class))).thenReturn(1);
        when(paymentMapper.markRefunding(PAYMENT_NO, PAYMENT_VERSION)).thenReturn(1);
        when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(channel());
        when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
        when(channelAdapter.invokeRefund(any(), any(), any())).thenReturn(processingResult());
    }

    /**
     * 构造已支付的支付单夹具
     *
     * @param status 支付单状态
     * @return 支付单
     */
    private static MallPaymentDO payment(PaymentStatusEnum status) {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setId(PAYMENT_ID);
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo(ORDER_NO);
        payment.setPayAmount(PAID_AMOUNT);
        payment.setChannelCode(CHANNEL_CODE);
        payment.setPaymentStatus(status.getCode());
        payment.setVersion(PAYMENT_VERSION);
        return payment;
    }

    /**
     * 构造退款单夹具
     *
     * @param status 退款单状态
     * @return 退款单
     */
    private static MallRefundDO refund(RefundStatusEnum status) {
        MallRefundDO refund = new MallRefundDO();
        refund.setRefundNo(REFUND_NO);
        refund.setPaymentId(PAYMENT_ID);
        refund.setOrderNo(ORDER_NO);
        refund.setAfterSaleNo(AFTER_SALE_NO);
        refund.setRefundAmount(REFUND_AMOUNT);
        refund.setChannelCode(CHANNEL_CODE);
        refund.setRefundStatus(status.getCode());
        refund.setVersion(0);
        return refund;
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
     * 构造退款请求
     *
     * @return 退款请求
     */
    private static RefundDTO refundDTO() {
        RefundDTO dto = new RefundDTO();
        dto.setPaymentNo(PAYMENT_NO);
        dto.setRefundAmount(REFUND_AMOUNT);
        dto.setAfterSaleNo(AFTER_SALE_NO);
        dto.setChannelCode(CHANNEL_CODE);
        return dto;
    }

    /**
     * 构造渠道「受理中」结果
     *
     * @return 退款结果
     */
    private static RefundResult processingResult() {
        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setChannelRefundNo("MOCKREF" + REFUND_NO);
        result.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
        result.setChannelRefundStatus("PROCESSING");
        return result;
    }

    /**
     * 构造渠道「即时成功」结果
     *
     * @return 退款结果
     */
    private static RefundResult successResult() {
        RefundResult result = processingResult();
        result.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
        result.setChannelRefundStatus("SUCCESS");
        return result;
    }

    /**
     * 构造渠道「未送达」结果
     *
     * @return 退款结果
     */
    private static RefundResult failedResult() {
        RefundResult result = new RefundResult();
        result.setSuccess(false);
        result.setFailReason("渠道连接超时");
        return result;
    }

    @Nested
    @DisplayName("查询支付单可退款状态")
    class GetPaymentStatus {

        @Test
        @DisplayName("支付单不存在 → 返回 null（调用方按 null 判断，不抛异常）")
        void notFoundReturnsNull() {
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO)).thenReturn(null);

            assertThat(refundService.getPaymentStatus(PAYMENT_NO)).isNull();
        }

        @Test
        @DisplayName("正常：返回状态快照，累计已退款取自 sumRefundedAmount")
        void returnsSnapshot() {
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO))
                    .thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(2000L);

            PaymentStatusDTO dto = refundService.getPaymentStatus(PAYMENT_NO);

            assertThat(dto.getPaymentNo()).isEqualTo(PAYMENT_NO);
            assertThat(dto.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(dto.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PAID.getCode());
            assertThat(dto.getPayAmount()).isEqualTo(PAID_AMOUNT);
            assertThat(dto.getRefundedAmount()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("从未退款过：sumRefundedAmount 返回 null 时归一为 0")
        void nullRefundedAmountNormalizedToZero() {
            when(paymentMapper.selectByPaymentNo(PAYMENT_NO))
                    .thenReturn(payment(PaymentStatusEnum.PAID));
            when(refundMapper.sumRefundedAmount(PAYMENT_ID)).thenReturn(null);

            assertThat(refundService.getPaymentStatus(PAYMENT_NO).getRefundedAmount()).isZero();
        }
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
