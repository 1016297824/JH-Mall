package com.mall.order.infrastructure.feign;

import com.mall.api.feign.RemoteUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-user Feign 适配器
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteUserAdapter {

    private final RemoteUserService remoteUserService;

    /**
     * 校验收货地址归属
     *
     * <p>调用失败时抛异常而非返回 false——地址校验失败必须阻断下单，
     * 不能「失败即通过」，否则可绕过归属校验下单到他人地址。</p>
     *
     * @param userId    用户 ID
     * @param addressId 收货地址 ID
     * @return 地址属于该用户返回 true
     */
    public boolean validateAddress(Long userId, Long addressId) {
        boolean valid = remoteUserService.validateAddress(String.valueOf(userId), addressId);
        log.info("validateAddress userId={}, addressId={}, valid={}", userId, addressId, valid);
        return valid;
    }
}