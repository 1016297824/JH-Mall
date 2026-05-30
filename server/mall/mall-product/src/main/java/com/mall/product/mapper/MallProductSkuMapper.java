package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallProductSkuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallProductSkuMapper extends BaseMapper<MallProductSkuDO> {

    default List<MallProductSkuDO> selectBySpuId(Long spuId) {
        return selectList(new LambdaQueryWrapper<MallProductSkuDO>()
                .eq(MallProductSkuDO::getSpuId, spuId)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }

    default MallProductSkuDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallProductSkuDO>()
                .eq(MallProductSkuDO::getId, skuId)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }

    default List<MallProductSkuDO> selectBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<MallProductSkuDO>()
                .in(MallProductSkuDO::getId, skuIds)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }
}
