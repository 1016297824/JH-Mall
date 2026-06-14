package com.mall.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端商品模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-product-dev.yml 中 mall.product.* 配置</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.product")
public class MallProductConfigProperties {

    private final Sku sku = new Sku();

    private final Category category = new Category();

    private final Search search = new Search();

    private final Stock stock = new Stock();

    public Sku getSku() {
        return sku;
    }

    public Category getCategory() {
        return category;
    }

    public Search getSearch() {
        return search;
    }

    public Stock getStock() {
        return stock;
    }

    /** SKU 缓存配置 */
    public static class Sku {

        /** SKU 缓存 TTL（秒，默认 600 = 10 分钟） */
        private long cacheTtl = 600;

        public long getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(long cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** 类目缓存配置 */
    public static class Category {

        /** 类目树缓存 TTL（秒，默认 1800 = 30 分钟） */
        private long cacheTtl = 1800;

        public long getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(long cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** 搜索同步配置 */
    public static class Search {

        /** 搜索同步 Outbox 单次投递上限 */
        private int syncBatchSize = 100;

        private final Fallback fallback = new Fallback();

        public int getSyncBatchSize() {
            return syncBatchSize;
        }

        public void setSyncBatchSize(int syncBatchSize) {
            this.syncBatchSize = syncBatchSize;
        }

        public Fallback getFallback() {
            return fallback;
        }

        /** 降级搜索配置 */
        public static class Fallback {

            /** 降级搜索超时（毫秒） */
            private long timeout = 3000;

            /** 降级搜索单页上限 */
            private int maxSize = 100;

            public long getTimeout() {
                return timeout;
            }

            public void setTimeout(long timeout) {
                this.timeout = timeout;
            }

            public int getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }
        }
    }

    /** 库存配置 */
    public static class Stock {

        private final Compensate compensate = new Compensate();

        public Compensate getCompensate() {
            return compensate;
        }

        /** 库存补偿配置 */
        public static class Compensate {

            /** 库存释放幂等键 TTL（秒，默认 86400 = 24 小时） */
            private long ttl = 86400;

            public long getTtl() {
                return ttl;
            }

            public void setTtl(long ttl) {
                this.ttl = ttl;
            }
        }
    }

    private final Hot hot = new Hot();

    public Hot getHot() {
        return hot;
    }

    /** 热销商品排行配置（综合热度 = salesCount * 10 * salesWeight + uv * 10 * uvWeight） */
    public static class Hot {

        /** 排行榜最大保留数量（ZSet 只保留 Top N） */
        private int rankMaxSize = 200;

        /** C 端热点列表单次最大条数（接口请求 limit 超过此值时拒绝） */
        private int hotListLimit = 50;

        /** 销量权重（默认 0.6，与 uvWeight 之和应等于 1.0） */
        private double salesWeight = 0.6;

        /** 独立访客量权重（默认 0.4，与 salesWeight 之和应等于 1.0） */
        private double uvWeight = 0.4;

        /** UV 滑动窗口天数（默认 7，只统计最近 N 天独立访客） */
        private int uvWindowDays = 7;

        public int getRankMaxSize() {
            return rankMaxSize;
        }

        public void setRankMaxSize(int rankMaxSize) {
            this.rankMaxSize = rankMaxSize;
        }

        public int getHotListLimit() {
            return hotListLimit;
        }

        public void setHotListLimit(int hotListLimit) {
            this.hotListLimit = hotListLimit;
        }

        public double getSalesWeight() {
            return salesWeight;
        }

        public void setSalesWeight(double salesWeight) {
            this.salesWeight = salesWeight;
        }

        public double getUvWeight() {
            return uvWeight;
        }

        public void setUvWeight(double uvWeight) {
            this.uvWeight = uvWeight;
        }

        public int getUvWindowDays() {
            return uvWindowDays;
        }

        public void setUvWindowDays(int uvWindowDays) {
            this.uvWindowDays = uvWindowDays;
        }
    }
}
