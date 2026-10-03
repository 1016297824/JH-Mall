package com.mall.api.feign;

import com.mall.common.DTO.user.response.MallUserDTO;
import com.mall.common.DTO.user.response.UserCredentialDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
     * 查询用户密码凭据（仅供 mall-auth 校验密码使用）
     *
     * <p>MallUserDTO 的 password 字段为 WRITE_ONLY，序列化时会被忽略，
     * 因此密码哈希只能经此端点单独获取。</p>
     *
     * @param userId 用户 ID
     * @return 凭据 DTO（用户不存在时返回 null）
     */
    @GetMapping("/inner/user/{userId}/credential")
    UserCredentialDTO getCredential(@PathVariable("userId") String userId);

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
     * 递增用户 token_version（改密码 / 全端下线时调用）
     *
     * @param userId 用户 ID
     */
    @PutMapping("/inner/user/{userId}/token-version/increment")
    void incrementTokenVersion(@PathVariable("userId") String userId);

    /**
     * 获取用户 token_version
     *
     * @param userId 用户 ID
     * @return token_version 值（用户不存在返回 null）
     */
    @GetMapping("/inner/user/{userId}/token-version")
    Integer getTokenVersion(@PathVariable("userId") String userId);

    /**
     * 校验收货地址归属
     *
     * <p>供 mall-order 下单时校验地址是否属于当前用户，防止越权下单。
     * 见设计文档 {@code 07_mall-api契约层设计.md} §3.1。</p>
     *
     * <p>实现方：mall-user 的 {@code RemoteUserInnerController#validateAddress}。
     * 地址不存在 / 已删除 / 不属于该用户一律返回 {@code false}（业务判定不抛异常）；
     * 参数缺失或基础设施故障仍以异常冒泡，调用方须按「调用失败 ≠ 校验通过」处理。</p>
     *
     * @param userId    用户 ID
     * @param addressId 收货地址 ID
     * @return 地址存在、未删除且属于该用户返回 true
     */
    @GetMapping("/inner/user/addresses/validate")
    boolean validateAddress(@RequestParam("userId") String userId,
                            @RequestParam("addressId") Long addressId);

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
