import type { PageDomain, BaseEntity } from "../common";

/** 金额快照配置分页查询参数 */
export interface AmountQueryParams extends PageDomain {
  /** 订单 ID，与订单一对一 */
  orderId?: string;
  /** 商品明细快照 JSON */
  itemsJson?: string;
  /** 优惠券使用快照 JSON */
  couponSnapshotJson?: string;
  /** 活动优惠快照 JSON */
  promotionSnapshotJson?: string;
  /** 积分抵扣金额（单位：分） */
  pointsDiscount?: string;
  /** 商品总金额（单位：分） */
  totalAmount?: string;
  /** 优惠总金额（单位：分） */
  discountAmount?: string;
  /** 运费（单位：分） */
  freightAmount?: string;
  /** 实付金额（单位：分） */
  payAmount?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 金额快照配置信息 */
export interface MallOrderAmount extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 订单 ID，与订单一对一 */
  orderId?: string;
  /** 商品明细快照 JSON */
  itemsJson?: string;
  /** 优惠券使用快照 JSON */
  couponSnapshotJson?: string;
  /** 活动优惠快照 JSON */
  promotionSnapshotJson?: string;
  /** 积分抵扣金额（单位：分） */
  pointsDiscount?: string;
  /** 商品总金额（单位：分） */
  totalAmount?: string;
  /** 优惠总金额（单位：分） */
  discountAmount?: string;
  /** 运费（单位：分） */
  freightAmount?: string;
  /** 实付金额（单位：分） */
  payAmount?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
