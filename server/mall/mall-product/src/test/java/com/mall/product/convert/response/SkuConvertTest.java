package com.mall.product.convert.response;

import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.VO.SkuVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkuConvertTest {

    @Test
    void toSkuVOShouldConvertFields() {
        MallProductSkuDO skuDO = new MallProductSkuDO();
        skuDO.setId(101L);
        skuDO.setSpuId(1L);
        skuDO.setSkuCode("SKU001");
        skuDO.setSkuName("256GB 蓝色");
        skuDO.setPrice(699900L);
        skuDO.setMarketPrice(799900L);
        skuDO.setImage("/sku.jpg");
        skuDO.setWeight(200);

        SkuVO vo = SkuConvert.toSkuVO(skuDO);

        assertThat(vo.getSkuId()).isEqualTo("101");
        assertThat(vo.getSpuId()).isEqualTo("1");
        assertThat(vo.getSkuCode()).isEqualTo("SKU001");
        assertThat(vo.getSkuName()).isEqualTo("256GB 蓝色");
        assertThat(vo.getPrice()).isEqualTo(699900L);
        assertThat(vo.getMarketPrice()).isEqualTo(799900L);
        assertThat(vo.getImage()).isEqualTo("/sku.jpg");
        assertThat(vo.getWeight()).isEqualTo(200);
    }

    @Test
    void toSkuVOShouldNotIncludeCostPrice() {
        MallProductSkuDO skuDO = new MallProductSkuDO();
        skuDO.setId(101L);
        skuDO.setSpuId(1L);
        skuDO.setCostPrice(500000L);

        SkuVO vo = SkuConvert.toSkuVO(skuDO);

        assertThat(vo.getSkuId()).isEqualTo("101");
        assertThat(vo.getSpuId()).isEqualTo("1");
    }
}
