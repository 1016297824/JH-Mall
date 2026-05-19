import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, AmountQueryParams, MallOrderAmount } from '@/types'

// 查询金额快照列表
export function listAmount(query: AmountQueryParams): Promise<TableDataInfo<MallOrderAmount[]>> {
  return request({
    url: '/mall-order/amount/list',
    method: 'get',
    params: query
  })
}

// 查询金额快照详细
export function getAmount(id: number): Promise<AjaxResult<MallOrderAmount>> {
  return request({
    url: '/mall-order/amount/' + id,
    method: 'get'
  })
}

// 新增金额快照
export function addAmount(data: MallOrderAmount): Promise<AjaxResult> {
  return request({
    url: '/mall-order/amount',
    method: 'post',
    data: data
  })
}

// 修改金额快照
export function updateAmount(data: MallOrderAmount): Promise<AjaxResult> {
  return request({
    url: '/mall-order/amount',
    method: 'put',
    data: data
  })
}

// 删除金额快照
export function delAmount(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-order/amount/' + id,
    method: 'delete'
  })
}


