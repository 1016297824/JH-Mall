/**
 * 营销域常量
 *
 * <p>取值必须与后端保持一致：券记录状态见 {@code CouponRecordStatusEnum}。</p>
 */

/** 优惠券记录状态码 */
export const COUPON_RECORD_STATUS = {
  /** 可用 */
  AVAILABLE: 1,
  /** 已锁定（已用于未支付的订单） */
  LOCKED: 2,
  /** 已使用 */
  USED: 3,
  /** 已释放 */
  RELEASED: 4,
  /** 已过期 */
  EXPIRED: 5,
} as const

/** 状态码 → 中文描述（后端未返回 desc 时兜底） */
export const COUPON_RECORD_STATUS_TEXT: Record<number, string> = {
  [COUPON_RECORD_STATUS.AVAILABLE]: '可用',
  [COUPON_RECORD_STATUS.LOCKED]: '已锁定',
  [COUPON_RECORD_STATUS.USED]: '已使用',
  [COUPON_RECORD_STATUS.RELEASED]: '已释放',
  [COUPON_RECORD_STATUS.EXPIRED]: '已过期',
}

/** 我的优惠券筛选项；value 为 null 表示不限状态 */
export interface CouponStatusTab {
  label: string
  value: number | null
}

/** 我的优惠券状态标签页 */
export const COUPON_STATUS_TABS: CouponStatusTab[] = [
  { label: '全部', value: null },
  { label: '可用', value: COUPON_RECORD_STATUS.AVAILABLE },
  { label: '已使用', value: COUPON_RECORD_STATUS.USED },
  { label: '已过期', value: COUPON_RECORD_STATUS.EXPIRED },
]

/** 券列表单次返回上限（后端会归一化到 [1, 50]） */
export const COUPON_PAGE_LIMIT = 50
