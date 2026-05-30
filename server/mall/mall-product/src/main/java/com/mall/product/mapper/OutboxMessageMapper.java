package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.OutboxMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessageDO> {

    default List<OutboxMessageDO> selectPending(String topic, int limit) {
        return selectList(new LambdaQueryWrapper<OutboxMessageDO>()
                .eq(OutboxMessageDO::getTopic, topic)
                .eq(OutboxMessageDO::getStatus, "NEW")
                .orderByAsc(OutboxMessageDO::getNextRetryTime)
                .last("LIMIT " + limit));
    }

    @Update("UPDATE mall_outbox SET status = #{status}, retry_count = retry_count + 1 WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
