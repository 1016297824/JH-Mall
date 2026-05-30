package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallSkuStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MallSkuStockMapper extends BaseMapper<MallSkuStockDO> {
    default MallSkuStockDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallSkuStockDO>().eq(MallSkuStockDO::getSkuId, skuId));
    }

    default List<MallSkuStockDO> selectBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<MallSkuStockDO>().in(MallSkuStockDO::getSkuId, skuIds));
    }

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock - #{qty}, " +
            "locked_stock = locked_stock + #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version} " +
            "AND available_stock >= #{qty}")
    int reserveStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("version") Integer version);

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "locked_stock = locked_stock - #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int releaseStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("version") Integer version);

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int restock(@Param("skuId") Long skuId,
                @Param("qty") Integer qty,
                @Param("version") Integer version);
}
