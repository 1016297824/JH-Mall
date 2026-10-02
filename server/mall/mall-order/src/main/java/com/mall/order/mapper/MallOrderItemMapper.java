package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallOrderItemDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallOrderItemMapper extends BaseMapper<MallOrderItemDO> {

    /**
     * 按订单 ID 查询订单项
     *
     * @param orderId 订单 ID
     * @return 订单项列表
     */
    default List<MallOrderItemDO> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<MallOrderItemDO>()
                .eq(MallOrderItemDO::getOrderId, orderId)
                .eq(MallOrderItemDO::getIsDeleted, 0)
                .orderByAsc(MallOrderItemDO::getId));
    }

    /**
     * 批量插入订单项
     *
     * <p>下单编排时与订单主表、金额快照、Outbox 同事务写入（设计文档 §5.6）。</p>
     *
     * @param items 订单项列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO mall_order_item " +
            "(order_id, spu_id, sku_id, sku_code, sku_name, spu_name, main_image, attrs_json, " +
            " quantity, price, total_price, is_deleted, create_time, update_time) VALUES " +
            "<foreach collection='items' item='it' separator=','>" +
            "(#{it.orderId}, #{it.spuId}, #{it.skuId}, #{it.skuCode}, #{it.skuName}, #{it.spuName}, " +
            " #{it.mainImage}, #{it.attrsJson}, #{it.quantity}, #{it.price}, #{it.totalPrice}, " +
            " 0, NOW(), NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<MallOrderItemDO> items);
}