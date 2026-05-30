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

/**
 * 类目服务实现
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final MallCategoryMapper mallCategoryMapper;

    @Override
    public List<CategoryVO> tree() {
        // 查询全部可见类目（按排序值升序）
        List<MallCategoryDO> categoryDOList = mallCategoryMapper.selectVisibleCategories();
        // 构建多级树形结构
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
