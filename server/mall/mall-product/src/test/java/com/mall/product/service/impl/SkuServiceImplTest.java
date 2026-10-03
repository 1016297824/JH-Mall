package com.mall.product.service.impl;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.VO.SkuVO;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.mapper.MallSkuStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuServiceImplTest {

    @Mock private MallProductSkuMapper mallProductSkuMapper;
    @Mock private MallSkuStockMapper mallSkuStockMapper;
    @Mock private MallProductSpuMapper mallProductSpuMapper;
    @InjectMocks private SkuServiceImpl skuService;

    @Test
    void getBySkuIdShouldReturnSkuWithStock() {
        MallProductSkuDO skuDO = new MallProductSkuDO();
        skuDO.setId(101L); skuDO.setSpuId(1L); skuDO.setSkuName("256GB"); skuDO.setPrice(699900L);
        MallSkuStockDO stockDO = new MallSkuStockDO();
        stockDO.setSkuId(101L); stockDO.setAvailableStock(500);
        when(mallProductSkuMapper.selectBySkuId(101L)).thenReturn(skuDO);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stockDO);

        SkuVO vo = skuService.getBySkuId(101L);

        assertThat(vo.getSkuId()).isEqualTo("101");
        assertThat(vo.getSkuName()).isEqualTo("256GB");
        assertThat(vo.getAvailableStock()).isEqualTo(500);
    }

    @Test
    void getBySkuIdShouldThrowWhenNotFound() {
        when(mallProductSkuMapper.selectBySkuId(999L)).thenReturn(null);
        assertThatThrownBy(() -> skuService.getBySkuId(999L))
                .isInstanceOf(com.mall.common.exception.BusinessException.class);
    }

    @Test
    void batchGetSkuDTOsShouldFillSpuName() {
        MallProductSkuDO skuA = sku(101L, 1L, "256GB 蓝色");
        MallProductSkuDO skuB = sku(102L, 2L, "512GB 黑色");
        when(mallProductSkuMapper.selectBySkuIds(List.of(101L, 102L))).thenReturn(List.of(skuA, skuB));
        when(mallSkuStockMapper.selectBySkuIds(List.of(101L, 102L))).thenReturn(List.of(stock(101L, 500)));
        when(mallProductSpuMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(spu(1L, "iPhone 15 Pro Max")));

        List<ProductSkuDTO> dtos = skuService.batchGetSkuDTOs(List.of(101L, 102L));

        assertThat(dtos).hasSize(2);
        // SPU 能查到就带出名称，查不到（如 SPU 已删除）返回 null，由调用方决定兜底策略
        assertThat(dtos.get(0).getSpuName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(dtos.get(1).getSpuName()).isNull();
    }

    private MallProductSkuDO sku(Long id, Long spuId, String skuName) {
        MallProductSkuDO skuDO = new MallProductSkuDO();
        skuDO.setId(id);
        skuDO.setSpuId(spuId);
        skuDO.setSkuCode("SKU-" + id);
        skuDO.setSkuName(skuName);
        skuDO.setPrice(699900L);
        return skuDO;
    }

    private MallSkuStockDO stock(Long skuId, int available) {
        MallSkuStockDO stockDO = new MallSkuStockDO();
        stockDO.setSkuId(skuId);
        stockDO.setAvailableStock(available);
        return stockDO;
    }

    private MallProductSpuDO spu(Long id, String spuName) {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(id);
        spuDO.setSpuName(spuName);
        return spuDO;
    }
}
