package com.mall.search.service;

/**
 * 商品搜索索引管理服务
 *
 * <p>负责全量/增量索引重建、商品同步及回滚。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface IndexService {

    /**
     * 全量重建索引
     *
     * <p>分布式锁保护，防止并发重建。流程：创建新索引 - 全量灌入 - 增量回补 - 别名切换。</p>
     */
    void rebuildIndex();

    /**
     * 单商品索引同步（增量）
     *
     * <p><b>不做时间窗去重</b>：曾经实现的「同一 spuId + operation 1h 内只处理一次」
     * 会让同一商品的第二次变更被静默丢弃（索引停在旧值直到手工全量重建），已移除。
     * ES 的 upsert / delete 本身幂等，重复同步无副作用。</p>
     *
     * <p>乱序保护由 {@code sourceTimestamp} 承担：只接受比上一次已生效的同步更新的消息。</p>
     *
     * @param spuId           SPU ID
     * @param operation       操作类型（UPSERT / DELETE）
     * @param sourceTimestamp 生产端写入的时间戳（epoch 毫秒），越小表示变更越早
     */
    void syncProduct(Long spuId, String operation, long sourceTimestamp);

    /**
     * 回滚到上一个版本索引
     *
     * <p>取消当前重建操作，将别名切回最近的前一个索引版本。</p>
     */
    void rollback();
}
