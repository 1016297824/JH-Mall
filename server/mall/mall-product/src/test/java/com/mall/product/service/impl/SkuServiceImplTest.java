package com.mall.product.service.impl;

import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.VO.SkuVO;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallSkuStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuServiceImplTest {

    @Mock private MallProductSkuMapper mallProductSkuMapper;
    @Mock private MallSkuStockMapper mallSkuStockMapper;
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
}
