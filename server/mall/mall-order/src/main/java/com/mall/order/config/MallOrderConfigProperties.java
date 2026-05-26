package com.mall.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端订单模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-order-dev.yml 中 mall.order.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.order")
public class MallOrderConfigProperties {

    /** 下单后未支付自动关闭时间（分钟） */
    private int payExpireMinutes = 30;

    /** 超时关单延迟消息 topic */
    private String timeoutTopic = "mall:order:timeout";

    /** ruoyi-job 兜底日扫 cron 表达式 */
    private String timeoutFallbackCron = "0 0 2 * * ?";

    /** 收货后可申请售后天数 */
    private int refundDays = 7;

    /** 发货后自动确认收货天数 */
    private int autoReceiveDays = 15;

    /** 购物车最多商品数 */
    private int cartMaxItems = 99;

    /** 售后自动审核金额阈值（分） */
    private int autoApproveThreshold = 10000;

    public int getPayExpireMinutes() {
        return payExpireMinutes;
    }

    public void setPayExpireMinutes(int payExpireMinutes) {
        this.payExpireMinutes = payExpireMinutes;
    }

    public String getTimeoutTopic() {
        return timeoutTopic;
    }

    public void setTimeoutTopic(String timeoutTopic) {
        this.timeoutTopic = timeoutTopic;
    }

    public String getTimeoutFallbackCron() {
        return timeoutFallbackCron;
    }

    public void setTimeoutFallbackCron(String timeoutFallbackCron) {
        this.timeoutFallbackCron = timeoutFallbackCron;
    }

    public int getRefundDays() {
        return refundDays;
    }

    public void setRefundDays(int refundDays) {
        this.refundDays = refundDays;
    }

    public int getAutoReceiveDays() {
        return autoReceiveDays;
    }

    public void setAutoReceiveDays(int autoReceiveDays) {
        this.autoReceiveDays = autoReceiveDays;
    }

    public int getCartMaxItems() {
        return cartMaxItems;
    }

    public void setCartMaxItems(int cartMaxItems) {
        this.cartMaxItems = cartMaxItems;
    }

    public int getAutoApproveThreshold() {
        return autoApproveThreshold;
    }

    public void setAutoApproveThreshold(int autoApproveThreshold) {
        this.autoApproveThreshold = autoApproveThreshold;
    }
}
