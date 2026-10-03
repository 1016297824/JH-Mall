import request from './client'
import { MOCK_PAY_NO_PREFIX, MOCK_PAY_SIGN, PAYMENT_CALLBACK_PATH } from '@/utils/constants'

/** 发起支付请求；不含 userId，身份由网关透传的请求头决定 */
export interface PayRequest {
  /** 订单号 */
  orderNo: string
  /** 支付渠道编码，见 PAYMENT_CHANNEL */
  channelCode: string
  /** 用户在该渠道的标识，H5 / Native 可空 */
  openid?: string
}

/** 发起支付结果 */
export interface PayResult {
  /** 支付单号 */
  paymentNo: string
  /** 前端调起支付 SDK 的参数，字段随渠道而异 */
  payParams: Record<string, string>
}

/** 支付单 */
export interface Payment {
  paymentNo: string
  orderNo: string
  /** 支付金额（分） */
  payAmount: number
  /** 状态码，见 PAYMENT_STATUS */
  paymentStatus: number
  paymentStatusText: string
  paySuccessTime: string
  expireTime: string
}

/**
 * 发起支付
 *
 * <p>幂等语义由后端保证：同一 {@code userId + orderNo + channelCode} 重复发起
 * 不会新建支付单，因此网络重试是安全的。</p>
 *
 * @param params 发起支付请求
 * @returns 支付单号与调起参数
 */
export function postPayment(params: PayRequest): Promise<PayResult> {
  return request.post('/payment/payments', params).then((res) => res.data.data)
}

/**
 * 查询支付单
 *
 * @param paymentId 支付单主键 ID
 * @returns 支付单
 */
export function getPayment(paymentId: string): Promise<Payment> {
  return request.get(`/payment/payments/${paymentId}`).then((res) => res.data.data)
}

/**
 * 站点根地址（去掉末尾的 `/api` 前缀）
 *
 * <p>回调接口挂在根路径 `/callback/**` 下，与业务接口不在同一前缀，
 * 必须单独拼绝对地址。</p>
 *
 * @returns 站点根地址，开发环境为相对根（空串）
 */
function siteRoot(): string {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return base.endsWith('/api') ? base.slice(0, -'/api'.length) : base
}

/** 模拟支付回调报文（与后端 MockPayAdapter.parsePayCallback 的字段一一对应） */
export interface MockPayCallbackBody {
  /** 固定为 MOCK_PAY_SIGN，否则验签失败 */
  sign: string
  /** 渠道支付单号，由 MOCK_PAY_NO_PREFIX + 本地支付单号推导 */
  channelPaymentNo: string
  /** 支付金额（分），必须与本地支付单一致 */
  payAmount: number
  /** 渠道支付状态原文 */
  channelPayStatus: string
  /** 防重放随机串，必须每次不同 */
  nonce: string
}

/**
 * 构造模拟渠道支付回调报文
 *
 * <p>模拟渠道的支付单号由「固定前缀 + 本地支付单号」确定性推导，
 * 因此前端拿到 paymentNo 后即可自行构造回调。</p>
 *
 * @param paymentNo 本地支付单号
 * @param payAmount 支付金额（分）
 * @returns 回调报文
 */
export function buildMockPayCallback(
  paymentNo: string,
  payAmount: number,
): MockPayCallbackBody {
  return {
    sign: MOCK_PAY_SIGN,
    channelPaymentNo: `${MOCK_PAY_NO_PREFIX}${paymentNo}`,
    payAmount,
    channelPayStatus: 'SUCCESS',
    nonce: `${Date.now()}${Math.random().toString(36).slice(2, 8)}`,
  }
}

/**
 * 触发模拟渠道的支付回调（<b>仅本地联调</b>）
 *
 * <p>真实环境下这一步由支付平台异步回调服务端完成，浏览器不参与。
 * 模拟渠道没有平台可回调，故提供此入口打通链路；调用方必须以
 * {@code import.meta.env.DEV} 兜住，避免进入生产构建。</p>
 *
 * @param channelCode 渠道编码
 * @param body 回调报文
 */
export function postMockPayCallback(
  channelCode: string,
  body: MockPayCallbackBody,
): Promise<void> {
  // 回调不在 /api 前缀下，覆写 baseURL 避免被拼成 /api/callback/...
  return request
    .post(`${siteRoot()}${PAYMENT_CALLBACK_PATH}/${channelCode}`, body, { baseURL: '' })
    .then(() => undefined)
}
