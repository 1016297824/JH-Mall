package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.infrastructure.feign.RemoteProductAdapter;
import com.mall.search.repository.ProductIndexRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IndexServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndexServiceImplTest {

    @Mock
    private ProductIndexRepository productIndexRepository;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private RemoteProductAdapter remoteProductAdapter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MallSearchConfigProperties configProperties;

    private IndexServiceImpl indexService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        MallSearchConfigProperties.Rebuild rebuildConfig = mock(MallSearchConfigProperties.Rebuild.class);
        when(rebuildConfig.getBatchSize()).thenReturn(500);
        when(rebuildConfig.getTimestampFormat()).thenReturn("yyyyMMddHHmmss");
        MallSearchConfigProperties.Es esConfig = mock(MallSearchConfigProperties.Es.class);
        when(esConfig.getShards()).thenReturn(1);
        when(esConfig.getReplicas()).thenReturn(0);
        when(configProperties.getRebuild()).thenReturn(rebuildConfig);
        when(configProperties.getEs()).thenReturn(esConfig);
        indexService = new IndexServiceImpl(productIndexRepository, elasticsearchClient,
                remoteProductAdapter, stringRedisTemplate, configProperties);
    }

    @Test
    void syncProduct_delete_shouldCallDeleteById() {
        // RED/GREEN: 幂等去重未命中时执行删除
        when(valueOperations.setIfAbsent(contains("dedup"), eq("1"), eq(1L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        indexService.syncProduct(1L, "DELETE");
        verify(productIndexRepository).deleteById(1L);
    }

    @Test
    void syncProduct_duplicate_shouldSkip() {
        // 幂等去重命中，跳过
        when(valueOperations.setIfAbsent(contains("dedup"), eq("1"), eq(1L), eq(TimeUnit.HOURS)))
                .thenReturn(false);
        indexService.syncProduct(1L, "UPSERT");
        verify(productIndexRepository, never()).deleteById(any());
        verify(productIndexRepository, never()).save(any());
    }

    @Test
    void rebuildIndex_acquireLock_shouldSucceed() {
        when(valueOperations.setIfAbsent(eq("mall:search:index:rebuild_lock"),
                any(), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOperations.get("mall:search:index:rebuild_lock")).thenReturn(null);
        assertDoesNotThrow(indexService::rebuildIndex);
    }

    @Test
    void rollback_shouldNotThrow() {
        assertDoesNotThrow(indexService::rollback);
    }
}
