package com.mall.common.DTO.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户密码凭据 DTO
 *
 * <p>仅供 mall-auth 校验密码时使用。与 {@link MallUserDTO} 不同：MallUserDTO 的 password
 * 是 WRITE_ONLY（Jackson 序列化时忽略，Feign 传输后必然为 null），而本 DTO 的 passwordHash
 * 必须可序列化，否则密码比对永远失败。</p>
 *
 * @author JH-Mall
 * @date 2026/09/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCredentialDTO {

    /** 用户 ID */
    private String userId;

    /** 密码 BCrypt 哈希 */
    private String passwordHash;

    @Override
    public String toString() {
        // 密码哈希属敏感数据，不输出实际值，避免随日志泄漏
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("userId", userId)
                .append("passwordHash", "***")
                .toString();
    }

}
