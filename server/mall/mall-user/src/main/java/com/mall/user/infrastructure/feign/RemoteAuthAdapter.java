package com.mall.user.infrastructure.feign;

import org.springframework.stereotype.Component;

/**
 * 远程认证适配器，封装对 mall-auth 的手机号解密和脱敏调用
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
public class RemoteAuthAdapter {

    /**
     * 解密手机号（预留 Feign 调用，当前直接返回原值）
     *
     * @param encryptedPhone 加密手机号
     * @return 解密后的手机号明文
     */
    public String decryptPhone(String encryptedPhone) {
        return encryptedPhone;
    }

    /**
     * 手机号脱敏，中间4位替换为****
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
