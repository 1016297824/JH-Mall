package com.mall.marketing.infrastructure.outbox;

import com.mall.common.enums.OutboxStatusEnum;
import com.mall.marketing.DO.MallOutboxDO;
import com.mall.marketing.mapper.MallOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Outbox 消息投递调度器
 *
 * <p>扫描 {@code mall_outbox} 中到期待投递的消息，投递到 RocketMQ。
 * 投递失败按指数退避重试，超过上限置 FAILED 等待人工介入
 * （设计文档 §7.3：10s → 30s → 60s，各加 0~5s 随机抖动，上限 3 次）。</p>
 *
 * <p><b>注意</b>：本调度器与业务事务分离，消息可能重复投递，
 * 消费方必须做幂等去重（设计文档 §7.4）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    /** 单轮扫描条数 */
    private static final int BATCH_SIZE = 100;

    /** 最大重试次数，达到即置 FAILED */
    private static final int MAX_RETRY = 3;

    /** 退避间隔（秒），按重试次数取下标 */
    private static final long[] BACKOFF_SECONDS = {10L, 30L, 60L};

    /** 随机抖动上限（毫秒），避免重试风暴同时打峰 */
    private static final long JITTER_MAX_MS = 5000L;

    private final MallOutboxMapper outboxMapper;

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 扫描并投递待发送消息
     *
     * <p>固定延迟 1 秒，配合退避粒度（最短 10s）足够。</p>
     */
    @Scheduled(fixedDelay = 1000L)
    public void dispatch() {
        List<MallOutboxDO> pendingList;
        try {
            pendingList = outboxMapper.selectPending(BATCH_SIZE);
        } catch (Exception e) {
            log.error("Outbox 扫描失败", e);
            return;
        }
        if (pendingList.isEmpty()) {
            return;
        }

        for (MallOutboxDO message : pendingList) {
            try {
                doDispatch(message);
            } catch (Exception e) {
                log.error("Outbox 投递异常, messageId={}, topic={}",
                        message.getMessageId(), message.getTopic(), e);
                handleFailure(message);
            }
        }
    }

    /**
     * 执行单条投递
     *
     * @param message 待投递消息
     */
    private void doDispatch(MallOutboxDO message) {
        Message<String> payload = MessageBuilder.withPayload(message.getPayload())
                .setHeader("KEYS", message.getMessageId())
                .build();
        rocketMQTemplate.syncSend(message.getTopic(), payload);
        outboxMapper.markSent(message.getId());
        log.info("Outbox 已投递: topic={}, messageId={}, aggregateId={}",
                message.getTopic(), message.getMessageId(), message.getAggregateId());
    }

    /**
     * 失败处理：未达上限则安排下次重试，达上限置 FAILED
     *
     * @param message 投递失败的消息
     */
    private void handleFailure(MallOutboxDO message) {
        int retryCount = message.getRetryCount() == null ? 1 : message.getRetryCount() + 1;
        if (retryCount >= MAX_RETRY) {
            outboxMapper.markRetry(message.getId(),
                    OutboxStatusEnum.FAILED.getCode(), retryCount, null);
            log.error("Outbox 达重试上限，置 FAILED 待人工介入: topic={}, messageId={}, retry={}",
                    message.getTopic(), message.getMessageId(), retryCount);
            return;
        }
        long backoffSeconds = BACKOFF_SECONDS[Math.min(retryCount - 1, BACKOFF_SECONDS.length - 1)];
        long jitterMs = ThreadLocalRandom.current().nextLong(JITTER_MAX_MS);
        LocalDateTime next = LocalDateTime.now()
                .plusSeconds(backoffSeconds)
                .plusNanos(jitterMs * 1_000_000L);
        outboxMapper.markRetry(message.getId(),
                OutboxStatusEnum.NEW.getCode(), retryCount, next);
        log.warn("Outbox 投递失败已安排重试: topic={}, messageId={}, retry={}, next={}",
                message.getTopic(), message.getMessageId(), retryCount, next);
    }
}
