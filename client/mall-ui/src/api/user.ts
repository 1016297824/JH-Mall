import request from './client'
import type { UserProfile } from '@/types'

/** 查询当前登录用户资料 */
export function getProfile(): Promise<UserProfile> {
  return request.get('/user/profile').then((res) => res.data.data)
}

/**
 * 收货地址
 *
 * <p>字段与后端 {@code AddressVO} 一一对应；{@code addressId} 是 Long 的字符串形式，
 * 避免 JS 精度丢失。</p>
 */
export interface UserAddress {
  /** 新增时为 null，其余场景由后端返回 */
  addressId: string | null
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  zipCode: string | null
  isDefault: boolean
  /** 标签，如「家」「公司」 */
  label: string | null
}

/** 查询收货地址列表 */
export function getAddresses(): Promise<UserAddress[]> {
  return request.get('/user/addresses').then((res) => res.data.data)
}

/** 新增收货地址 */
export function postAddress(data: UserAddress): Promise<UserAddress> {
  return request.post('/user/addresses', data).then((res) => res.data.data)
}

/** 修改收货地址 */
export function putAddress(addressId: string, data: UserAddress): Promise<UserAddress> {
  return request.put(`/user/addresses/${addressId}`, data).then((res) => res.data.data)
}

/** 删除收货地址 */
export function deleteAddress(addressId: string): Promise<void> {
  return request.delete(`/user/addresses/${addressId}`).then((res) => res.data.data)
}

/** 设为默认地址 */
export function putDefaultAddress(addressId: string): Promise<void> {
  return request.put(`/user/addresses/${addressId}/default`).then((res) => res.data.data)
}

/** 积分账户余额 */
export interface PointsAccount {
  /** 累计积分 */
  totalPoints: number
  /** 可用积分 */
  availablePoints: number
  /** 已使用积分 */
  usedPoints: number
  /** 已过期积分 */
  expiredPoints: number
}

/** 积分流水条目 */
export interface PointsRecord {
  id: string
  bizType: string
  bizTypeName: string
  /** 1=收入 2=支出，见 POINTS_CHANGE_TYPE */
  changeType: number
  points: number
  beforePoints: number
  afterPoints: number
  remark: string
  createTime: string
}

/** 分页结果（MyBatis-Plus IPage 的 JSON 形态） */
export interface PointsRecordPage {
  records: PointsRecord[]
  total: number
  size: number
  current: number
  pages: number
}

/** 查询积分余额 */
export function getPoints(): Promise<PointsAccount> {
  return request.get('/user/points').then((res) => res.data.data)
}

/**
 * 分页查询积分流水
 *
 * @param page 页码，从 1 开始
 * @param size 每页条数
 * @param bizType 业务类型过滤，不传为全部
 * @returns 分页结果
 */
export function getPointsRecords(
  page = 1,
  size = 20,
  bizType?: string,
): Promise<PointsRecordPage> {
  return request
    .get('/user/points/records', { params: { page, size, bizType } })
    .then((res) => res.data.data)
}

/** 会员等级 */
export interface MemberLevel {
  levelName: string
  icon: string
  levelValue: number
}

/** 会员信息 */
export interface Membership {
  currentLevel: MemberLevel | null
  /** 当前成长值 */
  growth: number
  /** 累计成长值 */
  totalGrowth: number
  /** 下一级等级，已是最高级时为 null */
  nextLevel: MemberLevel | null
  /** 权益文案列表 */
  benefits: string[]
}

/** 查询会员信息 */
export function getMembership(): Promise<Membership> {
  return request.get('/user/membership').then((res) => res.data.data)
}
