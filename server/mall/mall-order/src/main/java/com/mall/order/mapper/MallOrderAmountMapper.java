package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallOrderAmountDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单金额快照 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallOrderAmountMapper extends BaseMapper<MallOrderAmountDO> {

    /**
     * 按订单 ID 查询金额快照
     *
     * @param orderId 订单 ID
     * @return 金额快照，不存在返回 null
     */
    default MallOrderAmountDO selectByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapper<MallOrderAmountDO>()
                .eq(MallOrderAmountDO::getOrderId, orderId)
                .eq(MallOrderAmountDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }
}