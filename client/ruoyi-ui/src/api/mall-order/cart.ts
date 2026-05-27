import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, CartQueryParams, MallOrderCart } from '@/types'

// 查询购物车列表
export function listCart(query: CartQueryParams): Promise<TableDataInfo<MallOrderCart[]>> {
  return request({
    url: '/mall-admin/cart/list',
    method: 'get',
    params: query
  })
}

// 查询购物车详细
export function getCart(id: number): Promise<AjaxResult<MallOrderCart>> {
  return request({
    url: '/mall-admin/cart/' + id,
    method: 'get'
  })
}

// 新增购物车
export function addCart(data: MallOrderCart): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/cart',
    method: 'post',
    data: data
  })
}

// 修改购物车
export function updateCart(data: MallOrderCart): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/cart',
    method: 'put',
    data: data
  })
}

// 删除购物车
export function delCart(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/cart/' + id,
    method: 'delete'
  })
}


