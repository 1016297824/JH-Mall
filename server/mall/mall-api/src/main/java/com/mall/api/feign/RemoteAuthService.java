package com.mall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * C 端认证服务 Feign 接口（MVP 阶段占位，暂未实现）
 *
 * <p>后续用于内部服务间调用认证相关接口</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@FeignClient(contextId = "remoteAuthService", value = "mall-auth")
public interface RemoteAuthService {
}
