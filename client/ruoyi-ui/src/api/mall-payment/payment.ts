import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, PaymentQueryParams, MallPayment } from '@/types'

// 查询支付单列表
export function listPayment(query: PaymentQueryParams): Promise<TableDataInfo<MallPayment[]>> {
  return request({
    url: '/mall-admin/payment/list',
    method: 'get',
    params: query
  })
}

// 查询支付单详细
export function getPayment(id: number): Promise<AjaxResult<MallPayment>> {
  return request({
    url: '/mall-admin/payment/' + id,
    method: 'get'
  })
}

// 新增支付单
export function addPayment(data: MallPayment): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/payment',
    method: 'post',
    data: data
  })
}

// 修改支付单
export function updatePayment(data: MallPayment): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/payment',
    method: 'put',
    data: data
  })
}

// 删除支付单
export function delPayment(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/payment/' + id,
    method: 'delete'
  })
}


