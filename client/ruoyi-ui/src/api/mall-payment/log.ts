import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, LogQueryParams, MallPaymentCallbackLog } from '@/types'

// 查询回调日志列表
export function listLog(query: LogQueryParams): Promise<TableDataInfo<MallPaymentCallbackLog[]>> {
  return request({
    url: '/mall-admin/log/list',
    method: 'get',
    params: query
  })
}

// 查询回调日志详细
export function getLog(id: number): Promise<AjaxResult<MallPaymentCallbackLog>> {
  return request({
    url: '/mall-admin/log/' + id,
    method: 'get'
  })
}

// 新增回调日志
export function addLog(data: MallPaymentCallbackLog): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/log',
    method: 'post',
    data: data
  })
}

// 修改回调日志
export function updateLog(data: MallPaymentCallbackLog): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/log',
    method: 'put',
    data: data
  })
}

// 删除回调日志
export function delLog(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/log/' + id,
    method: 'delete'
  })
}


