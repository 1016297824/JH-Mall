package com.mall.marketing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端营销模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-marketing-dev.yml 中 mall.marketing.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.marketing")
public class MallMarketingConfigProperties {

    private final Coupon coupon = new Coupon();

    private final Calculation calculation = new Calculation();

    public Coupon getCoupon() {
        return coupon;
    }

    public Calculation getCalculation() {
        return calculation;
    }

    /** 优惠券配置 */
    public static class Coupon {

        /** 优惠券过期扫描间隔（秒，默认 3600 = 每小时） */
        private long expireScanInterval = 3600;

        /** 单次过期处理上限 */
        private int expireBatchSize = 500;

        public long getExpireScanInterval() {
            return expireScanInterval;
        }

        public void setExpireScanInterval(long expireScanInterval) {
            this.expireScanInterval = expireScanInterval;
        }

        public int getExpireBatchSize() {
            return expireBatchSize;
        }

        public void setExpireBatchSize(int expireBatchSize) {
            this.expireBatchSize = expireBatchSize;
        }
    }

    /** 优惠试算配置 */
    public static class Calculation {

        /** 优惠试算超时时间（毫秒） */
        private long timeout = 500;

        /** 过滤后参与全组合搜索的最大候选券数 */
        private int maxCandidates = 20;

        /** 促销规则匹配结果本地缓存 TTL（秒） */
        private long ruleCacheTtl = 60;

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public int getMaxCandidates() {
            return maxCandidates;
        }

        public void setMaxCandidates(int maxCandidates) {
            this.maxCandidates = maxCandidates;
        }

        public long getRuleCacheTtl() {
            return ruleCacheTtl;
        }

        public void setRuleCacheTtl(long ruleCacheTtl) {
            this.ruleCacheTtl = ruleCacheTtl;
        }
    }
}
