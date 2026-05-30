package com.mall.product.controller.inner;

import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.product.service.ISkuService;
import com.mall.product.service.ISpuService;
import com.mall.product.service.IStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/inner/product")
@RequiredArgsConstructor
public class RemoteProductInnerController {

    private final ISkuService skuService;
    private final IStockService stockService;
    private final ISpuService spuService;

    @GetMapping("/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds) {
        return skuService.batchGetSkuDTOs(skuIds);
    }

    @PostMapping("/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<ReserveStockItemRequest> items) {
        return stockService.reserveStock(orderNo, items);
    }

    @PostMapping("/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo) {
        stockService.releaseStock(orderNo);
    }

    @PostMapping("/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty) {
        stockService.restock(skuId, qty);
    }

    @GetMapping("/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                     @RequestParam("size") int size) {
        return spuService.pageForFullRebuild(page, size);
    }
}
