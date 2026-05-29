package com.mall.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端支付模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-payment-dev.yml 中 mall.payment.* 配置</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.payment")
public class MallPaymentConfigProperties {

    /** 启用的支付渠道列表，逗号分隔（* 需重启） */
    private String channels = "wechat,alipay";

    /** 渠道配置本地缓存 TTL（秒） */
    private long channelCacheTtl = 300;

    /** 支付单创建后 N 秒未支付，主动调渠道查交易状态 */
    private long activeQueryDelay = 1800;

    private final Callback callback = new Callback();

    private final Channel channel = new Channel();

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public long getChannelCacheTtl() {
        return channelCacheTtl;
    }

    public void setChannelCacheTtl(long channelCacheTtl) {
        this.channelCacheTtl = channelCacheTtl;
    }

    public long getActiveQueryDelay() {
        return activeQueryDelay;
    }

    public void setActiveQueryDelay(long activeQueryDelay) {
        this.activeQueryDelay = activeQueryDelay;
    }

    public Callback getCallback() {
        return callback;
    }

    public Channel getChannel() {
        return channel;
    }

    /** 回调配置 */
    public static class Callback {

        /** 支付平台回调/主动查询超时时间（秒） */
        private long timeout = 30;

        /** 回调 nonce 去重 TTL（秒） */
        private long nonceTtl = 86400;

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getNonceTtl() {
            return nonceTtl;
        }

        public void setNonceTtl(long nonceTtl) {
            this.nonceTtl = nonceTtl;
        }
    }

    /** 渠道配置 */
    public static class Channel {

        private final Wechat wechat = new Wechat();

        private final Alipay alipay = new Alipay();

        public Wechat getWechat() {
            return wechat;
        }

        public Alipay getAlipay() {
            return alipay;
        }

        /** 微信支付配置 */
        public static class Wechat {

            /** 微信 AppId（* 需重启） */
            private String appId;

            /** 微信商户号（* 需重启） */
            private String mchId;

            /** 微信 APIv3 密钥（加密存储） */
            private String apiV3Key;

            /** 微信商户私钥（加密存储，PEM 格式） */
            private String privateKey;

            /** 微信商户证书序列号（* 需重启） */
            private String serialNo;

            public String getAppId() {
                return appId;
            }

            public void setAppId(String appId) {
                this.appId = appId;
            }

            public String getMchId() {
                return mchId;
            }

            public void setMchId(String mchId) {
                this.mchId = mchId;
            }

            public String getApiV3Key() {
                return apiV3Key;
            }

            public void setApiV3Key(String apiV3Key) {
                this.apiV3Key = apiV3Key;
            }

            public String getPrivateKey() {
                return privateKey;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public String getSerialNo() {
                return serialNo;
            }

            public void setSerialNo(String serialNo) {
                this.serialNo = serialNo;
            }
        }

        /** 支付宝支付配置 */
        public static class Alipay {

            /** 支付宝 AppId（* 需重启） */
            private String appId;

            /** 支付宝应用私钥（加密存储） */
            private String privateKey;

            /** 支付宝公钥（* 需重启） */
            private String publicKey;

            public String getAppId() {
                return appId;
            }

            public void setAppId(String appId) {
                this.appId = appId;
            }

            public String getPrivateKey() {
                return privateKey;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public String getPublicKey() {
                return publicKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }
        }
    }
}
