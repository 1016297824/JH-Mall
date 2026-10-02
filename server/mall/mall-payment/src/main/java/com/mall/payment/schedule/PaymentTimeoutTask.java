package com.mall.payment.schedule;

import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.statemachine.PaymentEventEnum;
import com.mall.payment.statemachine.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时关闭任务
 *
 * <p>对应设计文档 §4.4「超时与关闭」：扫描 {@code payment_status = UNPAID
 * AND expire_time < NOW()} 的支付单，置为 {@code CLOSED}。</p>
 *
 * <p><b>为什么不调渠道的关单 API</b>：微信 Native / 支付宝当面付这类扫码支付的
 * 订单在 {@code expire_time} 后由渠道侧自动失效，无需主动关闭；本地状态置
 * {@code CLOSED} 的作用是保证本地状态一致（避免永久停留在 UNPAID）。
 * 后续接入需要显式关单的渠道时，可在适配器层补 {@code closeOrder} 并在本任务调用。</p>
 *
 * <p><b>本任务不写 Outbox</b>：设计 §8.1 未定义「支付单关闭」事件；
 * 订单侧的关单由其自身的超时任务负责，两者相互独立。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutTask {

    /** 单轮扫描条数上限 */
    private static final int BATCH_SIZE = 200;

    private final MallPaymentMapper paymentMapper;

    private final PaymentStateMachine paymentStateMachine;

    /**
     * 关闭超时未支付的支付单
     *
     * <p>扫描间隔取配置 {@code mall.payment.timeout-scan-interval}（单位：<b>秒</b>），
     * 默认 3600 秒。注意 {@code fixedDelayString} 的单位是<b>毫秒</b>，
     * 故在占位符后拼接 {@code "000"} 完成秒→毫秒换算
     * （该写法<b>仅适用于整数秒配置</b>，若配置写成小数会得到错误间隔）。</p>
     */
    @Scheduled(fixedDelayString = "${mall.payment.timeout-scan-interval:3600}000")
    public void closeTimeoutPayments() {
        List<MallPaymentDO> candidates;
        try {
            candidates = paymentMapper.selectTimeoutUnpaid(LocalDateTime.now(), BATCH_SIZE);
        } catch (Exception e) {
            log.error("支付超时扫描失败", e);
            return;
        }
        if (candidates.isEmpty()) {
            return;
        }

        int closed = 0;
        for (MallPaymentDO payment : candidates) {
            try {
                paymentStateMachine.transition(payment, PaymentEventEnum.PAY_TIMEOUT_CLOSE);
                int rows = paymentMapper.markClosed(payment.getPaymentNo(), payment.getVersion());
                if (rows > 0) {
                    closed++;
                }
            } catch (Exception e) {
                // 单条异常不中断整批；CAS 未命中也走这里（状态已被并发方推进）
                log.warn("关闭超时支付单失败: paymentNo={}, reason={}",
                        payment.getPaymentNo(), e.getMessage());
            }
        }
        log.info("支付超时关闭完成: 候选={}, 实际关闭={}", candidates.size(), closed);
    }
}
