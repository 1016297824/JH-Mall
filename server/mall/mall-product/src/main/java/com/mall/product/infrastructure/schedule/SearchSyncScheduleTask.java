package com.mall.product.infrastructure.schedule;

import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.OutboxStatusEnum;
import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.DO.OutboxMessageDO;
import com.mall.product.infrastructure.mq.SearchSyncProducer;
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

    /** 单轮扫描条数上限 */
    private static final int BATCH_SIZE = 100;

    private final OutboxMessageMapper outboxMessageMapper;

    private final SearchSyncProducer searchSyncProducer;

    /**
     * 执行 Outbox 消息补偿投递
     *
     * <p>扫描待发送的搜索同步消息，<b>真正重新投递给搜索引擎</b>：
     * 投递成功才置 {@code SENT}；失败保持 {@code NEW} 等下一轮重试。</p>
     *
     * <p><b>为何不能只改状态</b>：本任务的存在意义就是「实时同步失败后的兜底」。
     * 若只把状态改成 SENT 而不投递，消息会被永久标记为已发送，
     * 而索引实际从未更新 —— 属于会静默丢数据的假补偿。</p>
     *
     * @return 本次<b>成功补偿</b>的记录数（跳过与失败的都不计入）
     */
    public int execute() {
        // 查询 Outbox 表中状态为 NEW 的搜索同步消息
        List<OutboxMessageDO> pendingList = outboxMessageMapper.selectPending(
                MqTopicConstants.Product.SEARCH_SYNC, BATCH_SIZE);

        int compensated = 0;
        for (OutboxMessageDO outbox : pendingList) {
            try {
                SyncOperationEnum operation = resolveOperation(outbox.getEventType());
                if (operation == null) {
                    // 操作码非法属投递方缺陷，重投也不会好：置 FAILED 留痕待人工排查
                    log.error("搜索同步 Outbox 操作码非法，置 FAILED: messageId={}, eventType={}",
                            outbox.getMessageId(), outbox.getEventType());
                    outboxMessageMapper.updateStatus(outbox.getId(), OutboxStatusEnum.FAILED.getCode());
                    continue;
                }
                if (searchSyncProducer.resync(Long.valueOf(outbox.getAggregateId()), operation)) {
                    outboxMessageMapper.updateStatus(outbox.getId(), OutboxStatusEnum.SENT.getCode());
                    compensated++;
                }
                // 投递失败：保持 NEW，下轮重试
            } catch (Exception e) {
                // 单条异常不中断整批，且不置 SENT
                log.error("搜索同步补偿异常，保留待下轮重试: messageId={}", outbox.getMessageId(), e);
            }
        }
        log.info("搜索同步补偿完成: 待补偿={}, 成功={}", pendingList.size(), compensated);
        return compensated;
    }

    /**
     * 按操作码解析同步操作枚举
     *
     * @param code 操作码字符串
     * @return 匹配的枚举，无法识别返回 {@code null}
     */
    private static SyncOperationEnum resolveOperation(String code) {
        if (code == null) {
            return null;
        }
        for (SyncOperationEnum operation : SyncOperationEnum.values()) {
            if (operation.getCode().equals(code)) {
                return operation;
            }
        }
        return null;
    }
}
