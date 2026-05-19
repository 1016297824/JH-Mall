import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, StockQueryParams, MallProductSkuStock } from '@/types'

// 查询库存管理列表
export function listStock(query: StockQueryParams): Promise<TableDataInfo<MallProductSkuStock[]>> {
  return request({
    url: '/mall-product/stock/list',
    method: 'get',
    params: query
  })
}

// 查询库存管理详细
export function getStock(id: number): Promise<AjaxResult<MallProductSkuStock>> {
  return request({
    url: '/mall-product/stock/' + id,
    method: 'get'
  })
}

// 新增库存管理
export function addStock(data: MallProductSkuStock): Promise<AjaxResult> {
  return request({
    url: '/mall-product/stock',
    method: 'post',
    data: data
  })
}

// 修改库存管理
export function updateStock(data: MallProductSkuStock): Promise<AjaxResult> {
  return request({
    url: '/mall-product/stock',
    method: 'put',
    data: data
  })
}

// 删除库存管理
export function delStock(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-product/stock/' + id,
    method: 'delete'
  })
}


