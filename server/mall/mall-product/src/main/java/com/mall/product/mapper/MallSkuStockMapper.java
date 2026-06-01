package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallSkuStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * SKU 库存 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface MallSkuStockMapper extends BaseMapper<MallSkuStockDO> {

    /**
     * 根据 SKU ID 查询库存（库存记录不区分删除状态，is_deleted 统一为 0）
     *
     * @param skuId SKU ID
     * @return 库存 DO
     */
    default MallSkuStockDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallSkuStockDO>().eq(MallSkuStockDO::getSkuId, skuId));
    }

    /**
     * 批量查询库存
     *
     * @param skuIds SKU ID 列表
     * @return 库存列表
     */
    default List<MallSkuStockDO> selectBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<MallSkuStockDO>().in(MallSkuStockDO::getSkuId, skuIds));
    }

    /**
     * 预扣库存（可用 → 锁定，乐观锁版本控制）
     *
     * @param skuId   SKU ID
     * @param qty     扣减数量
     * @param version 乐观锁版本号
     * @return 影响行数
     */
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

    /**
     * 释放预扣库存（锁定 → 可用，乐观锁版本控制）
     *
     * @param skuId   SKU ID
     * @param qty     释放数量
     * @param version 乐观锁版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "locked_stock = locked_stock - #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int releaseStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("version") Integer version);

    /**
     * 补货（增加可用库存）
     *
     * @param skuId   SKU ID
     * @param qty     补货数量
     * @param version 乐观锁版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int restock(@Param("skuId") Long skuId,
                @Param("qty") Integer qty,
                @Param("version") Integer version);
}
