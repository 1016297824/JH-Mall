package com.mall.common.DTO.user.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
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
@Data
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
