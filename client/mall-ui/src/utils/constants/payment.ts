/**
 * 支付域常量
 *
 * <p>取值必须与后端保持一致：支付状态见 {@code PaymentStatusEnum}，
 * 渠道编码见 {@code mall_payment_channel} 种子数据。</p>
 */

/** 支付渠道编码 */
export const PAYMENT_CHANNEL = {
  /** 微信支付 */
  WECHAT: 'wechat',
  /** 支付宝 */
  ALIPAY: 'alipay',
} as const

/** 渠道编码类型 */
export type PaymentChannel = (typeof PAYMENT_CHANNEL)[keyof typeof PAYMENT_CHANNEL]

/** 可选渠道 */
export interface PaymentChannelOption {
  code: PaymentChannel
  label: string
}

/** 前端展示的渠道列表 */
export const PAYMENT_CHANNEL_OPTIONS: PaymentChannelOption[] = [
  { code: PAYMENT_CHANNEL.WECHAT, label: '微信支付' },
  { code: PAYMENT_CHANNEL.ALIPAY, label: '支付宝' },
]

/** 支付单状态码 */
export const PAYMENT_STATUS = {
  /** 未支付 */
  UNPAID: 0,
  /** 已支付 */
  PAID: 1,
  /** 支付失败 */
  FAILED: 2,
  /** 已关闭 */
  CLOSED: 3,
  /** 退款中 */
  REFUNDING: 4,
  /** 已退款 */
  REFUNDED: 5,
} as const

/** 状态码 → 中文描述 */
export const PAYMENT_STATUS_TEXT: Record<number, string> = {
  [PAYMENT_STATUS.UNPAID]: '未支付',
  [PAYMENT_STATUS.PAID]: '已支付',
  [PAYMENT_STATUS.FAILED]: '支付失败',
  [PAYMENT_STATUS.CLOSED]: '已关闭',
  [PAYMENT_STATUS.REFUNDING]: '退款中',
  [PAYMENT_STATUS.REFUNDED]: '已退款',
}

/**
 * 渠道回调路径（根路径，<b>不在 `/api` 前缀下</b>）
 *
 * <p>网关为 `/callback/**` 单独配置了路由且 StripPrefix=0；而前端请求实例的
 * baseURL 是 `/api`，触发模拟回调时必须覆写 baseURL。</p>
 */
export const PAYMENT_CALLBACK_PATH = '/callback/payment'

/** 模拟渠道固定签名，与后端 {@code MockPayAdapter.MOCK_SIGN} 一致 */
export const MOCK_PAY_SIGN = 'MOCK_SIGN'

/** 模拟渠道支付单号前缀，与后端 {@code MockPayAdapter.CHANNEL_PAY_NO_PREFIX} 一致 */
export const MOCK_PAY_NO_PREFIX = 'MOCKPAY'
