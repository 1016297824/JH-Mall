package com.mall.api.feign;

import com.mall.common.DTO.user.response.MallUserDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * C 端用户服务 Feign 接口
 *
 * <p>提供给 mall-auth 调用，contextId 设为 "mall-user" 以避免与若依 RemoteUserService 冲突</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@FeignClient(contextId = "mall-user", value = "mall-user")
public interface RemoteUserService {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户 DTO（不存在时返回 null）
     */
    @GetMapping("/inner/user/phone/{phone}")
    MallUserDTO findByPhone(@PathVariable("phone") String phone);

    /**
     * 注册用户
     *
     * @param request 注册请求
     * @return 新建用户 ID
     */
    @PostMapping("/inner/user/register")
    String register(@RequestBody RegisterRequest request);

    /**
     * 更新用户密码
     *
     * @param userId  用户 ID
     * @param request 密码更新请求
     */
    @PutMapping("/inner/user/{userId}/password")
    void updatePassword(@PathVariable("userId") String userId, @RequestBody PasswordUpdateRequest request);

    /**
     * 更新用户手机号
     *
     * @param userId  用户 ID
     * @param request 手机号更新请求
     */
    @PutMapping("/inner/user/{userId}/phone")
    void updatePhone(@PathVariable("userId") String userId, @RequestBody PhoneUpdateRequest request);

    /**
     * 注销用户
     *
     * @param userId 用户 ID
     */
    @DeleteMapping("/inner/user/{userId}/account")
    void deactivateAccount(@PathVariable("userId") String userId);

    /**
     * 年度积分清零（ruoyi-job 调用）
     *
     * @return 共清零的积分总数
     */
    @PostMapping("/inner/user/points/expire")
    int expirePoints();

    /**
     * 注册请求
     */
    @Data
    @NoArgsConstructor
    class RegisterRequest {

        /** 手机号 */
        private String phone;
        /** 手机号 SHA-256 哈希 */
        private String phoneHash;
        /** 密码 BCrypt 哈希 */
        private String password;
        /** 注册方式 */
        private String registerType;

    }

    /**
     * 密码更新请求
     */
    @Data
    @NoArgsConstructor
    class PasswordUpdateRequest {

        /** 新密码 BCrypt 哈希 */
        private String newPassword;

    }

    /**
     * 手机号更新请求
     */
    @Data
    @NoArgsConstructor
    class PhoneUpdateRequest {

        /** 新手机号 */
        private String newPhone;
        /** 新手机号 SHA-256 哈希 */
        private String newPhoneHash;

    }

}
