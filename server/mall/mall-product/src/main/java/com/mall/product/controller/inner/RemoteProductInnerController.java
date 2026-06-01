package com.mall.product.controller.inner;

import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.product.service.IHotProductService;
import com.mall.product.service.ISkuService;
import com.mall.product.service.ISpuService;
import com.mall.product.service.IStockService;
import com.mall.product.infrastructure.schedule.SearchSyncScheduleTask;
import com.mall.product.infrastructure.schedule.HotRankRefreshTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品内部 Controller（Feign 内部调用）
 *
 * <p>供 mall-order、mall-search 等模块通过 Feign 调用，不走网关认证</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@RestController
@RequestMapping("/inner/product")
@RequiredArgsConstructor
public class RemoteProductInnerController {

    private final ISkuService skuService;
    private final IStockService stockService;
    private final ISpuService spuService;
    private final SearchSyncScheduleTask searchSyncScheduleTask;
    private final HotRankRefreshTask hotRankRefreshTask;

    /**
     * 批量查询 SKU（Feign 内部调用）
     *
     * <p>供 mall-order 订单模块查询商品快照，含库存在售状态判断</p>
     *
     * @param skuIds SKU ID 列表
     * @return SKU DTO 列表（含可用库存、在售标记）
     */
    @GetMapping("/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds) {
        return skuService.batchGetSkuDTOs(skuIds);
    }

    /**
     * 预扣库存（乐观锁并发安全）
     *
     * <p>供 mall-order 下单时调用，逐项乐观锁扣减 available → locked。
     * 预扣成功后在 Redis 记录 orderNo:skuId 幂等键。</p>
     *
     * @param orderNo 订单号（幂等键前缀）
     * @param items   预扣项列表
     * @return 是否全部扣减成功
     */
    @PostMapping("/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<ReserveStockItemRequest> items) {
        return stockService.reserveStock(orderNo, items);
    }

    /**
     * 释放预扣库存（订单取消时调用）
     *
     * <p>供 mall-order 订单取消时释放已锁库存，locked → available</p>
     *
     * @param orderNo 订单号
     */
    @PostMapping("/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo) {
        stockService.releaseStock(orderNo);
    }

    /**
     * 补货（增加可用库存）
     *
     * <p>供 mall-admin 管理端或售后流程调用，乐观锁增加 available_stock</p>
     *
     * @param skuId SKU ID
     * @param qty   补货数量
     */
    @PostMapping("/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty) {
        stockService.restock(skuId, qty);
    }

    /**
     * 全量分页查询 SPU（供搜索索引重建）
     *
     * <p>返回全部未删除 SPU（不限上架状态），供 mall-search 模块重建搜索索引使用</p>
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return SPU DTO 分页
     */
    @GetMapping("/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                     @RequestParam("size") int size) {
        return spuService.pageForFullRebuild(page, size);
    }

    /**
     * 补偿 Outbox 消息（ruoyi-job 定时调度）
     *
     * <p>扫描 Outbox 表中状态为 NEW 的搜索同步消息，逐条补偿投递。
     * 搜索引擎不可用导致实时同步失败时，由此定时任务兜底。</p>
     *
     * @return 本次处理的消息数量
     */
    @PostMapping("/outbox/compensate")
    int compensateOutbox() {
        return searchSyncScheduleTask.execute();
    }

    /**
     * 全量刷新热点排名（Feign 内部调用）
     *
     * <p>综合销量分 + UV 分加权重算 ZSet score，
     * SETNX 分布式锁防并发，ruoyi-job 定时调度。
     * 同时清理未进 Top N 的过期 UV HyperLogLog 键。</p>
     */
    @PostMapping("/hot/refresh")
    void refreshHotRank() {
        hotRankRefreshTask.execute();
    }
}
