package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallProductSkuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SKU Mapper
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface MallProductSkuMapper extends BaseMapper<MallProductSkuDO> {

    /**
     * 根据 SPU ID 查询 SKU 列表
     *
     * @param spuId SPU ID
     * @return SKU 列表
     */
    default List<MallProductSkuDO> selectBySpuId(Long spuId) {
        return selectList(new LambdaQueryWrapper<MallProductSkuDO>()
                .eq(MallProductSkuDO::getSpuId, spuId)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }

    /**
     * 根据 SKU ID 查询
     *
     * @param skuId SKU ID
     * @return SKU DO
     */
    default MallProductSkuDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallProductSkuDO>()
                .eq(MallProductSkuDO::getId, skuId)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }

    /**
     * 批量查询 SKU
     *
     * @param skuIds SKU ID 列表
     * @return SKU 列表
     */
    default List<MallProductSkuDO> selectBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<MallProductSkuDO>()
                .in(MallProductSkuDO::getId, skuIds)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }
}
