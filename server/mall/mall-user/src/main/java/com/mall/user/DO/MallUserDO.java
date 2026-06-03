package com.mall.user.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 用户账号实体
 *
 * <p>对应数据库表 mall_user</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user")
public class MallUserDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 手机号（AES加密存储） */
    @TableField("phone")
    private String phone;

    /** 手机号哈希（用于精确匹配） */
    @TableField("phone_hash")
    private String phoneHash;

    /** 密码（BCrypt加密） */
    @TableField("password")
    private String password;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar")
    private String avatar;

    /** 邮箱（AES加密存储） */
    @TableField("email")
    private String email;

    /** 邮箱哈希（用于精确匹配） */
    @TableField("email_hash")
    private String emailHash;

    /** 性别（0-未知 1-男 2-女） */
    @TableField("gender")
    private Integer gender;

    /** 生日 */
    @TableField("birthday")
    private LocalDateTime birthday;

    /** 用户状态（0-正常 1-禁用 2-注销） */
    @TableField("user_status")
    private Integer userStatus;

    /** 注册方式（phone/微信/邮箱） */
    @TableField("register_type")
    private String registerType;

    /** 注册IP */
    @TableField("register_ip")
    private String registerIp;

    /** 注册时间 */
    @TableField("register_time")
    private LocalDateTime registerTime;

    /** 最后登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 是否同意隐私协议（0-未同意 1-已同意） */
    @TableField("is_privacy_agreed")
    private Integer isPrivacyAgreed;

    /** 隐私协议同意时间 */
    @TableField("privacy_agreed_time")
    private LocalDateTime privacyAgreedTime;

    /** 微信OpenID */
    @TableField("wechat_openid")
    private String wechatOpenid;

    /** 微信UnionID */
    @TableField("wechat_unionid")
    private String wechatUnionid;

    /** 逻辑删除标记（0-未删除 1-已删除） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 版本号（乐观锁） */
    @TableField("version")
    private Integer version;

    /** token 版本号（递增），用于全端下线 */
    @TableField("token_version")
    private Integer tokenVersion;

    /** 备注 */
    @TableField("remark")
    private String remark;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
