import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, SkuQueryParams, MallProductSku } from '@/types'

// 查询SKU 管理列表
export function listSku(query: SkuQueryParams): Promise<TableDataInfo<MallProductSku[]>> {
  return request({
    url: '/mall-product/sku/list',
    method: 'get',
    params: query
  })
}

// 查询SKU 管理详细
export function getSku(id: number): Promise<AjaxResult<MallProductSku>> {
  return request({
    url: '/mall-product/sku/' + id,
    method: 'get'
  })
}

// 新增SKU 管理
export function addSku(data: MallProductSku): Promise<AjaxResult> {
  return request({
    url: '/mall-product/sku',
    method: 'post',
    data: data
  })
}

// 修改SKU 管理
export function updateSku(data: MallProductSku): Promise<AjaxResult> {
  return request({
    url: '/mall-product/sku',
    method: 'put',
    data: data
  })
}

// 删除SKU 管理
export function delSku(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-product/sku/' + id,
    method: 'delete'
  })
}


