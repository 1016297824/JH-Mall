package com.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.DTO.PageResult;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.service.IHotProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpuServiceImplTest {

    @Mock
    private MallProductSpuMapper mallProductSpuMapper;

    @Mock
    private MallProductSkuMapper mallProductSkuMapper;

    @Mock
    private IHotProductService hotProductService;

    @InjectMocks
    private SpuServiceImpl spuService;

    @Test
    void pageShouldReturnSpuList() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setSpuName("iPhone");
        spuDO.setPublishStatus(1);
        spuDO.setVerifyStatus(1);
        spuDO.setCategoryId(10L);
        spuDO.setBrandId(20L);
        spuDO.setPriceMin(699900L);
        spuDO.setPriceMax(899900L);
        spuDO.setSalesCount(100);

        Page<MallProductSpuDO> mockPage = new Page<>(1, 20, 1);
        mockPage.setRecords(List.of(spuDO));
        when(mallProductSpuMapper.selectPublishedPage(any(), any(), any(), any())).thenReturn(mockPage);

        PageResult<SpuVO> result = spuService.page(1, 20, null, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().get(0).getSpuName()).isEqualTo("iPhone");
    }

    @Test
    void detailShouldReturnSpuWithSkus() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setSpuName("iPhone");
        spuDO.setPublishStatus(1);
        spuDO.setVerifyStatus(1);
        spuDO.setCategoryId(10L);
        spuDO.setBrandId(20L);
        spuDO.setPriceMin(699900L);
        spuDO.setPriceMax(899900L);
        spuDO.setSalesCount(100);
        spuDO.setSpuDescription("<p>test</p>");

        MallProductSkuDO sku = new MallProductSkuDO();
        sku.setId(101L);
        sku.setSkuName("256GB");
        sku.setPrice(699900L);
        sku.setImage("/sku.jpg");

        when(mallProductSpuMapper.selectById(1L)).thenReturn(spuDO);
        when(mallProductSkuMapper.selectBySpuId(1L)).thenReturn(List.of(sku));

        SpuDetailVO detail = spuService.detail(1L);

        assertThat(detail.getSpuId()).isEqualTo("1");
        assertThat(detail.getSkus()).hasSize(1);
        assertThat(detail.getSkus().get(0).getSkuName()).isEqualTo("256GB");
    }

    @Test
    void detailShouldThrowWhenNotPublished() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setPublishStatus(0);
        spuDO.setVerifyStatus(1);
        when(mallProductSpuMapper.selectById(1L)).thenReturn(spuDO);

        assertThatThrownBy(() -> spuService.detail(1L))
                .isInstanceOf(com.mall.common.exception.BusinessException.class);
    }
}
