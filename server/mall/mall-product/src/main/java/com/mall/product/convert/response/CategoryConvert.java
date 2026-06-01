package com.mall.product.convert.response;

import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 类目转换器（DO → VO）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class CategoryConvert {

    private CategoryConvert() {
    }

    /**
     * 类目 DO 转 VO
     *
     * @param categoryDO 类目 DO
     * @return 类目 VO
     */
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

    /**
     * 构建类目树
     *
     * <p>遍历全部扁平类目列表，parentId=0 为根节点，其余按其 parentId 挂到对应父节点 children 下。
     * 利用 ID → VO 映射实现 O(n) 时间复杂度建树。</p>
     *
     * @param categoryDOList 类目 DO 列表
     * @return 树形结构类目列表
     */
    public static List<CategoryVO> buildTree(List<MallCategoryDO> categoryDOList) {
        List<CategoryVO> allVos = categoryDOList.stream()
                .map(CategoryConvert::toCategoryVO)
                .toList();
        // 建立 ID → VO 映射，便于 O(1) 查找父节点
        Map<Long, CategoryVO> voMap = allVos.stream()
                .collect(Collectors.toMap(v -> Long.parseLong(v.getCategoryId()), v -> v));
        // 一次遍历完成树组装：parentId=0 为顶层根节点，其余找到父节点后追加到 children
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
