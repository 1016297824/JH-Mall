package com.mall.product.convert.response;

import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpuConvertTest {

    @Test
    void toSpuVOShouldConvertFields() {
        MallProductSpuDO spuDO = spuDO(1L, 10L, 20L, "iPhone 15", "/img.jpg", 699900L, 899900L, 5000);

        SpuVO vo = SpuConvert.toSpuVO(spuDO);

        assertThat(vo.getSpuId()).isEqualTo("1");
        assertThat(vo.getSpuName()).isEqualTo("iPhone 15");
        assertThat(vo.getMainImage()).isEqualTo("/img.jpg");
        assertThat(vo.getPriceMin()).isEqualTo(699900L);
        assertThat(vo.getPriceMax()).isEqualTo(899900L);
        assertThat(vo.getSalesCount()).isEqualTo(5000);
        assertThat(vo.getCategoryId()).isEqualTo("10");
        assertThat(vo.getBrandId()).isEqualTo("20");
    }

    @Test
    void toSpuDetailVOShouldIncludeSkus() {
        MallProductSpuDO spuDO = spuDO(1L, 10L, 20L, "iPhone 15", "/img.jpg", 699900L, 899900L, 5000);
        spuDO.setSpuDescription("<p>详情</p>");
        spuDO.setReviewCount(100);
        spuDO.setImagesJson("[\"/img1.jpg\",\"/img2.jpg\"]");

        MallProductSkuDO sku1 = new MallProductSkuDO();
        sku1.setId(101L); sku1.setSkuName("256GB 蓝色"); sku1.setPrice(699900L); sku1.setImage("/sku1.jpg");

        SpuDetailVO vo = SpuConvert.toSpuDetailVO(spuDO, List.of(sku1));

        assertThat(vo.getSpuId()).isEqualTo("1");
        assertThat(vo.getDescription()).isEqualTo("<p>详情</p>");
        assertThat(vo.getReviewCount()).isEqualTo(100);
        assertThat(vo.getImages()).containsExactly("/img1.jpg", "/img2.jpg");
        assertThat(vo.getSkus()).hasSize(1);
        assertThat(vo.getSkus().get(0).getSkuId()).isEqualTo("101");
        assertThat(vo.getSkus().get(0).getSkuName()).isEqualTo("256GB 蓝色");
        assertThat(vo.getSkus().get(0).getPrice()).isEqualTo(699900L);
    }

    private MallProductSpuDO spuDO(Long id, Long categoryId, Long brandId, String name, String image, Long priceMin, Long priceMax, Integer sales) {
        MallProductSpuDO d = new MallProductSpuDO();
        d.setId(id);
        d.setCategoryId(categoryId);
        d.setBrandId(brandId);
        d.setSpuName(name);
        d.setMainImage(image);
        d.setPriceMin(priceMin);
        d.setPriceMax(priceMax);
        d.setSalesCount(sales);
        return d;
    }
}
