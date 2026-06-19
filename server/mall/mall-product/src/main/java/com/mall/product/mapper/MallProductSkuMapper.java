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

    /**
     * 根据 SPU ID 列表批量查询 SKU（供 N+1 优化使用）
     *
     * @param spuIds SPU ID 列表
     * @return 所有关联 SKU（调用方按 spuId 分组）
     */
    default List<MallProductSkuDO> batchSelectBySpuIds(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<MallProductSkuDO>()
                .in(MallProductSkuDO::getSpuId, spuIds)
                .eq(MallProductSkuDO::getIsDeleted, 0));
    }
}
