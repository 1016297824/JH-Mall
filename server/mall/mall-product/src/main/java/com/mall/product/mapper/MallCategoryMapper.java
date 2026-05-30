package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallCategoryMapper extends BaseMapper<MallCategoryDO> {

    default List<MallCategoryDO> selectVisibleCategories() {
        return selectList(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getIsVisible, 1)
                .eq(MallCategoryDO::getIsDeleted, 0)
                .orderByAsc(MallCategoryDO::getSortOrder));
    }

    default MallCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getId, categoryId)
                .eq(MallCategoryDO::getIsDeleted, 0));
    }
}
