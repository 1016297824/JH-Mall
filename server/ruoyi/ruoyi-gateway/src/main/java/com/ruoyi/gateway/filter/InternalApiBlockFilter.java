package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.ServletUtils;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 内部接口防护过滤器（order = -300，先于所有鉴权过滤器执行）
 *
 * <p>网关开启了服务发现自动路由（{@code discovery.locator}），会为每个 Nacos 服务生成
 * {@code /{serviceId}/**} 路由，使服务间内部接口（{@code /inner/**}）可被外部经
 * {@code /mall-user/inner/**} 这类路径访问。内部接口按设计无鉴权、仅限服务间内网调用，
 * 因此在此处直接拒绝，避免密码哈希等敏感数据经网关外泄。</p>
 *
 * <p>服务间 Feign 调用直连服务端口（如 mall-user:9302），不经过网关，不受本过滤器影响。</p>
 *
 * @author JH-Mall
 * @date 2026/09/27
 */
@Component
public class InternalApiBlockFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(InternalApiBlockFilter.class);

    /** 内部接口路径段 */
    private static final String BLOCKED_SEGMENT = "inner";

    /** 矩阵参数模式（形如 {@code ;x=1}）：下游 Tomcat 会剥离，判定前必须先去除 */
    private static final Pattern MATRIX_PARAM_PATTERN = Pattern.compile(";.*?(?=/|$)");

    /**
     * 过滤器优先级，-300 高于 AuthFilter(-200) 与 MallAuthFilter(-150)
     */
    @Override
    public int getOrder()
    {
        return -300;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        String path = exchange.getRequest().getURI().getPath();
        if (isInternalPath(path))
        {
            log.warn("[内部接口拦截] 拒绝外部访问内部接口, path={}", path);
            ServerHttpResponse response = exchange.getResponse();
            return ServletUtils.webFluxResponseWriter(response, "接口不存在", HttpStatus.NOT_FOUND);
        }
        return chain.filter(exchange);
    }

    /**
     * 判断路径是否指向内部接口
     *
     * <p>先剥离矩阵参数（形如 {@code ;x=1}）并折叠重复斜杠，再按路径段精确匹配。
     * 不能只用 Ant 通配：{@code /mall-user/inner;x=1/...} 中 {@code inner} 不是完整路径段，
     * 通配匹配不到，而下游 Tomcat 会剥离矩阵参数，请求仍能到达内部接口。</p>
     *
     * @param path 请求路径
     * @return 是否内部接口
     */
    private boolean isInternalPath(String path)
    {
        String normalized = MATRIX_PARAM_PATTERN.matcher(path).replaceAll("").replaceAll("/{2,}", "/");
        for (String segment : normalized.split("/"))
        {
            if (BLOCKED_SEGMENT.equals(segment))
            {
                return true;
            }
        }
        return false;
    }
}
