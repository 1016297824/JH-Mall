import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, OrderQueryParams, MallOrder } from '@/types'

// 查询订单管理列表
export function listOrder(query: OrderQueryParams): Promise<TableDataInfo<MallOrder[]>> {
  return request({
    url: '/mall-admin/order/list',
    method: 'get',
    params: query
  })
}

// 查询订单管理详细
export function getOrder(id: number): Promise<AjaxResult<MallOrder>> {
  return request({
    url: '/mall-admin/order/' + id,
    method: 'get'
  })
}

// 新增订单管理
export function addOrder(data: MallOrder): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order',
    method: 'post',
    data: data
  })
}

// 修改订单管理
export function updateOrder(data: MallOrder): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order',
    method: 'put',
    data: data
  })
}

// 删除订单管理
export function delOrder(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order/' + id,
    method: 'delete'
  })
}

// 发货：填物流信息并推进 PAID → WAIT_DELIVER（经 mall-order 状态机，不直改状态）
export function deliverOrder(orderNo: string, logisticsCompany: string, logisticsNo: string): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order/deliver',
    method: 'put',
    params: { orderNo, logisticsCompany, logisticsNo }
  })
}

// 揽收：确认快递已取件，推进 WAIT_DELIVER → WAIT_RECEIVE（快递平台回调未对接前的手工入口）
export function logisticsPickOrder(orderNo: string): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order/logistics-pick',
    method: 'put',
    params: { orderNo }
  })
}

// 强制取消：仅零金额未发货的异常订单可取消，推进 PAID → CANCELLED
export function forceCancelOrder(orderNo: string, cancelReason?: string): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/order/force-cancel',
    method: 'put',
    params: { orderNo, cancelReason }
  })
}


