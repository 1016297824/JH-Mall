package com.mall.auth.infrastructure.feign;

import com.mall.api.dto.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import com.mall.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteUserAdapter {

    private static final Logger log = LoggerFactory.getLogger(RemoteUserAdapter.class);

    private final RemoteUserService remoteUserService;

    public RemoteUserAdapter(RemoteUserService remoteUserService) {
        this.remoteUserService = remoteUserService;
    }

    public MallUserDTO findByPhone(String phone) {
        try {
            return remoteUserService.findByPhone(phone);
        } catch (Exception e) {
            log.error("调用mall-user findByPhone失败, phone={}", phone, e);
            throw new BusinessException("B0001", "用户服务不可用", "用户服务暂不可用，请稍后重试");
        }
    }

    public String register(RemoteUserService.RegisterRequest request) {
        try {
            return remoteUserService.register(request);
        } catch (Exception e) {
            log.error("调用mall-user register失败", e);
            throw new BusinessException("B0001", "用户服务不可用", "用户服务暂不可用，请稍后重试");
        }
    }

    public void updatePassword(String userId, RemoteUserService.PasswordUpdateRequest request) {
        try {
            remoteUserService.updatePassword(userId, request);
        } catch (Exception e) {
            log.error("调用mall-user updatePassword失败, userId={}", userId, e);
            throw new BusinessException("B0001", "用户服务不可用", "用户服务暂不可用，请稍后重试");
        }
    }

    public void updatePhone(String userId, RemoteUserService.PhoneUpdateRequest request) {
        try {
            remoteUserService.updatePhone(userId, request);
        } catch (Exception e) {
            log.error("调用mall-user updatePhone失败, userId={}", userId, e);
            throw new BusinessException("B0001", "用户服务不可用", "用户服务暂不可用，请稍后重试");
        }
    }

    public void deactivateAccount(String userId) {
        try {
            remoteUserService.deactivateAccount(userId);
        } catch (Exception e) {
            log.error("调用mall-user deactivateAccount失败, userId={}", userId, e);
            throw new BusinessException("B0001", "用户服务不可用", "用户服务暂不可用，请稍后重试");
        }
    }
}
