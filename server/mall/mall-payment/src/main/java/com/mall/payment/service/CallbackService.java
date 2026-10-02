package com.mall.payment.service;

import com.mall.payment.dto.response.CallbackResult;

import java.util.Map;

/**
 * 支付 / 退款回调服务
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §3.5、§5 与 §10。</p>
 *
 * <p><b>三条不可动摇的约束</b>：</p>
 * <ol>
 *   <li><b>先落库再应答</b>：支付单状态必须先在本地事务内落库，才返回成功应答；
 *       反过来会导致「平台以为成功、我们没记录」</li>
 *   <li><b>幂等</b>：渠道会重复回调同一笔交易，必须靠 nonce 去重 + 状态机 CAS 双重保证</li>
 *   <li><b>验签</b>：由渠道适配器完成（网关无渠道私钥与平台证书，做不到）</li>
 * </ol>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public interface CallbackService {

    /**
     * 处理支付回调
     *
     * @param channelCode 渠道编码
     * @param rawBody     回调原始报文
     * @param headers     回调请求头
     * @return 处理结果（含给平台的应答体）
     */
    CallbackResult processPayCallback(String channelCode, String rawBody, Map<String, String> headers);

    /**
     * 处理退款回调
     *
     * @param channelCode 渠道编码
     * @param rawBody     回调原始报文
     * @param headers     回调请求头
     * @return 处理结果（含给平台的应答体）
     */
    CallbackResult processRefundCallback(String channelCode, String rawBody, Map<String, String> headers);
}
