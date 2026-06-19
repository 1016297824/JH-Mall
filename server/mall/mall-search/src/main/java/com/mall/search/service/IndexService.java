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
     * <p>幂等去重：同一 spuId + operation 1h 内仅处理一次。</p>
     *
     * @param spuId     SPU ID
     * @param operation 操作类型（UPSERT / DELETE）
     */
    void syncProduct(Long spuId, String operation);

    /**
     * 回滚到上一个版本索引
     *
     * <p>取消当前重建操作，将别名切回最近的前一个索引版本。</p>
     */
    void rollback();
}
