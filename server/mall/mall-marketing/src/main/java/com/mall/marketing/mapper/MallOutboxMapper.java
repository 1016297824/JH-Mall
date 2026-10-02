package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.DO.MallOutboxDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息 Mapper
 *
 * <p>与 mall-order 共用 {@code mall_outbox} 表，但只操作本模块投递的 topic，
 * 互不感知对方数据。</p>
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
     * @param id Outbox 主键
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = '" + STATUS_SENT + "', update_time = NOW() WHERE id = #{id}")
    int markSent(@Param("id") Long id);

    /**
     * 标记投递失败并安排下次重试
     *
     * <p>指数退避 10s / 30s / 60s + 抖动（设计文档 §7.3），
     * 超过 3 次置 FAILED 进死信，由人工介入。</p>
     *
     * @param id            Outbox 主键
     * @param status        目标状态（PENDING 或 FAILED）
     * @param retryCount    已重试次数
     * @param nextRetryTime 下次重试时间
     * @return 影响行数
     */
    @Update("UPDATE mall_outbox SET status = #{status}, retry_count = #{retryCount}, "
            + "next_retry_time = #{nextRetryTime}, update_time = NOW() WHERE id = #{id}")
    int markRetry(@Param("id") Long id,
                  @Param("status") String status,
                  @Param("retryCount") Integer retryCount,
                  @Param("nextRetryTime") LocalDateTime nextRetryTime);

    /**
     * 按消息全局 ID 查询
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
