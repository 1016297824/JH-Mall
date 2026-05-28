package com.mall.auth.infrastructure.feign;

import com.mall.common.DTO.user.MallUserDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.api.feign.RemoteUserService;
import com.mall.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 调用适配器
 *
 * <p>封装 {@link RemoteUserService} 的 Feign 调用，统一处理异常并转换为 {@link BusinessException}。
 * 避免 Controller 层直接依赖 Feign 接口的异常处理细节。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Component
public class RemoteUserAdapter {

    private static final Logger log = LoggerFactory.getLogger(RemoteUserAdapter.class);

    /** 用户服务 Feign 接口 */
    private final RemoteUserService remoteUserService;

    /**
     * 构造用户服务适配器
     *
     * @param remoteUserService 用户服务 Feign 接口
     */
    public RemoteUserAdapter(RemoteUserService remoteUserService) {
        this.remoteUserService = remoteUserService;
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户 DTO（不存在时返回 null）
     */
    public MallUserDTO findByPhone(String phone) {
        try {
            return remoteUserService.findByPhone(phone);
        } catch (Exception e) {
            log.error("调用mall-user findByPhone失败, phone={}", phone, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 注册用户
     *
     * @param request 注册请求
     * @return 新建用户 ID
     */
    public String register(RemoteUserService.RegisterRequest request) {
        try {
            return remoteUserService.register(request);
        } catch (Exception e) {
            log.error("调用mall-user register失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 更新用户密码
     *
     * @param userId  用户 ID
     * @param request 密码更新请求
     */
    public void updatePassword(String userId, RemoteUserService.PasswordUpdateRequest request) {
        try {
            remoteUserService.updatePassword(userId, request);
        } catch (Exception e) {
            log.error("调用mall-user updatePassword失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 更新用户手机号
     *
     * @param userId  用户 ID
     * @param request 手机号更新请求
     */
    public void updatePhone(String userId, RemoteUserService.PhoneUpdateRequest request) {
        try {
            remoteUserService.updatePhone(userId, request);
        } catch (Exception e) {
            log.error("调用mall-user updatePhone失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 注销用户
     *
     * @param userId 用户 ID
     */
    public void deactivateAccount(String userId) {
        try {
            remoteUserService.deactivateAccount(userId);
        } catch (Exception e) {
            log.error("调用mall-user deactivateAccount失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
