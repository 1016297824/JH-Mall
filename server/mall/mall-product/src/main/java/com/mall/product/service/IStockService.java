package com.mall.product.service;

import com.mall.api.feign.RemoteProductService;
import java.util.List;

/**
 * 库存服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface IStockService {

    /**
     * 预扣库存
     *
     * @param orderNo 订单号（幂等键）
     * @param items   预扣项列表
     * @return 是否全部扣减成功
     */
    boolean reserveStock(String orderNo, List<RemoteProductService.ReserveStockItemRequest> items);

    /**
     * 释放预扣库存（订单取消时调用）
     *
     * <p>与 {@link #reserveStock} 对称地返回结果：存在未能成功释放的预扣项时返回 {@code false}，
     * 由调用方决定是否重试。</p>
     *
     * <p><b>异常语义</b>：扫描预扣记录失败时抛 {@link com.mall.common.exception.BusinessException}
     * （此时尚未产生任何 DB/Redis 副作用，上抛安全）；单个预扣项释放失败时<b>不抛异常</b>，
     * 而是返回 {@code false}——中途抛异常会让事务回滚 DB 却不恢复已删除的 Redis 预扣记录，
     * 反而造成更严重的不一致。</p>
     *
     * @param orderNo 订单号
     * @return true=全部释放成功（含无预扣记录的幂等情形）；false=存在未成功释放的项
     */
    boolean releaseStock(String orderNo);

    /**
     * 回补库存（增加可用库存）
     *
     * <p><b>幂等</b>：以 {@code bizNo} 为幂等键，同一业务单重复调用只回补一次。
     * 调用方（mall-order 的售后退款回调）是跨服务调用——restock 成功后本地事务若回滚，
     * MQ 重投会再次进入这里，无幂等键会造成重复回补。</p>
     *
     * @param skuId SKU ID
     * @param qty   回补数量
     * @param bizNo 业务单号（幂等键，如售后单号）
     */
    void restock(Long skuId, Integer qty, String bizNo);
}
