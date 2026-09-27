package com.ruoyi.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.core.constant.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * InternalApiBlockFilter 单元测试
 *
 * @author JH-Mall
 * @date 2026/09/27
 */
class InternalApiBlockFilterTest
{
    private final InternalApiBlockFilter filter = new InternalApiBlockFilter();

    /**
     * 构造指定路径的交换对象
     *
     * @param path 请求路径
     * @return Mock 交换对象
     */
    private MockServerWebExchange exchangeOf(String path)
    {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }

    /**
     * 构造放行用的过滤器链
     *
     * @return Mock 过滤器链
     */
    private GatewayFilterChain passThroughChain()
    {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }

    @Test
    void shouldBlockInnerApiBehindServiceNameRoute()
    {
        MockServerWebExchange exchange = exchangeOf("/mall-user/inner/user/8/credential");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertBlocked(exchange);
    }

    @Test
    void shouldBlockBareInnerApi()
    {
        MockServerWebExchange exchange = exchangeOf("/inner/user/register");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertBlocked(exchange);
    }

    /**
     * 断言请求已被拒绝：响应体含 404 业务码
     *
     * <p>与网关既有过滤器一致，{@code ServletUtils.webFluxResponseWriter} 以 HTTP 200
     * 承载业务码，因此这里校验响应体而非 HTTP 状态。</p>
     *
     * @param exchange 交换对象
     */
    private void assertBlocked(MockServerWebExchange exchange)
    {
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body, "应写出响应体");
        assertTrue(body.contains(String.valueOf(HttpStatus.NOT_FOUND)), "响应体应含 404 业务码");
    }

    @Test
    void shouldBlockInnerApiWithMatrixParameter()
    {
        // 下游 Tomcat 会剥离 ;x=1，若只做 Ant 通配匹配则此路径会绕过
        MockServerWebExchange exchange = exchangeOf("/mall-user/inner;x=1/user/8/credential");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertBlocked(exchange);
    }

    @Test
    void shouldBlockInnerApiWithDoubleSlash()
    {
        MockServerWebExchange exchange = exchangeOf("/mall-user//inner//user/8/credential");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertBlocked(exchange);
    }

    @Test
    void shouldPassThroughCApiPath()
    {
        MockServerWebExchange exchange = exchangeOf("/api/user/profile");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        assertNull(exchange.getResponse().getStatusCode());
        verify(chain).filter(exchange);
    }

    @Test
    void shouldPassThroughServiceNameRouteThatIsNotInner()
    {
        MockServerWebExchange exchange = exchangeOf("/mall-user/other");
        GatewayFilterChain chain = passThroughChain();

        filter.filter(exchange, chain).block();

        assertNull(exchange.getResponse().getStatusCode());
        verify(chain).filter(exchange);
    }
}
