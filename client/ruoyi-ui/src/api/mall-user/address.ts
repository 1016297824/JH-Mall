import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, AddressQueryParams, MallUserAddress } from '@/types'

// 查询地址簿列表
export function listAddress(query: AddressQueryParams): Promise<TableDataInfo<MallUserAddress[]>> {
  return request({
    url: '/mall-admin/address/list',
    method: 'get',
    params: query
  })
}

// 查询地址簿详细
export function getAddress(id: number): Promise<AjaxResult<MallUserAddress>> {
  return request({
    url: '/mall-admin/address/' + id,
    method: 'get'
  })
}

// 新增地址簿
export function addAddress(data: MallUserAddress): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/address',
    method: 'post',
    data: data
  })
}

// 修改地址簿
export function updateAddress(data: MallUserAddress): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/address',
    method: 'put',
    data: data
  })
}

// 删除地址簿
export function delAddress(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/address/' + id,
    method: 'delete'
  })
}


