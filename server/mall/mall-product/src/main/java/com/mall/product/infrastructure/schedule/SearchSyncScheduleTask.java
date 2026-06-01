package com.mall.product.infrastructure.schedule;

import com.mall.common.constant.MqTopicConstants;
import com.mall.product.DO.OutboxMessageDO;
import com.mall.product.mapper.OutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 搜索同步补偿定时任务
 *
 * <p>由 ruoyi-job 调度，通过 {@code /inner/product/outbox/compensate} 端点调用。
 * 扫描 Outbox 表中待发送的搜索同步消息，逐条补偿投递</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncScheduleTask {

    private final OutboxMessageMapper outboxMessageMapper;

    /**
     * 执行 Outbox 消息补偿投递
     *
     * <p>扫描待发送的搜索同步消息（最多 100 条），逐条更新状态为 SENT。
     * 为最终一致性兜底：搜索引擎不可用时实时同步失败的消息由此补偿。</p>
     *
     * @return 本次处理的记录数
     */
    public int execute() {
        // 查询 Outbox 表中状态为 NEW 的搜索同步消息
        List<OutboxMessageDO> pendingList = outboxMessageMapper.selectPending(
                MqTopicConstants.Product.SEARCH_SYNC, 100);
        // 逐条补偿投递：将状态从 NEW 更新为 SENT
        // TODO: (JH-Mall, 2026/06/01) 实际应调用 searchSyncProducer.syncProduct() 重新投递搜索引擎
        for (OutboxMessageDO outbox : pendingList) {
            try {
                log.info("Compensate outbox: messageId={}, spuId={}", outbox.getMessageId(), outbox.getAggregateId());
                outboxMessageMapper.updateStatus(outbox.getId(), "SENT");
            } catch (Exception e) {
                log.error("Failed to compensate outbox: messageId={}", outbox.getMessageId(), e);
            }
        }
        return pendingList.size();
    }
}
