package com.mall.product.service.impl;

import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.constant.CacheConstants;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.mapper.MallSkuStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private MallSkuStockMapper mallSkuStockMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private StockServiceImpl stockService;

    @Test
    void reserveStockShouldSucceed() {
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L); stock.setAvailableStock(100); stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.reserveStock(101L, 1, 1)).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReserveStockItemRequest item = new ReserveStockItemRequest(101L, 1);
        boolean result = stockService.reserveStock("ORD001", List.of(item));

        assertThat(result).isTrue();
    }

    @Test
    void reserveStockShouldThrowWhenInsufficient() {
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L); stock.setAvailableStock(0); stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.reserveStock(101L, 10, 1)).thenReturn(0);

        ReserveStockItemRequest item = new ReserveStockItemRequest(101L, 10);
        assertThatThrownBy(() -> stockService.reserveStock("ORD001", List.of(item)))
                .isInstanceOf(BusinessException.class);
    }

    // ========== restock：以 bizNo 幂等，防止跨服务调用（MQ 重投）重复回补 ==========

    /** 回补幂等键（与 StockServiceImpl 拼法一致） */
    private static final String RESTOCK_KEY = CacheConstants.Product.STOCK_RESTOCK + "AS_001:101";

    @Test
    void restockShouldAddStockOnFirstCall() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(RESTOCK_KEY), eq("5"), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L);
        stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.restock(101L, 5, 1)).thenReturn(1);

        stockService.restock(101L, 5, "AS_001");

        verify(mallSkuStockMapper).restock(101L, 5, 1);
    }

    @Test
    void restockShouldSkipWhenIdempotentKeyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(RESTOCK_KEY), eq("5"), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(false);

        stockService.restock(101L, 5, "AS_001");

        // 幂等命中：不得再回补库存
        verify(mallSkuStockMapper, never()).restock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void restockShouldReleaseKeyWhenDbFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(RESTOCK_KEY), eq("5"), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(null);

        assertThatThrownBy(() -> stockService.restock(101L, 5, "AS_001"))
                .isInstanceOf(BusinessException.class);

        // 回补失败必须释放幂等标记，否则重试会被幂等拦截、库存永不回补
        verify(redisTemplate).delete(RESTOCK_KEY);
    }

    @Test
    void restockShouldThrowAndReleaseKeyWhenVersionConflict() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(RESTOCK_KEY), eq("5"), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L);
        stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        // 乐观锁冲突（并发改动同一行）→ 影响 0 行，属常态而非罕见路径
        when(mallSkuStockMapper.restock(101L, 5, 1)).thenReturn(0);

        // 不校验影响行数会静默"假成功"：幂等键留存 + 日志谎报已回补 → 库存永久少回补
        assertThatThrownBy(() -> stockService.restock(101L, 5, "AS_001"))
                .isInstanceOf(BusinessException.class);

        verify(redisTemplate).delete(RESTOCK_KEY);
    }

    // ========== releaseStock：返回值即「是否全部释放成功」，消费者据此决定是否重投 ==========

    /** 预扣记录 key（与 StockServiceImpl 拼法一致） */
    private static final String RESERVE_KEY = "mall:product:stock:reserve:ORD001:101";

    @Test
    void releaseStockShouldReturnTrueWhenNoReserveRecord() {
        // 游标必须先构造好再 stub：在 thenReturn 参数里嵌套 stub 会触发 UnfinishedStubbingException
        Cursor<String> cursor = emptyCursor();
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        assertThat(stockService.releaseStock("ORD001")).isTrue();
    }

    @Test
    void releaseStockShouldReturnTrueWhenAllReleased() {
        stubSingleReserveKey();
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L);
        stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.releaseStock(101L, 2, 1)).thenReturn(1);

        assertThat(stockService.releaseStock("ORD001")).isTrue();
    }

    @Test
    void releaseStockShouldReturnFalseWhenDbReleaseFails() {
        stubSingleReserveKey();
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L);
        stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        // 乐观锁始终冲突：重试耗尽后必须返回 false，否则调用方会 ACK 而库存永不回补
        when(mallSkuStockMapper.releaseStock(101L, 2, 1)).thenReturn(0);

        assertThat(stockService.releaseStock("ORD001")).isFalse();
    }

    @Test
    void releaseStockShouldThrowWhenScanFails() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenThrow(new RuntimeException("SCAN 不可用"));

        // 不能返回空列表 → 否则被当成「无预扣记录」返回 true，调用方 ACK 后库存永久占用
        assertThatThrownBy(() -> stockService.releaseStock("ORD001"))
                .isInstanceOf(BusinessException.class);
    }

    /** 让 SCAN 命中一条预扣记录，并令其数量为 2 */
    private void stubSingleReserveKey() {
        // 同上：游标先构造，避免在外层 stub 未完成时嵌套 stub
        Cursor<String> cursor = cursorWith(RESERVE_KEY);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RESERVE_KEY)).thenReturn("2");
    }

    /** 构造空游标 */
    @SuppressWarnings("unchecked")
    private Cursor<String> emptyCursor() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        return cursor;
    }

    /** 构造只含一个 key 的游标 */
    @SuppressWarnings("unchecked")
    private Cursor<String> cursorWith(String key) {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key);
        return cursor;
    }
}
