package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.util.ObjectBuilder;
import com.mall.common.DTO.PageResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.infrastructure.feign.RemoteProductAdapter;
import com.mall.search.repository.ProductIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
    private ElasticsearchIndicesClient indicesClient;

    @Mock
    private CreateIndexResponse createIndexResponse;

    @Mock
    private UpdateAliasesResponse updateAliasesResponse;

    @Mock
    private DeleteIndexResponse deleteIndexResponse;

    @Mock
    private RemoteProductAdapter remoteProductAdapter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MallSearchConfigProperties configProperties;

    /** 当前生效索引名（测试夹具） */
    private static final String CURRENT_INDEX = "mall_product_v20261001000000";

    /** 上一版本索引名（测试夹具） */
    private static final String PREVIOUS_INDEX = "mall_product_v20260930000000";

    private IndexServiceImpl indexService;

    @BeforeEach
    void setUp() throws IOException {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        MallSearchConfigProperties.Rebuild rebuildConfig = mock(MallSearchConfigProperties.Rebuild.class);
        when(rebuildConfig.getBatchSize()).thenReturn(500);
        when(rebuildConfig.getTimestampFormat()).thenReturn("yyyyMMddHHmmss");
        MallSearchConfigProperties.Es esConfig = mock(MallSearchConfigProperties.Es.class);
        when(esConfig.getShards()).thenReturn(1);
        when(esConfig.getReplicas()).thenReturn(0);
        when(configProperties.getRebuild()).thenReturn(rebuildConfig);
        when(configProperties.getEs()).thenReturn(esConfig);

        // ES 客户端 mock 链：indices() 未 stub 时返回 null，
        // 会令 rebuildIndex / rollback 在调用 create/getAlias 时抛 NPE 而非预期异常
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.create(any(Function.class))).thenReturn(createIndexResponse);
        when(indicesClient.updateAliases(any(Function.class))).thenReturn(updateAliasesResponse);
        when(indicesClient.delete(any(Function.class))).thenReturn(deleteIndexResponse);

        indexService = new IndexServiceImpl(productIndexRepository, elasticsearchClient,
                remoteProductAdapter, stringRedisTemplate, configProperties);
    }

    @Test
    void syncProduct_delete_shouldCallDeleteById() {
        indexService.syncProduct(1L, "DELETE");
        verify(productIndexRepository).deleteById(1L);
    }

    @Test
    void syncProduct_shouldNotDedupByTimeWindow() {
        // 同一商品连续两次同步都必须执行：ES 的 delete 本身幂等，
        // 而"1 小时内只同步一次"会让第二次变更被静默丢弃（索引停在旧值）
        indexService.syncProduct(1L, "DELETE");
        indexService.syncProduct(1L, "DELETE");

        verify(productIndexRepository, org.mockito.Mockito.times(2)).deleteById(1L);
    }

    @Test
    void rebuildIndex_acquireLock_shouldSucceed() throws IOException {
        when(valueOperations.setIfAbsent(eq("mall:search:index:rebuild_lock"),
                any(), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOperations.get("mall:search:index:rebuild_lock")).thenReturn(null);
        // 首次重建：ES 中尚无任何 mall_product 索引
        GetAliasResponse emptyAlias = aliasResponse();
        when(indicesClient.getAlias(any(Function.class))).thenReturn(emptyAlias);
        // 分页拉取返回空 → 灌数循环立即结束
        when(remoteProductAdapter.fetchAllSpusForSearch(anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 500, 0L, List.of()));

        assertDoesNotThrow(indexService::rebuildIndex);

        // 走通正常路径：创建了新索引并切换别名
        verify(indicesClient).create(any(Function.class));
        verify(indicesClient).updateAliases(any(Function.class));
    }

    @Test
    void rollback_shouldNotThrow() throws IOException {
        // 别名 mall_product 指向当前索引；mall_product_v* 下还能找到上一版本
        GetAliasResponse currentOnly = aliasResponse(CURRENT_INDEX);
        GetAliasResponse withPrevious = aliasResponse(CURRENT_INDEX, PREVIOUS_INDEX);
        when(indicesClient.getAlias(any(Function.class))).thenAnswer(invocation -> {
            String requested = resolveAliasName(invocation.getArgument(0));
            return CURRENT_INDEX.equals(requested) ? currentOnly : withPrevious;
        });

        assertDoesNotThrow(indexService::rollback);

        // 回滚动作 = 切换别名
        verify(indicesClient).updateAliases(any(Function.class));
    }

    @Test
    void rebuildIndex_esRuntimeException_shouldThrowBusinessException() throws IOException {
        when(valueOperations.setIfAbsent(eq("mall:search:index:rebuild_lock"),
                any(), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOperations.get("mall:search:index:rebuild_lock")).thenReturn(null);
        GetAliasResponse emptyAlias = aliasResponse();
        when(indicesClient.getAlias(any(Function.class))).thenReturn(emptyAlias);
        // ES Java Client 连接失败等场景抛的是运行时异常，而非 IOException
        when(indicesClient.create(any(Function.class))).thenThrow(new RuntimeException("ES connection refused"));

        BusinessException ex = catchThrowableOfType(
                () -> indexService.rebuildIndex(), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
    }

    @Test
    void rebuildIndex_remoteBusinessException_shouldPropagate() throws IOException {
        when(valueOperations.setIfAbsent(eq("mall:search:index:rebuild_lock"),
                any(), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOperations.get("mall:search:index:rebuild_lock")).thenReturn(null);
        GetAliasResponse emptyAlias = aliasResponse();
        when(indicesClient.getAlias(any(Function.class))).thenReturn(emptyAlias);
        // 远端商品服务抛业务异常时，应原样透传，不能被兜底捕获改写成 SYSTEM_ERROR
        when(remoteProductAdapter.fetchAllSpusForSearch(anyInt(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        BusinessException ex = catchThrowableOfType(
                () -> indexService.rebuildIndex(), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
    }

    /**
     * 构造 getAlias 响应，键为索引名
     *
     * @param indexNames 响应中要包含的索引名，可为空
     * @return 已 stub {@code aliases()} 的响应对象
     */
    private GetAliasResponse aliasResponse(String... indexNames) {
        Map<String, IndexAliases> aliases = new LinkedHashMap<>();
        for (String indexName : indexNames) {
            aliases.put(indexName, mock(IndexAliases.class));
        }
        GetAliasResponse response = mock(GetAliasResponse.class);
        when(response.aliases()).thenReturn(aliases);
        return response;
    }

    /**
     * 从 getAlias 的 builder 函数参数中解析出被请求的索引/别名表达式
     *
     * @param builderFn getAlias 的 builder 函数参数
     * @return 请求的索引表达式（多个以逗号连接）
     */
    @SuppressWarnings("unchecked")
    private String resolveAliasName(Object builderFn) {
        Function<GetAliasRequest.Builder, ObjectBuilder<GetAliasRequest>> fn =
                (Function<GetAliasRequest.Builder, ObjectBuilder<GetAliasRequest>>) builderFn;
        GetAliasRequest request = fn.apply(new GetAliasRequest.Builder()).build();
        return String.join(",", request.index());
    }
}
