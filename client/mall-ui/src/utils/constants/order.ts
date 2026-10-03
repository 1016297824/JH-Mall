/**
 * 订单域常量
 *
 * <p>取值必须与后端保持一致：状态码见 {@code OrderStatusEnum}，
 * 操作码见 {@code OrderEventEnum}（仅用户可触发的部分）。</p>
 */

/** 订单状态码 */
export const ORDER_STATUS = {
  /** 待支付 */
  WAIT_PAY: 0,
  /** 已支付 */
  PAID: 1,
  /** 待发货 */
  WAIT_DELIVER: 2,
  /** 待收货 */
  WAIT_RECEIVE: 3,
  /** 已完成 */
  COMPLETED: 4,
  /** 已取消 */
  CANCELLED: 5,
  /** 已关闭 */
  CLOSED: 6,
  /** 退款中 */
  REFUNDING: 7,
  /** 已退款 */
  REFUNDED: 8,
} as const

/** 状态码 → 中文描述（列表页在描述缺失时兜底展示） */
export const ORDER_STATUS_TEXT: Record<number, string> = {
  [ORDER_STATUS.WAIT_PAY]: '待支付',
  [ORDER_STATUS.PAID]: '已支付',
  [ORDER_STATUS.WAIT_DELIVER]: '待发货',
  [ORDER_STATUS.WAIT_RECEIVE]: '待收货',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消',
  [ORDER_STATUS.CLOSED]: '已关闭',
  [ORDER_STATUS.REFUNDING]: '退款中',
  [ORDER_STATUS.REFUNDED]: '已退款',
}

/** 订单列表筛选项；value 为 null 表示不限状态 */
export interface OrderStatusTab {
  label: string
  value: number | null
}

/** 订单列表状态标签页 */
export const ORDER_STATUS_TABS: OrderStatusTab[] = [
  { label: '全部', value: null },
  { label: '待支付', value: ORDER_STATUS.WAIT_PAY },
  { label: '待发货', value: ORDER_STATUS.WAIT_DELIVER },
  { label: '待收货', value: ORDER_STATUS.WAIT_RECEIVE },
  { label: '已完成', value: ORDER_STATUS.COMPLETED },
]

/** 订单操作码：后端 actions 字段会返回这些值 */
export const ORDER_ACTION = {
  /** 用户取消订单（仅待支付可取消） */
  USER_CANCEL: 'USER_CANCEL',
  /** 确认收货（仅待收货可确认） */
  CONFIRM_RECEIPT: 'CONFIRM_RECEIPT',
} as const

/** 订单列表分页大小 */
export const ORDER_PAGE_SIZE = 10
