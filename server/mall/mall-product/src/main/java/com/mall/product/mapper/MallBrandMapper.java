package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallBrandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallBrandMapper extends BaseMapper<MallBrandDO> {

    default List<MallBrandDO> selectAll() {
        return selectList(new LambdaQueryWrapper<MallBrandDO>()
                .eq(MallBrandDO::getIsDeleted, 0)
                .orderByAsc(MallBrandDO::getSortOrder));
    }
}
