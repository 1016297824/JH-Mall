package com.mall.product.convert.response;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrandConvertTest {

    @Test
    void toBrandVOShouldConvertFields() {
        MallBrandDO brandDO = new MallBrandDO();
        brandDO.setId(1L);
        brandDO.setName("Apple");
        brandDO.setLogo("/logo.png");
        brandDO.setDescription("Apple Inc.");
        brandDO.setSortOrder(1);

        BrandVO vo = BrandConvert.toBrandVO(brandDO);

        assertThat(vo.getBrandId()).isEqualTo("1");
        assertThat(vo.getName()).isEqualTo("Apple");
        assertThat(vo.getLogo()).isEqualTo("/logo.png");
        assertThat(vo.getDescription()).isEqualTo("Apple Inc.");
        assertThat(vo.getSortOrder()).isEqualTo(1);
    }

    @Test
    void toBrandVOShouldReturnNullWhenDOIsNull() {
        assertThat(BrandConvert.toBrandVO(null)).isNull();
    }

    @Test
    void toBrandVOListShouldConvertList() {
        MallBrandDO b1 = new MallBrandDO(); b1.setId(1L); b1.setName("Apple"); b1.setSortOrder(1);
        MallBrandDO b2 = new MallBrandDO(); b2.setId(2L); b2.setName("华为"); b2.setSortOrder(2);

        List<BrandVO> list = BrandConvert.toBrandVOList(Arrays.asList(b1, b2));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getBrandId()).isEqualTo("1");
        assertThat(list.get(1).getName()).isEqualTo("华为");
    }
}
