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
    private final IHotProductService hotProductService;
    private final SearchSyncScheduleTask searchSyncScheduleTask;

    /**
     * 批量查询 SKU（Feign 内部调用）
     *
     * @param skuIds SKU ID 列表
     * @return SKU DTO 列表
     */
    @GetMapping("/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds) {
        return skuService.batchGetSkuDTOs(skuIds);
    }

    /**
     * 预扣库存
     *
     * @param orderNo 订单号
     * @param items   预扣项列表
     * @return 是否成功
     */
    @PostMapping("/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<ReserveStockItemRequest> items) {
        return stockService.reserveStock(orderNo, items);
    }

    /**
     * 释放预扣库存
     *
     * @param orderNo 订单号
     */
    @PostMapping("/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo) {
        stockService.releaseStock(orderNo);
    }

    /**
     * 补货
     *
     * @param skuId SKU ID
     * @param qty   数量
     */
    @PostMapping("/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty) {
        stockService.restock(skuId, qty);
    }

    /**
     * 全量查询 SPU（供搜索索引重建）
     *
     * @param page 页码
     * @param size 每页条数
     * @return SPU DTO 分页
     */
    @GetMapping("/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                     @RequestParam("size") int size) {
        return spuService.pageForFullRebuild(page, size);
    }

    /**
     * 补偿 Outbox 消息（ruoyi-job 调度）
     *
     * <p>扫描并投递待发送的搜索同步 Outbox 消息</p>
     *
     * @return 本次处理的消息数量
     */
    @PostMapping("/outbox/compensate")
    int compensateOutbox() {
        return searchSyncScheduleTask.execute();
    }

    /**
     * 刷新热点排名
     */
    @PostMapping("/hot/refresh")
    void refreshHotRank() {
        hotProductService.refreshHotRank();
    }
}
