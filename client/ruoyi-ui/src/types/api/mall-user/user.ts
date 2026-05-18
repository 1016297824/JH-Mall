import type { PageDomain, BaseEntity } from "../common";

/** 用户账号配置分页查询参数 */
export interface UserQueryParams extends PageDomain {
  /** 手机号（AES-256-GCM 加密存储） */
  phone?: string;
  /** 手机号 SHA256 哈希辅助列，用于等值查询 */
  phoneHash?: string;
  /** 密码 BCrypt 哈希 */
  password?: string;
  /** 昵称 */
  nickname?: string;
  /** 头像 URL */
  avatar?: string;
  /** 邮箱（AES-256-GCM 加密存储） */
  email?: string;
  /** 邮箱 SHA256 哈希辅助列 */
  emailHash?: string;
  /** 性别：0=未知 1=男 2=女 */
  gender?: string;
  /** 生日 */
  birthday?: string;
  /** 用户状态：0=正常 1=冻结 2=注销 */
  userStatus?: string;
  /** 注册方式：phone / wechat / email */
  registerType?: string;
  /** 注册 IP */
  registerIp?: string;
  /** 注册时间 */
  registerTime?: string;
  /** 最后登录时间 */
  lastLoginTime?: string;
  /** 最后登录 IP */
  lastLoginIp?: string;
  /** 是否同意隐私协议：1=同意 0=未同意 */
  isPrivacyAgreed?: string;
  /** 同意隐私协议时间 */
  privacyAgreedTime?: string;
  /** 微信 OpenID */
  wechatOpenid?: string;
  /** 微信 UnionID */
  wechatUnionid?: string;
  /** 逻辑删除标志：1=已删除 0=未删除 */
  isDeleted?: string;
  /** 乐观锁版本号 */
  version?: string;
}

/** 用户账号配置信息 */
export interface MallUser extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 手机号（AES-256-GCM 加密存储） */
  phone?: string;
  /** 手机号 SHA256 哈希辅助列，用于等值查询 */
  phoneHash?: string;
  /** 密码 BCrypt 哈希 */
  password?: string;
  /** 昵称 */
  nickname?: string;
  /** 头像 URL */
  avatar?: string;
  /** 邮箱（AES-256-GCM 加密存储） */
  email?: string;
  /** 邮箱 SHA256 哈希辅助列 */
  emailHash?: string;
  /** 性别：0=未知 1=男 2=女 */
  gender?: string;
  /** 生日 */
  birthday?: string;
  /** 用户状态：0=正常 1=冻结 2=注销 */
  userStatus?: string;
  /** 注册方式：phone / wechat / email */
  registerType?: string;
  /** 注册 IP */
  registerIp?: string;
  /** 注册时间 */
  registerTime?: string;
  /** 最后登录时间 */
  lastLoginTime?: string;
  /** 最后登录 IP */
  lastLoginIp?: string;
  /** 是否同意隐私协议：1=同意 0=未同意 */
  isPrivacyAgreed?: string;
  /** 同意隐私协议时间 */
  privacyAgreedTime?: string;
  /** 微信 OpenID */
  wechatOpenid?: string;
  /** 微信 UnionID */
  wechatUnionid?: string;
  /** 逻辑删除标志：1=已删除 0=未删除 */
  isDeleted?: string;
  /** 创建人 */
  createBy?: string;
  /** 修改人 */
  updateBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号 */
  version?: string;
  /** 备注 */
  remark?: string;
}
