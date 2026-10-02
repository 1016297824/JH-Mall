package com.mall.payment.infrastructure.channel;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.payment.config.MallPaymentConfigProperties;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * PaymentChannelFactory 单元测试
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@DisplayName("PaymentChannelFactory 渠道适配器工厂")
class PaymentChannelFactoryTest {

    private final MockPayAdapter mockPayAdapter = new MockPayAdapter();

    @Test
    @DisplayName("启用模拟渠道时，任意渠道编码都路由到 MockPayAdapter")
    void mockEnabledRoutesToMockAdapter() {
        PaymentChannelFactory factory = factory(true);

        assertThat(factory.getAdapter("wechat")).isSameAs(mockPayAdapter);
        assertThat(factory.getAdapter("alipay")).isSameAs(mockPayAdapter);
    }

    @Test
    @DisplayName("未启用模拟渠道且真实适配器尚未接入时，抛支付服务异常")
    void mockDisabledThrowsPaymentServiceError() {
        PaymentChannelFactory factory = factory(false);

        ThrowingCallable callable = () -> factory.getAdapter("wechat");

        BusinessException ex = catchThrowableOfType(callable, BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_SERVICE_ERROR.getCode());
    }

    /**
     * 构造工厂夹具
     *
     * @param mockEnabled 是否启用模拟渠道
     * @return 工厂
     */
    private PaymentChannelFactory factory(boolean mockEnabled) {
        MallPaymentConfigProperties config = new MallPaymentConfigProperties();
        config.setMockEnabled(mockEnabled);
        return new PaymentChannelFactory(config, mockPayAdapter);
    }
}
