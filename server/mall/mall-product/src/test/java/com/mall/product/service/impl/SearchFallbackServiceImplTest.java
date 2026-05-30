package com.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.mapper.MallProductSpuMapper;
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
class SearchFallbackServiceImplTest {

    @Mock private MallProductSpuMapper mallProductSpuMapper;
    @InjectMocks private SearchFallbackServiceImpl searchFallbackService;

    @Test
    void searchShouldReturnResults() {
        MallProductSpuDO spu = new MallProductSpuDO();
        spu.setId(1L); spu.setSpuName("测试商品"); spu.setPublishStatus(1);
        Page<MallProductSpuDO> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(spu));
        when(mallProductSpuMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);

        List<SpuVO> result = searchFallbackService.search("测试", 1, 20);

        assertThat(result).isNotEmpty();
    }

    @Test
    void searchShouldThrowWhenKeywordTooShort() {
        assertThatThrownBy(() -> searchFallbackService.search("a", 1, 20))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void searchShouldThrowWhenKeywordTooLong() {
        String longKeyword = "a".repeat(51);
        assertThatThrownBy(() -> searchFallbackService.search(longKeyword, 1, 20))
                .isInstanceOf(BusinessException.class);
    }
}
