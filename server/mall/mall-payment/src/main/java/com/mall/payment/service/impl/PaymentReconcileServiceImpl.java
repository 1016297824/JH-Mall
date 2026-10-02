package com.mall.payment.service.impl;

import com.mall.common.constant.MqTopicConstants;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.dto.event.PaymentPaidEvent;
import com.mall.payment.infrastructure.channel.ChannelBillResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.outbox.OutboxPublisher;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.service.PaymentReconcileService;
import com.mall.payment.statemachine.PaymentEventEnum;
import com.mall.payment.statemachine.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 支付单对账服务实现
 *
 * <p>对应设计文档 §5.5「支付平台回调未收到（网络中断）」的补偿：
 * 主动查询渠道交易状态，渠道确认已收款则补记 {@code PAID} 并补发
 * {@code mall:payment:paid}。</p>
 *
 * <p><b>保守原则</b>：任何不确定的情形（无渠道单号、渠道查询失败、
 * 渠道状态无法判定为已支付、CAS 未命中）一律返回 {@code false} 且不改动本地状态。
 * 宁可漏补（下一轮扫描会重试），也不能把未支付的单误记为已支付。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconcileServiceImpl implements PaymentReconcileService {

    /** 事件类型：支付成功 */
    private static final String EVENT_PAYMENT_PAID = "PaymentPaid";

    /**
     * 渠道侧「已支付」交易状态集合
     *
     * <p>微信：{@code SUCCESS}；支付宝：{@code TRADE_SUCCESS} / {@code TRADE_FINISHED}。
     * 各渠道状态集不同，理想位置是下沉到适配器（渠道差异应封装在适配层），
     * 但当前仅接入模拟渠道且渠道状态字符串由适配器原样透出，
     * 故先集中在此维护；接入真实渠道时若状态集差异扩大，应改为适配器方法。</p>
     */
    private static final Set<String> PAID_TRADE_STATUSES =
            Set.of("SUCCESS", "TRADE_SUCCESS", "TRADE_FINISHED");

    private final MallPaymentMapper paymentMapper;

    private final MallPaymentChannelMapper channelMapper;

    private final PaymentChannelFactory channelFactory;

    private final PaymentStateMachine paymentStateMachine;

    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reconcile(MallPaymentDO payment) {
        // ① 未发起过渠道则无从对账（用户可能从未扫码）
        String channelPaymentNo = payment.getChannelPaymentNo();
        if (channelPaymentNo == null || channelPaymentNo.isBlank()) {
            return false;
        }

        // ② 主动向渠道查询真实交易状态
        MallPaymentChannelDO channel = channelMapper.selectByChannelCode(payment.getChannelCode());
        PaymentChannelAdapter adapter = channelFactory.getAdapter(payment.getChannelCode());
        ChannelBillResult bill = adapter.queryBill(channelPaymentNo, channel);

        // ③ 渠道查询失败 / 未支付：保守不动本地状态，等下一轮或回调
        if (!bill.isSuccess() || !isPaidTradeStatus(bill.getChannelTradeStatus())) {
            log.debug("对账未确认已支付: paymentNo={}, querySuccess={}, tradeStatus={}",
                    payment.getPaymentNo(), bill.isSuccess(), bill.getChannelTradeStatus());
            return false;
        }

        // ④ 渠道确认已收款：状态机推进 + 乐观锁 CAS 落库
        paymentStateMachine.transition(payment, PaymentEventEnum.PAY_SUCCESS_CALLBACK);
        int rows = paymentMapper.markPaid(payment.getPaymentNo(), bill.getChannelTradeStatus(),
                payment.getVersion());

        // ⑤ CAS 未命中说明状态已被并发方（回调/其他实例）推进，不再补发事件
        if (rows == 0) {
            log.info("对账 CAS 未命中，状态已被并发推进: paymentNo={}", payment.getPaymentNo());
            return false;
        }

        // ⑥ 与状态推进同一本地事务补发支付成功事件（设计 §5.4 / §8.3）
        PaymentPaidEvent event = new PaymentPaidEvent(payment.getPaymentNo(), payment.getOrderNo(),
                payment.getUserId(), payment.getPayAmount(), payment.getPaySuccessTime(),
                channelPaymentNo, payment.getChannelCode());
        outboxPublisher.publish(MqTopicConstants.Payment.PAID, EVENT_PAYMENT_PAID,
                OutboxPublisher.AGGREGATE_PAYMENT, payment.getPaymentNo(), event);

        log.warn("对账补记支付成功: paymentNo={}, orderNo={}, tradeStatus={}",
                payment.getPaymentNo(), payment.getOrderNo(), bill.getChannelTradeStatus());
        return true;
    }

    /**
     * 判定渠道交易状态是否为「已支付」
     *
     * @param channelTradeStatus 渠道侧交易状态原文
     * @return 已支付返回 true
     */
    private static boolean isPaidTradeStatus(String channelTradeStatus) {
        return channelTradeStatus != null
                && PAID_TRADE_STATUSES.contains(channelTradeStatus.trim().toUpperCase());
    }
}
