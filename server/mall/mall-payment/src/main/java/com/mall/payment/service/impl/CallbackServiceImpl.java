package com.mall.payment.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.payment.DO.MallPaymentCallbackLogDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import com.mall.payment.config.MallPaymentConfigProperties;
import com.mall.payment.dto.event.PaymentPaidEvent;
import com.mall.payment.dto.event.RefundFailedEvent;
import com.mall.payment.dto.event.RefundSucceededEvent;
import com.mall.payment.dto.response.CallbackResult;
import com.mall.payment.infrastructure.channel.PayCallbackResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.channel.RefundCallbackResult;
import com.mall.payment.infrastructure.outbox.OutboxPublisher;
import com.mall.payment.mapper.MallPaymentCallbackLogMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.mapper.MallRefundMapper;
import com.mall.payment.service.CallbackService;
import com.mall.payment.statemachine.PaymentEventEnum;
import com.mall.payment.statemachine.PaymentStateMachine;
import com.mall.payment.statemachine.RefundEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付 / 退款回调服务实现
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §5 回调流程与 §10 回调安全：
 * 验签分支、nonce 防重放、<b>先落库再应答</b>、金额比对、CAS 幂等。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackServiceImpl implements CallbackService {

    /** 回调类型：支付回调 */
    private static final String CALLBACK_TYPE_PAY = "PAY";

    /** 回调类型：退款回调 */
    private static final String CALLBACK_TYPE_REFUND = "REFUND";

    /** 验签结果：验签通过 */
    private static final int VERIFIED_PASS = 1;

    /** 验签结果：验签失败 */
    private static final int VERIFIED_FAIL = 2;

    /** Redis 防重放占位值 */
    private static final String NONCE_VALUE = "1";

    /** 支付平台成功应答体（模拟渠道 / 支付宝纯文本均为 success） */
    private static final String SUCCESS_BODY = "success";

    /** 事件类型：支付成功 */
    private static final String EVENT_PAYMENT_PAID = "PaymentPaid";

    /** 事件类型：退款成功 */
    private static final String EVENT_REFUND_SUCCEEDED = "RefundSucceeded";

    /** 事件类型：退款失败 */
    private static final String EVENT_REFUND_FAILED = "RefundFailed";

    private final MallPaymentCallbackLogMapper callbackLogMapper;

    private final MallPaymentMapper paymentMapper;

    private final MallRefundMapper refundMapper;

    private final PaymentChannelFactory channelFactory;

    private final PaymentStateMachine paymentStateMachine;

    private final StringRedisTemplate stringRedisTemplate;

    private final MallPaymentConfigProperties configProperties;

    private final OutboxPublisher outboxPublisher;

    /**
     * 处理支付回调
     *
     * <p>顺序固定为「验签 → 先落回调日志 → nonce 防重放 → 定位支付单并比对金额 →
     * 状态机推进 + CAS 落库 → 回填处理结果 → 应答」，其中落库严格先于应答（设计 §5.3）。</p>
     *
     * @param channelCode 渠道编码
     * @param rawBody     回调原始报文
     * @param headers     回调请求头
     * @return 处理结果（含给平台的应答体）
     */
    @Override
    public CallbackResult processPayCallback(String channelCode, String rawBody, Map<String, String> headers) {
        PaymentChannelAdapter adapter = channelFactory.getAdapter(channelCode);
        PayCallbackResult parsed = adapter.parsePayCallback(rawBody, headers);

        // ① 先落回调日志（原始报文留痕），无论验签成败都记
        MallPaymentCallbackLogDO logDO = buildCallbackLog(channelCode, CALLBACK_TYPE_PAY, rawBody,
                parsed.getNonce(), parsed.isVerified());

        // ② 验签失败：不推进状态、不占用 nonce，直接以失败应答交由 Controller 返回 400
        if (!parsed.isVerified()) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED,
                    "验签失败：" + parsed.getFailReason());
            log.warn("支付回调验签失败: channelCode={}, paymentNo={}, reason={}",
                    channelCode, parsed.getChannelPaymentNo(), parsed.getFailReason());
            return new CallbackResult(false, null, parsed.getFailReason());
        }

        // ③ nonce 防重放：命中说明是渠道重复回调，直接返回成功应答且不重复推进
        if (!acquireNonce(CacheConstants.Payment.CALLBACK + channelCode + ":" + parsed.getNonce())) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "重复回调，幂等忽略");
            log.info("支付回调重复，幂等忽略: channelCode={}, nonce={}", channelCode, parsed.getNonce());
            return successResult();
        }

        // ④ 用渠道支付单号定位本地支付单
        MallPaymentDO payment = paymentMapper.selectByChannelPaymentNo(parsed.getChannelPaymentNo());
        if (payment == null) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED, "支付单不存在");
            log.warn("支付回调处理失败，支付单不存在: channelPaymentNo={}", parsed.getChannelPaymentNo());
            return new CallbackResult(false, null, "支付单不存在");
        }

        // ⑤ 金额比对：与本地支付单金额不一致即拒绝（防回调篡改）
        if (parsed.getPayAmount() != null && !parsed.getPayAmount().equals(payment.getPayAmount())) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED, "回调金额与本地支付单不一致");
            log.error("支付回调金额不一致，疑似篡改: paymentNo={}, callbackAmount={}, localAmount={}",
                    payment.getPaymentNo(), parsed.getPayAmount(), payment.getPayAmount());
            return new CallbackResult(false, null, "回调金额与本地支付单不一致");
        }

        // ⑥ 金额、状态均校验通过后经状态机推进，再由 Mapper CAS 落库（先落库再应答）
        paymentStateMachine.transition(payment, PaymentEventEnum.PAY_SUCCESS_CALLBACK);
        int rows = paymentMapper.markPaid(payment.getPaymentNo(), parsed.getChannelPayStatus(),
                payment.getVersion());

        // ⑦ CAS 影响 0 行 = 已被并发回调/定时任务处理，幂等返回成功
        if (rows == 0) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "并发已处理，幂等返回");
            log.info("支付回调 CAS 未命中，幂等返回: paymentNo={}", payment.getPaymentNo());
            return successResult();
        }

        // ⑧ 与状态推进同一本地事务补发支付成功事件（设计 §5.4）
        publishPaymentPaid(payment);

        markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "支付回调处理成功");
        return successResult();
    }

    /**
     * 处理退款回调
     *
     * <p>结构与支付回调一致：先落库再应答、nonce 防重放、CAS 幂等。
     * 退款终态同时推进退款单与支付单：成功则退款单 {@code SUCCESS} + 支付单 {@code REFUNDED}；
     * 失败则退款单 {@code FAILED} + 支付单回退 {@code PAID}。</p>
     *
     * @param channelCode 渠道编码
     * @param rawBody     回调原始报文
     * @param headers     回调请求头
     * @return 处理结果（含给平台的应答体）
     */
    @Override
    public CallbackResult processRefundCallback(String channelCode, String rawBody, Map<String, String> headers) {
        PaymentChannelAdapter adapter = channelFactory.getAdapter(channelCode);
        RefundCallbackResult parsed = adapter.parseRefundCallback(rawBody, headers);

        MallPaymentCallbackLogDO logDO = buildCallbackLog(channelCode, CALLBACK_TYPE_REFUND, rawBody,
                parsed.getNonce(), parsed.isVerified());

        if (!parsed.isVerified()) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED,
                    "验签失败：" + parsed.getFailReason());
            log.warn("退款回调验签失败: channelCode={}, channelRefundNo={}, reason={}",
                    channelCode, parsed.getChannelRefundNo(), parsed.getFailReason());
            return new CallbackResult(false, null, parsed.getFailReason());
        }

        if (!acquireNonce(CacheConstants.Payment.REFUND_CALLBACK + channelCode + ":" + parsed.getNonce())) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "重复回调，幂等忽略");
            log.info("退款回调重复，幂等忽略: channelCode={}, nonce={}", channelCode, parsed.getNonce());
            return successResult();
        }

        // 用渠道退款单号定位本地退款单
        MallRefundDO refund = refundMapper.selectByChannelRefundNo(parsed.getChannelRefundNo());
        if (refund == null) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED, "退款单不存在");
            log.warn("退款回调处理失败，退款单不存在: channelRefundNo={}", parsed.getChannelRefundNo());
            return new CallbackResult(false, null, "退款单不存在");
        }

        Integer refundStatus = parsed.getRefundStatus();
        if (refundStatus == null || refundStatus == RefundStatusEnum.PROCESSING.getCode()) {
            // 非终态：仅留痕，等待后续回调推进
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "退款处理中，等待终态回调");
            return successResult();
        }

        // 终态需同时推进退款单与支付单，故反查关联支付单
        MallPaymentDO payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment == null) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_FAILED, "退款单关联的支付单不存在");
            log.warn("退款回调处理失败，关联支付单不存在: refundNo={}, paymentId={}",
                    refund.getRefundNo(), refund.getPaymentId());
            return new CallbackResult(false, null, "退款单关联的支付单不存在");
        }

        if (refundStatus == RefundStatusEnum.SUCCESS.getCode()) {
            paymentStateMachine.refundTransition(refund, RefundEventEnum.REFUND_SUCCESS_CALLBACK);
            int refundRows = refundMapper.markSuccess(refund.getRefundNo(), parsed.getChannelRefundNo(),
                    parsed.getChannelRefundStatus(), refund.getVersion());
            if (refundRows == 0) {
                markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "并发已处理，幂等返回");
                return successResult();
            }
            paymentStateMachine.transition(payment, PaymentEventEnum.REFUND_SUCCESS_CALLBACK);
            paymentMapper.markRefunded(payment.getPaymentNo(), payment.getVersion());
            publishRefundSucceeded(refund, payment);
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "退款成功回调处理完成");
            return successResult();
        }

        // 退款失败终态：退款单 FAILED，支付单回退 PAID，避免永久卡在退款中
        paymentStateMachine.refundTransition(refund, RefundEventEnum.REFUND_FAIL_CALLBACK);
        int refundRows = refundMapper.markFailed(refund.getRefundNo(), parsed.getChannelRefundStatus(),
                refund.getVersion());
        if (refundRows == 0) {
            markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "并发已处理，幂等返回");
            return successResult();
        }
        paymentStateMachine.transition(payment, PaymentEventEnum.REFUND_FAIL_CALLBACK);
        paymentMapper.revertToPaid(payment.getPaymentNo(), payment.getVersion());
        publishRefundFailed(refund, payment);
        markProcessed(logDO, MallPaymentCallbackLogMapper.PROCESS_SUCCESS, "退款失败回调处理完成");
        return successResult();
    }

    /**
     * 构建回调日志（回调日志只增不改，此处仅 insert）
     *
     * @param channelCode 渠道编码
     * @param callbackType 回调类型
     * @param rawBody     回调原始报文
     * @param nonce       防重放 nonce
     * @param verified    验签是否通过
     * @return 待落库的回调日志
     */
    private MallPaymentCallbackLogDO buildCallbackLog(String channelCode, String callbackType, String rawBody,
                                                      String nonce, boolean verified) {
        MallPaymentCallbackLogDO logDO = new MallPaymentCallbackLogDO();
        logDO.setChannelCode(channelCode);
        logDO.setCallbackType(callbackType);
        logDO.setRawBody(rawBody);
        logDO.setNonce(nonce);
        logDO.setIsVerified(verified ? VERIFIED_PASS : VERIFIED_FAIL);
        logDO.setProcessStatus(MallPaymentCallbackLogMapper.PROCESS_PENDING);
        logDO.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        logDO.setCreateTime(now);
        logDO.setUpdateTime(now);
        callbackLogMapper.insert(logDO);
        return logDO;
    }

    /**
     * Redis 占位实现 nonce 防重放
     *
     * <p>{@code SETNX} 占位成功说明该回调首次到达；返回 {@code false} 表示已被处理过，
     * 调用方应直接返回成功应答且不重复推进状态。TTL 取配置 {@code mall.payment.callback.nonce-ttl}。</p>
     *
     * @param nonceKey 防重放 Key
     * @return 占位成功（首次回调）返回 true
     */
    private boolean acquireNonce(String nonceKey) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(nonceKey, NONCE_VALUE,
                configProperties.getCallback().getNonceTtl(), TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 回填回调日志处理结果
     *
     * <p>各分支均建议调用，便于事后按处理状态排查；日志主键为 null 时（极端场景）跳过。</p>
     *
     * @param logDO         回调日志
     * @param processStatus 处理状态，取值见 {@code MallPaymentCallbackLogMapper.PROCESS_*}
     * @param processResult 处理结果说明
     */
    private void markProcessed(MallPaymentCallbackLogDO logDO, int processStatus, String processResult) {
        if (logDO.getId() == null) {
            return;
        }
        callbackLogMapper.markProcessed(logDO.getId(), processStatus, processResult);
    }

    /**
     * 构造成功应答
     *
     * @return 成功应答体
     */
    /**
     * 补发「支付成功」事件（设计 §5.4）
     *
     * <p>必须在 CAS 命中后调用：CAS 影响 0 行说明状态已被并发方推进，
     * 此时再发事件会让 mall-order 重复推进订单（虽有状态机兜底，但属无谓的重复消费）。</p>
     *
     * @param payment 支付单（已推进 PAID）
     */
    private void publishPaymentPaid(MallPaymentDO payment) {
        PaymentPaidEvent event = new PaymentPaidEvent(payment.getPaymentNo(), payment.getOrderNo(),
                payment.getUserId(), payment.getPayAmount(), payment.getPaySuccessTime(),
                payment.getChannelPaymentNo(), payment.getChannelCode());
        outboxPublisher.publish(MqTopicConstants.Payment.PAID, EVENT_PAYMENT_PAID,
                OutboxPublisher.AGGREGATE_PAYMENT, payment.getPaymentNo(), event);
    }

    /**
     * 补发「退款成功」事件（设计 §8.1）
     *
     * @param refund  退款单（已推进 SUCCESS）
     * @param payment 关联支付单（已推进 REFUNDED），userId 取自支付单
     */
    private void publishRefundSucceeded(MallRefundDO refund, MallPaymentDO payment) {
        RefundSucceededEvent event = new RefundSucceededEvent(refund.getRefundNo(),
                payment.getPaymentNo(), refund.getOrderNo(), refund.getAfterSaleNo(),
                payment.getUserId(), refund.getRefundAmount(), LocalDateTime.now(),
                refund.getChannelRefundNo());
        outboxPublisher.publish(MqTopicConstants.Payment.REFUND_SUCCEEDED, EVENT_REFUND_SUCCEEDED,
                OutboxPublisher.AGGREGATE_REFUND, refund.getRefundNo(), event);
    }

    /**
     * 补发「退款失败」事件（设计 §8.1）
     *
     * <p>mall-order 消费后通知用户退款失败并允许重新发起售后。</p>
     *
     * @param refund  退款单（已推进 FAILED）
     * @param payment 关联支付单（已回退 PAID）
     */
    private void publishRefundFailed(MallRefundDO refund, MallPaymentDO payment) {
        RefundFailedEvent event = new RefundFailedEvent(refund.getRefundNo(),
                payment.getPaymentNo(), refund.getOrderNo(), refund.getAfterSaleNo(),
                payment.getUserId(), refund.getRefundAmount(),
                refund.getChannelRefundStatus(), refund.getChannelRefundNo());
        outboxPublisher.publish(MqTopicConstants.Payment.REFUND_FAILED, EVENT_REFUND_FAILED,
                OutboxPublisher.AGGREGATE_REFUND, refund.getRefundNo(), event);
    }

    /**
     * 构造成功应答
     *
     * @return 成功结果
     */
    private CallbackResult successResult() {
        return new CallbackResult(true, SUCCESS_BODY, null);
    }
}
