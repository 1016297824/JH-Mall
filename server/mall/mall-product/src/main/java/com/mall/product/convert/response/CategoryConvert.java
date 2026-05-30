package com.mall.product.convert.response;

import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoryConvert {

    private CategoryConvert() {
    }

    public static CategoryVO toCategoryVO(MallCategoryDO categoryDO) {
        if (categoryDO == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(String.valueOf(categoryDO.getId()));
        vo.setParentId(String.valueOf(categoryDO.getParentId()));
        vo.setName(categoryDO.getName());
        vo.setLevel(categoryDO.getLevel());
        vo.setIcon(categoryDO.getIcon());
        vo.setSortOrder(categoryDO.getSortOrder());
        vo.setPath(categoryDO.getPath());
        return vo;
    }

    public static List<CategoryVO> buildTree(List<MallCategoryDO> categoryDOList) {
        List<CategoryVO> allVos = categoryDOList.stream()
                .map(CategoryConvert::toCategoryVO)
                .toList();

        Map<Long, CategoryVO> voMap = allVos.stream()
                .collect(Collectors.toMap(v -> Long.parseLong(v.getCategoryId()), v -> v));

        List<CategoryVO> tree = new ArrayList<>();
        for (CategoryVO vo : allVos) {
            Long parentId = Long.parseLong(vo.getParentId());
            if (parentId == 0) {
                tree.add(vo);
            } else {
                CategoryVO parent = voMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return tree;
    }
}
