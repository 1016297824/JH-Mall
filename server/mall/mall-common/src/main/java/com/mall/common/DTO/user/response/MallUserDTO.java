package com.mall.common.DTO.user.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * C 端用户 DTO
 *
 * <p>Feign 远程调用 mall-user 时使用的用户数据传输对象。password 字段仅写入不读取。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class MallUserDTO {

    /** 用户 ID */
    private String id;
    /** 手机号 */
    private String phone;
    /** 手机号 SHA-256 哈希 */
    private String phoneHash;
    /** 密码 BCrypt 哈希（仅写入） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** 邮箱 */
    private String email;
    /** 邮箱 SHA-256 哈希 */
    private String emailHash;
    /** 性别 */
    private String gender;
    /** 用户状态 */
    private String userStatus;
    /** 注册方式 */
    private String registerType;
    /** 注册 IP */
    private String registerIp;
    /** 隐私协议同意标记 */
    private String privacyAgreed;

    /**
     * 获取用户 ID
     *
     * @return 用户 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置用户 ID
     *
     * @param id 用户 ID
     */
    public void setId(String id) {
        this.id = id;
    }

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
     * 获取昵称
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像 URL
     *
     * @return 头像 URL
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置头像 URL
     *
     * @param avatar 头像 URL
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 获取邮箱
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取邮箱哈希
     *
     * @return 邮箱 SHA-256 哈希
     */
    public String getEmailHash() {
        return emailHash;
    }

    /**
     * 设置邮箱哈希
     *
     * @param emailHash 邮箱 SHA-256 哈希
     */
    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    /**
     * 获取性别
     *
     * @return 性别
     */
    public String getGender() {
        return gender;
    }

    /**
     * 设置性别
     *
     * @param gender 性别
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * 获取用户状态
     *
     * @return 用户状态
     */
    public String getUserStatus() {
        return userStatus;
    }

    /**
     * 设置用户状态
     *
     * @param userStatus 用户状态
     */
    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
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

    /**
     * 获取注册 IP
     *
     * @return 注册 IP
     */
    public String getRegisterIp() {
        return registerIp;
    }

    /**
     * 设置注册 IP
     *
     * @param registerIp 注册 IP
     */
    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    /**
     * 获取隐私协议同意标记
     *
     * @return 隐私协议同意标记
     */
    public String getPrivacyAgreed() {
        return privacyAgreed;
    }

    /**
     * 设置隐私协议同意标记
     *
     * @param privacyAgreed 隐私协议同意标记
     */
    public void setPrivacyAgreed(String privacyAgreed) {
        this.privacyAgreed = privacyAgreed;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
