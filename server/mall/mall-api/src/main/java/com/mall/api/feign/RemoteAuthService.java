package com.mall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(contextId = "remoteAuthService", value = "mall-auth")
public interface RemoteAuthService {
    // MVP 阶段不实现
}
