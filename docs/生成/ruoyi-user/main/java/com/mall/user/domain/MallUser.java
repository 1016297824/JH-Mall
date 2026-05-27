package com.mall.user.DO;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 用户账号对象 mall_user
 *
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 手机号（AES-256-GCM 加密存储） */
    @Excel(name = "手机号", readConverterExp = "A=ES-256-GCM,加=密存储")
    private String phone;

    /** 手机号 SHA256 哈希辅助列，用于等值查询 */
    @Excel(name = "手机号 SHA256 哈希辅助列，用于等值查询")
    private String phoneHash;

    /** 密码 BCrypt 哈希 */
    @Excel(name = "密码 BCrypt 哈希")
    private String password;

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickname;

    /** 头像 URL */
    @Excel(name = "头像 URL")
    private String avatar;

    /** 邮箱（AES-256-GCM 加密存储） */
    @Excel(name = "邮箱", readConverterExp = "A=ES-256-GCM,加=密存储")
    private String email;

    /** 邮箱 SHA256 哈希辅助列 */
    @Excel(name = "邮箱 SHA256 哈希辅助列")
    private String emailHash;

    /** 性别：0=未知 1=男 2=女 */
    @Excel(name = "性别：0=未知 1=男 2=女")
    private String gender;

    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生日", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthday;

    /** 用户状态：0=正常 1=冻结 2=注销 */
    @Excel(name = "用户状态：0=正常 1=冻结 2=注销")
    private String userStatus;

    /** 注册方式：phone / wechat / email */
    @Excel(name = "注册方式：phone / wechat / email")
    private String registerType;

    /** 注册 IP */
    @Excel(name = "注册 IP")
    private String registerIp;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "注册时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date registerTime;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date lastLoginTime;

    /** 最后登录 IP */
    @Excel(name = "最后登录 IP")
    private String lastLoginIp;

    /** 是否同意隐私协议：1=同意 0=未同意 */
    @Excel(name = "是否同意隐私协议：1=同意 0=未同意")
    private String isPrivacyAgreed;

    /** 同意隐私协议时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "同意隐私协议时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date privacyAgreedTime;

    /** 微信 OpenID */
    @Excel(name = "微信 OpenID")
    private String wechatOpenid;

    /** 微信 UnionID */
    @Excel(name = "微信 UnionID")
    private String wechatUnionid;

    /** 逻辑删除标志：1=已删除 0=未删除 */
    @Excel(name = "逻辑删除标志：1=已删除 0=未删除")
    private String isDeleted;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private String version;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhoneHash(String phoneHash)
    {
        this.phoneHash = phoneHash;
    }

    public String getPhoneHash()
    {
        return phoneHash;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmailHash(String emailHash)
    {
        this.emailHash = emailHash;
    }

    public String getEmailHash()
    {
        return emailHash;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getGender()
    {
        return gender;
    }

    public void setBirthday(Date birthday)
    {
        this.birthday = birthday;
    }

    public Date getBirthday()
    {
        return birthday;
    }

    public void setUserStatus(String userStatus)
    {
        this.userStatus = userStatus;
    }

    public String getUserStatus()
    {
        return userStatus;
    }

    public void setRegisterType(String registerType)
    {
        this.registerType = registerType;
    }

    public String getRegisterType()
    {
        return registerType;
    }

    public void setRegisterIp(String registerIp)
    {
        this.registerIp = registerIp;
    }

    public String getRegisterIp()
    {
        return registerIp;
    }

    public void setRegisterTime(Date registerTime)
    {
        this.registerTime = registerTime;
    }

    public Date getRegisterTime()
    {
        return registerTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginIp(String lastLoginIp)
    {
        this.lastLoginIp = lastLoginIp;
    }

    public String getLastLoginIp()
    {
        return lastLoginIp;
    }

    public void setIsPrivacyAgreed(String isPrivacyAgreed)
    {
        this.isPrivacyAgreed = isPrivacyAgreed;
    }

    public String getIsPrivacyAgreed()
    {
        return isPrivacyAgreed;
    }

    public void setPrivacyAgreedTime(Date privacyAgreedTime)
    {
        this.privacyAgreedTime = privacyAgreedTime;
    }

    public Date getPrivacyAgreedTime()
    {
        return privacyAgreedTime;
    }

    public void setWechatOpenid(String wechatOpenid)
    {
        this.wechatOpenid = wechatOpenid;
    }

    public String getWechatOpenid()
    {
        return wechatOpenid;
    }

    public void setWechatUnionid(String wechatUnionid)
    {
        this.wechatUnionid = wechatUnionid;
    }

    public String getWechatUnionid()
    {
        return wechatUnionid;
    }

    public void setIsDeleted(String isDeleted)
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted()
    {
        return isDeleted;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("phone", getPhone())
            .append("phoneHash", getPhoneHash())
            .append("password", getPassword())
            .append("nickname", getNickname())
            .append("avatar", getAvatar())
            .append("email", getEmail())
            .append("emailHash", getEmailHash())
            .append("gender", getGender())
            .append("birthday", getBirthday())
            .append("userStatus", getUserStatus())
            .append("registerType", getRegisterType())
            .append("registerIp", getRegisterIp())
            .append("registerTime", getRegisterTime())
            .append("lastLoginTime", getLastLoginTime())
            .append("lastLoginIp", getLastLoginIp())
            .append("isPrivacyAgreed", getIsPrivacyAgreed())
            .append("privacyAgreedTime", getPrivacyAgreedTime())
            .append("wechatOpenid", getWechatOpenid())
            .append("wechatUnionid", getWechatUnionid())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .append("remark", getRemark())
            .toString();
    }
}
