import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, OrderQueryParams, MallOrder } from '@/types'

// 查询订单管理列表
export function listOrder(query: OrderQueryParams): Promise<TableDataInfo<MallOrder[]>> {
  return request({
    url: '/mall-order/order/list',
    method: 'get',
    params: query
  })
}

// 查询订单管理详细
export function getOrder(id: number): Promise<AjaxResult<MallOrder>> {
  return request({
    url: '/mall-order/order/' + id,
    method: 'get'
  })
}

// 新增订单管理
export function addOrder(data: MallOrder): Promise<AjaxResult> {
  return request({
    url: '/mall-order/order',
    method: 'post',
    data: data
  })
}

// 修改订单管理
export function updateOrder(data: MallOrder): Promise<AjaxResult> {
  return request({
    url: '/mall-order/order',
    method: 'put',
    data: data
  })
}

// 删除订单管理
export function delOrder(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-order/order/' + id,
    method: 'delete'
  })
}


