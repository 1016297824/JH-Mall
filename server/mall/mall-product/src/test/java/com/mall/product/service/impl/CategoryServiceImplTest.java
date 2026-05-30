package com.mall.product.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import com.mall.product.mapper.MallCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private MallCategoryMapper mallCategoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void treeShouldReturnCategoryTree() {
        MallCategoryDO root = categoryDO(1L, 0L, "手机数码", 1);
        MallCategoryDO l2 = categoryDO(2L, 1L, "手机通讯", 2);
        when(mallCategoryMapper.selectVisibleCategories()).thenReturn(Arrays.asList(root, l2));

        List<CategoryVO> tree = categoryService.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("手机数码");
        assertThat(tree.get(0).getChildren()).hasSize(1);
    }

    @Test
    void getByCategoryIdShouldReturnCategory() {
        MallCategoryDO categoryDO = categoryDO(1L, 0L, "手机数码", 1);
        when(mallCategoryMapper.selectByCategoryId(1L)).thenReturn(categoryDO);

        CategoryVO vo = categoryService.getByCategoryId(1L);

        assertThat(vo.getCategoryId()).isEqualTo("1");
        assertThat(vo.getName()).isEqualTo("手机数码");
    }

    @Test
    void getByCategoryIdShouldThrowWhenNotFound() {
        when(mallCategoryMapper.selectByCategoryId(999L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.getByCategoryId(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
    }

    private MallCategoryDO categoryDO(Long id, Long parentId, String name, Integer level) {
        MallCategoryDO d = new MallCategoryDO();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        d.setLevel(level);
        d.setSortOrder(1);
        d.setIsVisible(1);
        return d;
    }
}
