package com.mall.search.infrastructure.mq;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.mq.MqDedupGuard;
import com.mall.search.service.IndexService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 搜索索引增量同步消费者
 *
 * <p>消费 {@code mall_search_sync} 事件并写入 ES 索引。</p>
 *
 * <p><b>本消费者是索引同步的唯一入口</b>：契约 {@code RemoteSearchService.syncProduct}
 * 声明的 {@code POST /inner/search/product/sync} 在 mall-search 侧从未实现（本模块只有
 * {@code /inner/search/index/rebuild}），mall-product 每次实时调用都 404，
 * 于是<b>所有</b>商品变更都走 Outbox → 补偿任务 → 本消费者。丢一条消息就是索引永久错一条。</p>
 *
 * <p>据此三条约束必须成立：</p>
 * <ol>
 *   <li><b>失败必须上抛</b>：原先解析失败只打日志后 return，消息被静默丢弃且不会重投</li>
 *   <li><b>按 msgId 去重</b>：原先用的是 {@code RocketMQListener<String>}，根本拿不到 msgId</li>
 *   <li><b>乱序保护</b>：去重只挡「同一条消息重投」，挡不住「更早的消息后到」——
 *       一条陈旧的 UPSERT 落在 DELETE 之后会让已删除的商品在索引里复活，
 *       故把生产端时间戳一并交给同步层做「后写覆盖先写」</li>
 * </ol>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopicConstants.Product.SEARCH_SYNC,
    consumerGroup = "mall-search-sync-consumer"
)
public class SearchSyncConsumer implements RocketMQListener<MessageExt> {

    /** JSON 解析器（无状态、线程安全，按项目约定静态复用） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 去重用的消费组标识，需为稳定常量 */
    private static final String CONSUMER_GROUP = "mall-search-sync";

    /** 缺省操作类型：生产端只发 UPSERT / DELETE，缺失时按 UPSERT 处理 */
    private static final String DEFAULT_OPERATION = "UPSERT";

    private final IndexService indexService;

    private final MqDedupGuard dedupGuard;

    /**
     * 消费搜索同步消息
     *
     * @param message MQ 消息（用 MessageExt 而非 String，以便取到 broker 分配的 msgId 做去重）
     */
    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String msgId = message.getMsgId();

        JsonNode json;
        try {
            json = OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            // 解析失败说明投递内容损坏，交由 MQ 重投；此时尚未占位，无需释放
            log.error("搜索同步消息解析失败: msgId={}, body={}", msgId, body, e);
            throw new IllegalStateException("搜索同步消息解析失败", e);
        }

        if (!json.hasNonNull("spuId")) {
            // 数据缺陷：重投也治不好，留痕后丢弃
            log.error("搜索同步消息缺少 spuId，丢弃: msgId={}, body={}", msgId, body);
            return;
        }
        Long spuId = json.get("spuId").asLong();
        String operation = json.hasNonNull("operation") ? json.get("operation").asText() : DEFAULT_OPERATION;
        long timestamp = json.hasNonNull("timestamp") ? json.get("timestamp").asLong() : 0L;

        if (!dedupGuard.tryDedup(msgId, CONSUMER_GROUP)) {
            return;
        }

        try {
            indexService.syncProduct(spuId, operation, timestamp);
        } catch (RuntimeException e) {
            log.error("搜索同步失败，交由 MQ 重试: msgId={}, spuId={}, operation={}",
                    msgId, spuId, operation, e);
            try {
                dedupGuard.release(msgId, CONSUMER_GROUP);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截: msgId={}", msgId, releaseError);
            }
            throw e;
        }
    }
}
