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
