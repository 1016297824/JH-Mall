import request from './client'
import { COUPON_PAGE_LIMIT } from '@/utils/constants'

/**
 * 优惠券定义（可领券列表项）
 *
 * <p>金额单位均为分；Long 型 ID 用 string 承载，避免 JS 精度丢失。
 * 字段与后端 {@code CouponDefResp} 一一对应。</p>
 */
export interface CouponDef {
  id: string
  couponName: string
  /** 券类型码，展示用 couponTypeDesc */
  couponType: number
  couponTypeDesc: string
  /** 面值（分），满减券与无门槛券使用 */
  faceValue: number
  /** 折扣率（百分比），如 85 表示 8.5 折 */
  discountRate: number
  /** 折扣上限（分） */
  discountLimit: number
  /** 使用门槛（分），0 表示无门槛 */
  minOrderAmount: number
  totalCount: number
  remainCount: number
  perUserLimit: number
  useStartTime: string
  useEndTime: string
  couponStatus: number
  couponStatusDesc: string
}

/** 我的优惠券（券记录） */
export interface CouponRecord {
  id: string
  couponId: string
  couponName: string
  couponType: number
  couponCode: string
  /** 面值快照（分） */
  faceValue: number
  /** 使用门槛（分） */
  minOrderAmount: number
  /** 状态码，见 COUPON_RECORD_STATUS */
  recordStatus: number
  recordStatusDesc: string
  /** 锁定的订单号，未占用为 null */
  orderNo: string | null
  lockTime: string | null
  useTime: string | null
  expireTime: string | null
}

/**
 * 查询可领取的优惠券
 *
 * <p>无需登录。</p>
 *
 * @param limit 单次返回上限，服务端归一化到 [1, 50]
 * @returns 可领券列表
 */
export function getAvailableCoupons(limit = COUPON_PAGE_LIMIT): Promise<CouponDef[]> {
  return request.get('/marketing/coupons', { params: { limit } }).then((res) => res.data.data)
}

/**
 * 领取优惠券
 *
 * @param couponDefId 券定义 ID
 * @returns 新建的券记录 ID
 */
export function postClaimCoupon(couponDefId: string): Promise<string> {
  return request.post(`/marketing/coupons/${couponDefId}/claims`).then((res) => res.data.data)
}

/**
 * 查询我的优惠券
 *
 * @param status 记录状态过滤，不传为全部
 * @param limit 单次返回上限
 * @returns 券记录列表
 */
export function getMyCoupons(
  status?: number,
  limit = COUPON_PAGE_LIMIT,
): Promise<CouponRecord[]> {
  return request
    .get('/marketing/coupons/claims', { params: { status, limit } })
    .then((res) => res.data.data)
}
