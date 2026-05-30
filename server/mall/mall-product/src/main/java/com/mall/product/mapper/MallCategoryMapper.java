package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 类目 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface MallCategoryMapper extends BaseMapper<MallCategoryDO> {

    /**
     * 查询所有可见类目（按排序值升序）
     *
     * @return 可见类目列表
     */
    default List<MallCategoryDO> selectVisibleCategories() {
        return selectList(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getIsVisible, 1)
                .eq(MallCategoryDO::getIsDeleted, 0)
                .orderByAsc(MallCategoryDO::getSortOrder));
    }

    /**
     * 根据 ID 查询类目
     *
     * @param categoryId 类目 ID
     * @return 类目 DO
     */
    default MallCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getId, categoryId)
                .eq(MallCategoryDO::getIsDeleted, 0));
    }
}
