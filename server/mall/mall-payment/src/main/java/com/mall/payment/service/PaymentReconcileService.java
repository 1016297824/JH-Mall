package com.mall.payment.service;

import com.mall.payment.DO.MallPaymentDO;

/**
 * 支付单对账服务
 *
 * <p>用于「支付平台回调丢失」的补偿场景（设计文档 §5.5 第二行）：
 * 定时任务扫描长时间未支付成功的支付单，主动向渠道查询真实交易状态，
 * 若渠道确认已收款但本地仍是未支付，则补记状态并补发事件。</p>
 *
 * <p>单独成服务而非并入任务类，是为了让「CAS 落库 + Outbox 落库」这两步
 * 处于同一事务（{@code @Transactional} 在同类内自调用不生效，
 * 写在定时任务里会被代理绕过）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public interface PaymentReconcileService {

    /**
     * 对单个支付单做渠道对账
     *
     * <p>仅当渠道明确返回「已支付」且本地 CAS 推进成功时才返回 {@code true}，
     * 并同时补发 {@code mall:payment:paid} 事件。其余情况（未发起渠道、
     * 渠道未支付、CAS 未命中、渠道查询失败）一律返回 {@code false}，由调用方决定后续动作。</p>
     *
     * <p><b>本方法不吞异常</b>：渠道查询可能抛运行时异常，由调用方兜底记录，
     * 避免单条记录异常中断整批扫描。</p>
     *
     * @param payment 待对账的支付单（调用方已查出）
     * @return 渠道确认已支付并成功补记返回 true
     */
    boolean reconcile(MallPaymentDO payment);
}
