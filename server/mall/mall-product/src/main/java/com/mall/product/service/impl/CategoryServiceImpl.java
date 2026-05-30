package com.mall.product.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import com.mall.product.convert.response.CategoryConvert;
import com.mall.product.mapper.MallCategoryMapper;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final MallCategoryMapper mallCategoryMapper;

    @Override
    public List<CategoryVO> tree() {
        List<MallCategoryDO> categoryDOList = mallCategoryMapper.selectVisibleCategories();
        return CategoryConvert.buildTree(categoryDOList);
    }

    @Override
    public CategoryVO getByCategoryId(Long categoryId) {
        MallCategoryDO categoryDO = mallCategoryMapper.selectByCategoryId(categoryId);
        if (categoryDO == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return CategoryConvert.toCategoryVO(categoryDO);
    }
}
