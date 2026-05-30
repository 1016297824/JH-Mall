package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.OutboxMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Outbox 消息 Mapper（可靠投递）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessageDO> {

    /**
     * 查询待发送的消息
     *
     * @param topic 消息主题
     * @param limit 获取条数
     * @return 待发送消息列表
     */
    default List<OutboxMessageDO> selectPending(String topic, int limit) {
        return selectList(new LambdaQueryWrapper<OutboxMessageDO>()
                .eq(OutboxMessageDO::getTopic, topic)
                .eq(OutboxMessageDO::getStatus, "NEW")
                .orderByAsc(OutboxMessageDO::getNextRetryTime)
                .last("LIMIT " + limit));
    }

    /**
     * 更新消息状态
     *
     * @param id     消息 ID
     * @param status 目标状态
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = #{status}, retry_count = retry_count + 1 WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
