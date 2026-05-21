import type { PageDomain, BaseEntity } from "../common";

/** 优惠券定义配置分页查询参数 */
export interface CouponQueryParams extends PageDomain {
  /** 优惠券名称 */
  couponName?: string;
  /** 优惠券类型 */
  couponType?: string;
  /** 优惠面值（单位：分），满减/无门槛时 = 减免金额，折扣券 = 0 */
  faceValue?: string;
  /** 折扣率（百分比），折扣券专用，80=8折 */
  discountRate?: string;
  /** 折扣上限（单位：分），折扣券专用 */
  discountLimit?: string;
  /** 最低订单金额门槛（单位：分），0=无门槛 */
  minOrderAmount?: string;
  /** 发行总量，0=不限量 */
  totalCount?: string;
  /** 剩余可领取数量 */
  remainCount?: string;
  /** 每人限领数量 */
  perUserLimit?: string;
  /** 有效期开始时间 */
  useStartTime?: string;
  /** 有效期截止时间 */
  useEndTime?: string;
  /** 优惠券状态 */
  couponStatus?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 乐观锁版本号，控制领取并发 */
  version?: string;
}

/** 优惠券定义配置信息 */
export interface MallMarketingCoupon extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 优惠券名称 */
  couponName?: string;
  /** 优惠券类型 */
  couponType?: string;
  /** 优惠面值（单位：分），满减/无门槛时 = 减免金额，折扣券 = 0 */
  faceValue?: string;
  /** 折扣率（百分比），折扣券专用，80=8折 */
  discountRate?: string;
  /** 折扣上限（单位：分），折扣券专用 */
  discountLimit?: string;
  /** 最低订单金额门槛（单位：分），0=无门槛 */
  minOrderAmount?: string;
  /** 发行总量，0=不限量 */
  totalCount?: string;
  /** 剩余可领取数量 */
  remainCount?: string;
  /** 每人限领数量 */
  perUserLimit?: string;
  /** 有效期开始时间 */
  useStartTime?: string;
  /** 有效期截止时间 */
  useEndTime?: string;
  /** 优惠券状态 */
  couponStatus?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建人 */
  createBy?: string;
  /** 修改人 */
  updateBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号，控制领取并发 */
  version?: string;
  /** 用户优惠券记录信息 */
  mallMarketingCouponRecordList?: MallMarketingCouponRecord[];
}

/** 用户优惠券记录配置信息 */
export interface MallMarketingCouponRecord extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 关联优惠券定义 ID */
  couponId?: string;
  /** 领取用户 ID */
  userId?: string;
  /** 优惠券编码（全局唯一），格式 CPN + 时间戳 + 随机数 */
  couponCode?: string;
  /** 记录状态 */
  recordStatus?: string;
  /** 使用/锁定的订单号 */
  orderNo?: string;
  /** 领取时的券面值快照（单位：分） */
  faceValue?: string;
  /** 锁定时间 */
  lockTime?: string;
  /** 使用（核销）时间 */
  useTime?: string;
  /** 释放时间 */
  releaseTime?: string;
  /** 过期时间 */
  expireTime?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
