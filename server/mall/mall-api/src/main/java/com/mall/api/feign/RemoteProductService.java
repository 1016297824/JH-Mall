package com.mall.api.feign;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.common.DTO.PageResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C 端商品服务 Feign 接口
 *
 * <p>提供给 mall-order、mall-search 等模块内部调用</p>
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@FeignClient(contextId = "mall-product", value = "mall-product")
public interface RemoteProductService {

    /**
     * 批量查询 SKU 信息
     *
     * @param skuIds SKU ID 列表
     * @return SKU DTO 列表
     */
    @GetMapping("/inner/product/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds);

    /**
     * 库存预占（下单锁定库存）
     *
     * @param orderNo 订单号
     * @param items   预占项列表
     * @return 是否全部预占成功
     */
    @PostMapping("/inner/product/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<ReserveStockItemRequest> items);

    /**
     * 释放库存（取消订单 / 超时取消）
     *
     * @param orderNo 订单号
     */
    @PostMapping("/inner/product/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo);

    /**
     * 回补库存（退货入库）
     *
     * @param skuId SKU ID
     * @param qty   回补数量
     */
    @PostMapping("/inner/product/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty);

    /**
     * 分页拉取全量 SPU（搜索索引重建用）
     *
     * @param page 页码
     * @param size 每页条数
     * @return SPU 分页结果
     */
    @GetMapping("/inner/product/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                    @RequestParam("size") int size);

    /**
     * 库存预占请求项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ReserveStockItemRequest {
        /** SKU ID */
        private Long skuId;
        /** 锁定数量 */
        private Integer qty;
    }
}
