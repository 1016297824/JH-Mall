package com.mall.api.feign;

import com.mall.common.DTO.user.MallUserDTO;
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
     * 注册请求
     */
    class RegisterRequest {

        /** 手机号 */
        private String phone;
        /** 手机号 SHA-256 哈希 */
        private String phoneHash;
        /** 密码 BCrypt 哈希 */
        private String password;
        /** 注册方式 */
        private String registerType;

        /**
         * 获取手机号
         *
         * @return 手机号
         */
        public String getPhone() {
            return phone;
        }

        /**
         * 设置手机号
         *
         * @param phone 手机号
         */
        public void setPhone(String phone) {
            this.phone = phone;
        }

        /**
         * 获取手机号哈希
         *
         * @return 手机号 SHA-256 哈希
         */
        public String getPhoneHash() {
            return phoneHash;
        }

        /**
         * 设置手机号哈希
         *
         * @param phoneHash 手机号 SHA-256 哈希
         */
        public void setPhoneHash(String phoneHash) {
            this.phoneHash = phoneHash;
        }

        /**
         * 获取密码 BCrypt 哈希
         *
         * @return 密码 BCrypt 哈希
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置密码 BCrypt 哈希
         *
         * @param password 密码 BCrypt 哈希
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 获取注册方式
         *
         * @return 注册方式
         */
        public String getRegisterType() {
            return registerType;
        }

        /**
         * 设置注册方式
         *
         * @param registerType 注册方式
         */
        public void setRegisterType(String registerType) {
            this.registerType = registerType;
        }

    }

    /**
     * 密码更新请求
     */
    class PasswordUpdateRequest {

        /** 新密码 BCrypt 哈希 */
        private String newPassword;

        /**
         * 获取新密码 BCrypt 哈希
         *
         * @return 新密码 BCrypt 哈希
         */
        public String getNewPassword() {
            return newPassword;
        }

        /**
         * 设置新密码 BCrypt 哈希
         *
         * @param newPassword 新密码 BCrypt 哈希
         */
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

    }

    /**
     * 手机号更新请求
     */
    class PhoneUpdateRequest {

        /** 新手机号 */
        private String newPhone;
        /** 新手机号 SHA-256 哈希 */
        private String newPhoneHash;

        /**
         * 获取新手机号
         *
         * @return 新手机号
         */
        public String getNewPhone() {
            return newPhone;
        }

        /**
         * 设置新手机号
         *
         * @param newPhone 新手机号
         */
        public void setNewPhone(String newPhone) {
            this.newPhone = newPhone;
        }

        /**
         * 获取新手机号哈希
         *
         * @return 新手机号 SHA-256 哈希
         */
        public String getNewPhoneHash() {
            return newPhoneHash;
        }

        /**
         * 设置新手机号哈希
         *
         * @param newPhoneHash 新手机号 SHA-256 哈希
         */
        public void setNewPhoneHash(String newPhoneHash) {
            this.newPhoneHash = newPhoneHash;
        }

    }

}
