package com.mall.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端搜索模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-search-dev.yml 中 mall.search.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.search")
public class MallSearchConfigProperties {

    private final Es es = new Es();

    private final Query query = new Query();

    private final Page page = new Page();

    private final Suggest suggest = new Suggest();

    private final Result result = new Result();

    private final Rebuild rebuild = new Rebuild();

    private final Index index = new Index();

    private final Disk disk = new Disk();

    public Es getEs() {
        return es;
    }

    public Query getQuery() {
        return query;
    }

    public Page getPage() {
        return page;
    }

    public Suggest getSuggest() {
        return suggest;
    }

    public Result getResult() {
        return result;
    }

    public Rebuild getRebuild() {
        return rebuild;
    }

    public Index getIndex() {
        return index;
    }

    public Disk getDisk() {
        return disk;
    }

    /** ES 连接配置（标 * 需重启生效） */
    public static class Es {

        /** ES 集群地址（* 需重启） */
        private String hosts = "localhost:9200";

        /** 索引分片数（* 需重启） */
        private int shards = 3;

        /** 索引副本数（* 需重启） */
        private int replicas = 1;

        public String getHosts() {
            return hosts;
        }

        public void setHosts(String hosts) {
            this.hosts = hosts;
        }

        public int getShards() {
            return shards;
        }

        public void setShards(int shards) {
            this.shards = shards;
        }

        public int getReplicas() {
            return replicas;
        }

        public void setReplicas(int replicas) {
            this.replicas = replicas;
        }
    }

    /** 搜索查询配置 */
    public static class Query {

        /** 搜索超时（毫秒） */
        private long timeout = 2000;

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    /** 分页配置 */
    public static class Page {

        /** 单页最大条数 */
        private int maxSize = 60;

        /** 分页最大深度（from + size） */
        private int maxDepth = 10000;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    /** 搜索建议配置 */
    public static class Suggest {

        /** 搜索建议缓存 TTL（秒） */
        private long cacheTtl = 300;

        public long getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(long cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** 搜索结果缓存配置 */
    public static class Result {

        /** 搜索结果缓存 TTL（秒） */
        private long cacheTtl = 60;

        public long getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(long cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** 索引重建配置 */
    public static class Rebuild {

        /** 全量重建单批条数 */
        private int batchSize = 500;

        /** 索引版本号时间戳格式 */
        private String timestampFormat = "yyyyMMddHHmmss";

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public String getTimestampFormat() {
            return timestampFormat;
        }

        public void setTimestampFormat(String timestampFormat) {
            this.timestampFormat = timestampFormat;
        }
    }

    /** 索引配置 */
    public static class Index {

        /** 保留索引版本数 */
        private int keepVersions = 2;

        public int getKeepVersions() {
            return keepVersions;
        }

        public void setKeepVersions(int keepVersions) {
            this.keepVersions = keepVersions;
        }
    }

    /** 磁盘告警配置 */
    public static class Disk {

        /** 磁盘告警阈值（百分比） */
        private int warningThreshold = 20;

        public int getWarningThreshold() {
            return warningThreshold;
        }

        public void setWarningThreshold(int warningThreshold) {
            this.warningThreshold = warningThreshold;
        }
    }
}
