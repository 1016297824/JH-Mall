package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.gateway.config.properties.MallAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * C 端认证过滤器（order = -150，拦截 /api/**）
 *
 * @author ruoyi
 */
@Component
public class MallAuthFilter implements GlobalFilter, Ordered
{
    private static final String C_TOKEN_PREFIX = "Bearer ";

    private static final String USER_ID_KEY = "userId";

    private static final String DEFAULT_USER_NAME = "c_end_user";

    @Autowired
    private MallAuthProperties mallAuthProperties;

    @PostConstruct
    public void checkConfig()
    {
        if (StringUtils.isEmpty(mallAuthProperties.getJwtSecret()))
        {
            throw new IllegalStateException("mall.security.jwt-secret 未配置，C 端认证无法启动");
        }
    }

    @Override
    public int getOrder()
    {
        return -150;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        if (isWhites(path))
        {
            return chain.filter(exchange);
        }

        // 2. 只拦截 /api/** 路径
        if (!StringUtils.isMatch("/api/**", path))
        {
            return chain.filter(exchange);
        }

        // 3. 清洗入站头（X-User-*, X-Mall-*）
        request = cleanHeaders(request);

        // 4. 注入 X-Request-Id traceId
        request = injectRequestId(request);

        // 5. 解析 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(C_TOKEN_PREFIX))
        {
            return unauthorized(response, "C 端请求缺少 token");
        }

        String token = authHeader.substring(C_TOKEN_PREFIX.length()).trim();
        Claims claims;
        try
        {
            claims = Jwts.parser()
                    .setSigningKey(mallAuthProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token)
                    .getBody();
        }
        catch (ExpiredJwtException e)
        {
            return unauthorized(response, "C 端 token 已过期");
        }
        catch (SignatureException e)
        {
            return unauthorized(response, "C 端 token 签名无效");
        }
        catch (Exception e)
        {
            return unauthorized(response, "C 端 token 无效");
        }

        // 6. 注入 X-User-Id 头
        String userId = claims.get(USER_ID_KEY, String.class);
        if (StringUtils.isEmpty(userId))
        {
            return unauthorized(response, "C 端 token 缺少 userId");
        }

        request = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", DEFAULT_USER_NAME)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 判断请求路径是否在白名单中
     */
    private boolean isWhites(String path)
    {
        String[] whites = mallAuthProperties.getWhites();
        if (whites == null || whites.length == 0)
        {
            return false;
        }
        for (String white : whites)
        {
            if (StringUtils.isMatch(white, path))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 清洗入站请求头：移除 X-User-* 和 X-Mall-* 前缀的头部
     */
    private ServerHttpRequest cleanHeaders(ServerHttpRequest request)
    {
        List<String> headersToRemove = new ArrayList<>();
        for (String headerName : request.getHeaders().toSingleValueMap().keySet())
        {
            if (headerName.startsWith("X-User-") || headerName.startsWith("X-Mall-"))
            {
                headersToRemove.add(headerName);
            }
        }
        if (headersToRemove.isEmpty())
        {
            return request;
        }
        ServerHttpRequest.Builder builder = request.mutate();
        for (String header : headersToRemove)
        {
            builder.headers(h -> h.remove(header));
        }
        return builder.build();
    }

    /**
     * 注入 X-Request-Id traceId（若不存在）
     */
    private ServerHttpRequest injectRequestId(ServerHttpRequest request)
    {
        if (request.getHeaders().getFirst("X-Request-Id") == null)
        {
            request = request.mutate()
                    .header("X-Request-Id", UUID.randomUUID().toString().replace("-", ""))
                    .build();
        }
        return request;
    }

    /**
     * 返回 401 错误
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String msg)
    {
        return ServletUtils.webFluxResponseWriter(response, msg, HttpStatus.UNAUTHORIZED);
    }
}
