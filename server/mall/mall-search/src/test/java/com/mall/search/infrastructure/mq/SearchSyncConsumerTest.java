package com.mall.search.infrastructure.mq;

import com.mall.common.mq.MqDedupGuard;
import com.mall.search.service.IndexService;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SearchSyncConsumer 单元测试
 *
 * <p>搜索索引同步<b>没有实时通道</b>：{@code RemoteSearchService.syncProduct} 声明的
 * {@code POST /inner/search/product/sync} 在 mall-search 侧从未实现（该模块只有
 * {@code /inner/search/index/rebuild}），因此 mall-product 每次实时调用都 404，
 * 一律降级到 Outbox → 补偿任务 → MQ。也就是说<b>本消费者是索引同步的唯一入口</b>，
 * 它丢一条消息就是索引永久错一条。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class SearchSyncConsumerTest {

    private static final String MSG_ID = "MSG_20261003000011";

    private static final String CONSUMER_GROUP = "mall-search-sync";

    @Mock
    private IndexService indexService;

    @Mock
    private MqDedupGuard dedupGuard;

    @InjectMocks
    private SearchSyncConsumer consumer;

    /** 构造带 msgId 的消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setMsgId(MSG_ID);
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }

    @Test
    @DisplayName("正常消息：带上生产端时间戳调用同步")
    void shouldSyncWithTimestamp() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(true);

        consumer.onMessage(message("{\"spuId\":101,\"operation\":\"UPSERT\",\"timestamp\":1791010000000}"));

        verify(indexService).syncProduct(eq(101L), eq("UPSERT"), eq(1791010000000L));
    }

    @Test
    @DisplayName("缺少 timestamp：按 0 处理，由同步层自行决定是否接受")
    void shouldDefaultTimestampToZero() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(true);

        consumer.onMessage(message("{\"spuId\":101,\"operation\":\"DELETE\"}"));

        verify(indexService).syncProduct(eq(101L), eq("DELETE"), eq(0L));
    }

    @Test
    @DisplayName("重复投递：直接跳过，不得重复写索引")
    void shouldSkipOnDuplicateDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(false);

        consumer.onMessage(message("{\"spuId\":101,\"operation\":\"UPSERT\",\"timestamp\":1}"));

        verifyNoInteractions(indexService);
    }

    @Test
    @DisplayName("消息体无法解析：上抛交由 MQ 重投，且不占用去重标记")
    void shouldRethrowOnUnparsableBody() {
        assertThatThrownBy(() -> consumer.onMessage(message("not-a-json")))
                .isInstanceOf(IllegalStateException.class);

        // 校验先于去重，解析失败不该占位
        verifyNoInteractions(dedupGuard);
    }

    @Test
    @DisplayName("缺少 spuId：属数据缺陷，留痕丢弃且不占用重投机会")
    void shouldDropMessageWithoutSpuId() {
        consumer.onMessage(message("{\"operation\":\"UPSERT\",\"timestamp\":1}"));

        verifyNoInteractions(indexService);
        verifyNoInteractions(dedupGuard);
    }

    @Test
    @DisplayName("同步失败：释放去重标记并上抛，交由 MQ 重投")
    void shouldReleaseDedupAndRethrowOnFailure() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(true);
        doThrow(new IllegalStateException("ES 不可用"))
                .when(indexService).syncProduct(anyLong(), anyString(), anyLong());

        assertThatThrownBy(() -> consumer.onMessage(
                message("{\"spuId\":101,\"operation\":\"UPSERT\",\"timestamp\":1}")))
                .isInstanceOf(IllegalStateException.class);

        verify(dedupGuard).release(MSG_ID, CONSUMER_GROUP);
    }
}
