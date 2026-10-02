package com.mall.payment.infrastructure.channel;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.payment.config.MallPaymentConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付渠道适配器工厂
 *
 * <p>按渠道编码路由到对应适配器。当前仅接入 {@link MockPayAdapter}：
 * {@code mall.payment.mock-enabled = true} 时所有渠道统一走模拟实现，
 * 使本地 / 联调环境可跑通全链路。</p>
 *
 * <p><b>接入真实渠道时</b>：把本类改为从注入的适配器列表中按 {@code channelCode} 路由，
 * 并把 {@code mock-enabled} 置为 false。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentChannelFactory {

    private final MallPaymentConfigProperties configProperties;

    private final MockPayAdapter mockPayAdapter;

    /**
     * 获取指定渠道的适配器
     *
     * @param channelCode 渠道编码，如 wechat / alipay
     * @return 渠道适配器
     */
    public PaymentChannelAdapter getAdapter(String channelCode) {
        if (configProperties.isMockEnabled()) {
            log.info("[PaymentChannelFactory] 模拟渠道已启用，渠道 {} 路由至 MockPayAdapter", channelCode);
            return mockPayAdapter;
        }
        throw new BusinessException(ErrorCode.PAYMENT_SERVICE_ERROR);
    }
}
