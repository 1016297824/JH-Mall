package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @date 2026/05/22
 */
@Component
public class MallAuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(MallAuthFilter.class);

    /** C 端 JWT 前缀 */
    private static final String C_TOKEN_PREFIX = "Bearer ";

    /** JWT 负载中的用户 ID 键名 */
    private static final String USER_ID_KEY = "userId";

    /** C 端匿名默认用户名 */
    private static final String DEFAULT_USER_NAME = "c_end_user";

    /** 网关透传 Header 名称 */
    private static final String HEADER_X_USER_ID = "X-User-Id";
    private static final String HEADER_X_USER_NAME = "X-User-Name";
    private static final String HEADER_X_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_X_USER_PREFIX = "X-User-";
    private static final String HEADER_X_MALL_PREFIX = "X-Mall-";

    @Autowired
    private MallAuthProperties mallAuthProperties;

    @Autowired
    private RedisService redisService;

    /** C 端 Redis Key 前缀——与 mall-common CacheConstants.Auth 保持一致 */
    private static final String REDIS_BLACKLIST_PREFIX = "mall:auth:blacklist:";
    /** C 端 token_version Redis Key 前缀 — 与 mall-common CacheConstants.Auth.USER_VERSION 一致 */
    private static final String USER_VERSION_PREFIX = "mall:auth:user_version:";

    /**
     * 启动时校验 C 端 JWT 密钥是否已配置
     */
    @PostConstruct
    public void checkConfig()
    {
        if (StringUtils.isEmpty(mallAuthProperties.getJwtSecret()))
        {
            throw new IllegalStateException("mall.security.jwt-secret 未配置，C 端认证无法启动");
        }
    }

    /**
     * 过滤器优先级，-150 介于 AuthFilter(-200) 和 Sentinel 之间
     */
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

        // 1. 只处理 /api/** 路径（非 C 端路径由 AuthFilter 管理）
        if (!StringUtils.isMatch("/api/**", path))
        {
            return chain.filter(exchange);
        }

        // 2. 清洗入站头（X-User-*, X-Mall-*），所有 /api/** 路径统一处理
        request = cleanHeaders(request);

        // 3. 注入 X-Request-Id traceId
        request = injectRequestId(request);

        // 4. 匿名路径直接放行
        if (isAnonymousPath(path))
        {
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 5. 解析 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(C_TOKEN_PREFIX))
        {
            log.warn("[C 端鉴权失败] 请求路径:{} 原因:缺少 token", path);
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
            log.warn("[C 端鉴权失败] 请求路径:{} 原因:token 已过期", path);
            return unauthorized(response, "C 端 token 已过期");
        }
        catch (SignatureException e)
        {
            log.warn("[C 端鉴权失败] 请求路径:{} 原因:签名无效", path);
            return unauthorized(response, "C 端 token 签名无效");
        }
        catch (Exception e)
        {
            log.warn("[C 端鉴权失败] 请求路径:{} 原因:token 解析异常", path);
            return unauthorized(response, "C 端 token 无效");
        }

        // 6. 检查 Token 黑名单与会话有效性
        String jti = claims.getId();
        String userId = claims.get(USER_ID_KEY, String.class);
        if (StringUtils.isEmpty(userId))
        {
            log.warn("[C 端鉴权失败] 请求路径:{} 原因:token 中缺少 userId", path);
            return unauthorized(response, "C 端 token 缺少 userId");
        }

        String blacklistKey = REDIS_BLACKLIST_PREFIX + jti;
        if (Boolean.TRUE.equals(redisService.hasKey(blacklistKey)))
        {
            log.warn("[C 端鉴权失败] 请求路径:{} jti:{} 原因:token 已被吊销（黑名单命中）", path, jti);
            return unauthorized(response, "C 端 token 已被吊销");
        }

        // token_version 校验（cache miss 直接放行，由下游 verify() 兜底回源 DB）
        Integer jwtVersion = claims.get("ver", Integer.class);
        String versionKey = USER_VERSION_PREFIX + userId;
        Object cachedVersion = redisService.getCacheObject(versionKey);
        if (jwtVersion != null && cachedVersion != null) {
            try {
                int currentVer = Integer.parseInt(cachedVersion.toString());
                if (jwtVersion != currentVer) {
                    log.warn("[C 端鉴权失败] 请求路径:{} userId:{} jwtVer:{} curVer:{} 原因:token_version 不匹配",
                            path, userId, jwtVersion, currentVer);
                    return unauthorized(response, "C 端 token 已失效");
                }
            } catch (NumberFormatException e) {
                log.error("[C 端鉴权] 缓存 version 转换失败, userId={}, cachedVersion={}", userId, cachedVersion, e);
            }
        }
        if (cachedVersion == null) {
            log.info("[C 端鉴权] 缓存未命中, userId={}, 放行下游兜底", userId);
        }

        // 7. 注入 X-User-Id 头
        request = request.mutate()
                .header(HEADER_X_USER_ID, userId)
                .header(HEADER_X_USER_NAME, DEFAULT_USER_NAME)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 判断请求路径是否在匿名路径中
     */
    private boolean isAnonymousPath(String path)
    {
        String[] paths = mallAuthProperties.getAnonymousPaths();
        if (paths == null || paths.length == 0)
        {
            return false;
        }
        for (String p : paths)
        {
            if (StringUtils.isMatch(p, path))
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
            if (headerName.startsWith(HEADER_X_USER_PREFIX) || headerName.startsWith(HEADER_X_MALL_PREFIX))
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
        if (request.getHeaders().getFirst(HEADER_X_REQUEST_ID) == null)
        {
            request = request.mutate()
                    .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString().replace("-", ""))
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
