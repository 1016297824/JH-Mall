import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, After_saleQueryParams, MallOrderAfterSale } from '@/types'

// 查询售后管理列表
export function listAfter_sale(query: After_saleQueryParams): Promise<TableDataInfo<MallOrderAfterSale[]>> {
  return request({
    url: '/mall-admin/after_sale/list',
    method: 'get',
    params: query
  })
}

// 查询售后管理详细
export function getAfter_sale(id: number): Promise<AjaxResult<MallOrderAfterSale>> {
  return request({
    url: '/mall-admin/after_sale/' + id,
    method: 'get'
  })
}

// 新增售后管理
export function addAfter_sale(data: MallOrderAfterSale): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/after_sale',
    method: 'post',
    data: data
  })
}

// 修改售后管理
export function updateAfter_sale(data: MallOrderAfterSale): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/after_sale',
    method: 'put',
    data: data
  })
}

// 删除售后管理
export function delAfter_sale(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/after_sale/' + id,
    method: 'delete'
  })
}

// 审核通过：由 mall-order 校验状态并发起退款，本接口只做透传
export function approveAfterSale(id: string, remark?: string): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/after_sale/approve',
    method: 'put',
    params: { id, remark }
  })
}

// 审核驳回
export function rejectAfterSale(id: string, remark?: string): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/after_sale/reject',
    method: 'put',
    params: { id, remark }
  })
}


