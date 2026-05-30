package com.mall.product.service.impl;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;
import com.mall.product.mapper.MallBrandMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private MallBrandMapper mallBrandMapper;

    @InjectMocks
    private BrandServiceImpl brandService;

    @Test
    void listShouldReturnAllBrands() {
        MallBrandDO b1 = new MallBrandDO(); b1.setId(1L); b1.setName("Apple"); b1.setSortOrder(1);
        when(mallBrandMapper.selectAll()).thenReturn(List.of(b1));

        List<BrandVO> result = brandService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Apple");
    }

    @Test
    void listShouldReturnEmptyWhenNoBrands() {
        when(mallBrandMapper.selectAll()).thenReturn(List.of());

        List<BrandVO> result = brandService.list(null);

        assertThat(result).isEmpty();
    }
}
