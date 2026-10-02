package com.mall.payment.schedule;

import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.service.PaymentReconcileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付回调丢失补偿任务
 *
 * <p>对应设计文档 §5.5 第二行：支付平台回调因网络中断等未送达时，
 * 扫描长时间未支付成功的支付单，主动向渠道查询真实交易状态
 * （{@code queryOrder} / {@code queryBill}），若渠道确认已收款则补记
 * {@code PAID} 并补发 {@code mall:payment:paid}。</p>
 *
 * <p>与 {@link PaymentTimeoutTask} 的扫描窗口<b>刻意不重叠</b>：
 * 本任务只处理<b>尚未过期</b>的单，关单任务只处理<b>已过期</b>的单，
 * 避免同一笔单被「对账判定未支付」与「关单」交叉处理。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
public class PaymentCompensateTask {

    /** 单轮扫描条数上限 */
    private static final int BATCH_SIZE = 100;

    private final MallPaymentMapper paymentMapper;

    private final PaymentReconcileService paymentReconcileService;

    /** 补偿延迟（秒）：创建满该时长仍无回调才主动对账，默认 30 分钟 */
    private final long compensateDelaySeconds;

    /**
     * 构造器注入
     *
     * <p>延迟值走构造器参数而非字段 {@code @Value}，便于单测直接传入固定值。</p>
     *
     * @param paymentMapper          支付单 Mapper
     * @param paymentReconcileService 对账服务
     * @param compensateDelaySeconds 补偿延迟（秒）
     */
    public PaymentCompensateTask(MallPaymentMapper paymentMapper,
                                 PaymentReconcileService paymentReconcileService,
                                 @Value("${mall.payment.compensate-delay:1800}")
                                 long compensateDelaySeconds) {
        this.paymentMapper = paymentMapper;
        this.paymentReconcileService = paymentReconcileService;
        this.compensateDelaySeconds = compensateDelaySeconds;
    }

    /**
     * 对长时间未支付成功的支付单做渠道对账
     *
     * <p>扫描间隔取 {@code mall.payment.compensate-scan-interval}（秒，默认 1800）；
     * 纳入对账的创建时间门槛取 {@code mall.payment.compensate-delay}（秒，默认 1800，
     * 即创建满 30 分钟仍无回调才主动查询，给正常回调留足到达时间）。</p>
     *
     * <p>同 {@link PaymentTimeoutTask}：占位符后拼 {@code "000"} 做秒→毫秒换算，
     * <b>仅适用于整数秒配置</b>。</p>
     */
    @Scheduled(fixedDelayString = "${mall.payment.compensate-scan-interval:1800}000")
    public void reconcileUnpaidPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<MallPaymentDO> candidates;
        try {
            candidates = paymentMapper.selectReconcileCandidates(now,
                    now.minusSeconds(compensateDelaySeconds), BATCH_SIZE);
        } catch (Exception e) {
            log.error("支付对账扫描失败", e);
            return;
        }
        if (candidates.isEmpty()) {
            return;
        }

        int reconciled = 0;
        for (MallPaymentDO payment : candidates) {
            try {
                if (paymentReconcileService.reconcile(payment)) {
                    reconciled++;
                    log.warn("支付回调丢失已补偿: paymentNo={}, orderNo={}",
                            payment.getPaymentNo(), payment.getOrderNo());
                }
            } catch (Exception e) {
                // 渠道不可用等异常不中断整批
                log.error("支付对账异常: paymentNo={}", payment.getPaymentNo(), e);
            }
        }
        log.info("支付对账完成: 候选={}, 补记={}", candidates.size(), reconciled);
    }
}
