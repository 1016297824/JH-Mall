package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallOutboxDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallOutboxMapper extends BaseMapper<MallOutboxDO> {

    /** 待投递状态，字符串字面量常量用于 SQL 拼接 */
    String STATUS_NEW = "NEW";

    /** 已发送状态 */
    String STATUS_SENT = "SENT";

    /** 已取消状态（支付成功后取消支付超时延迟消息） */
    String STATUS_CANCELLED = "CANCELLED";

    /**
     * 查询待投递消息
     *
     * <p>投递条件：状态为 NEW，且预约时间已到（含 null 表示立即投递）。</p>
     *
     * @param limit 单批拉取条数，控制单轮投递量
     * @return 待投递消息列表
     */
    default List<MallOutboxDO> selectPending(int limit) {
        return selectList(new LambdaQueryWrapper<MallOutboxDO>()
                .eq(MallOutboxDO::getStatus, STATUS_NEW)
                .and(w -> w.isNull(MallOutboxDO::getScheduledTime)
                        .or().le(MallOutboxDO::getScheduledTime, LocalDateTime.now()))
                .orderByAsc(MallOutboxDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 标记投递成功
     *
     * @param id       Outbox 主键
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = '" + STATUS_SENT + "', update_time = NOW() WHERE id = #{id}")
    int markSent(@Param("id") Long id);

    /**
     * 标记投递失败并安排下次重试
     *
     * <p>指数退避 10s / 30s / 60s + 抖动（设计文档 §7.2），
     * 超过 3 次置 FAILED 进死信，由人工介入。</p>
     *
     * @param id            Outbox 主键
     * @param retryCount    已重试次数
     * @param nextRetryTime 下次重试时间
     * @param finalFailure  是否已达重试上限（true=置FAILED）
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = #{status}, retry_count = #{retryCount}, " +
            "next_retry_time = #{nextRetryTime}, update_time = NOW() WHERE id = #{id}")
    int markRetry(@Param("id") Long id,
                  @Param("status") String status,
                  @Param("retryCount") Integer retryCount,
                  @Param("nextRetryTime") LocalDateTime nextRetryTime);

    /**
     * 取消某聚合下待投递的指定主题消息
     *
     * <p>支付成功时同步取消尚未投递的支付超时延迟消息（设计文档 §5.9），
     * 避免已支付订单被 MQ 延迟消息误关单。</p>
     *
     * @param aggregateType 聚合类型
     * @param aggregateId   聚合 ID（订单号）
     * @param topic         消息主题
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = '" + STATUS_CANCELLED + "', update_time = NOW() " +
            "WHERE aggregate_type = #{aggregateType} AND aggregate_id = #{aggregateId} " +
            "AND topic = #{topic} AND status = '" + STATUS_NEW + "'")
    int cancelPending(@Param("aggregateType") String aggregateType,
                      @Param("aggregateId") String aggregateId,
                      @Param("topic") String topic);

    /**
     * 按消息全局 ID 查询（消费端幂等去重用）
     *
     * @param messageId 消息 ID
     * @return 消息，不存在返回 null
     */
    default MallOutboxDO selectByMessageId(String messageId) {
        return selectOne(new LambdaQueryWrapper<MallOutboxDO>()
                .eq(MallOutboxDO::getMessageId, messageId)
                .last("LIMIT 1"));
    }
}