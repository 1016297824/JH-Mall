import type { PageDomain, BaseEntity } from "../common";

/** 地址簿配置分页查询参数 */
export interface AddressQueryParams extends PageDomain {
  /** 用户 ID */
  userId?: string;
  /** 收件人姓名 */
  receiverName?: string;
  /** 收件人手机号（AES-256-GCM 加密存储） */
  receiverPhone?: string;
  /** 省 */
  province?: string;
  /** 市 */
  city?: string;
  /** 区 */
  district?: string;
  /** 详细地址 */
  detailAddress?: string;
  /** 邮编 */
  zipCode?: string;
  /** 是否默认地址：1=默认 0=非默认 */
  isDefault?: string;
  /** 地址标签：家 / 公司 / 学校 */
  label?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 地址簿配置信息 */
export interface MallUserAddress extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 用户 ID */
  userId?: string;
  /** 收件人姓名 */
  receiverName?: string;
  /** 收件人手机号（AES-256-GCM 加密存储） */
  receiverPhone?: string;
  /** 省 */
  province?: string;
  /** 市 */
  city?: string;
  /** 区 */
  district?: string;
  /** 详细地址 */
  detailAddress?: string;
  /** 邮编 */
  zipCode?: string;
  /** 是否默认地址：1=默认 0=非默认 */
  isDefault?: string;
  /** 地址标签：家 / 公司 / 学校 */
  label?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
