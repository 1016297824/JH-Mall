import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, SpuQueryParams, MallProductSpu } from '@/types'

// 查询SPU 管理列表
export function listSpu(query: SpuQueryParams): Promise<TableDataInfo<MallProductSpu[]>> {
  return request({
    url: '/mall-product/spu/list',
    method: 'get',
    params: query
  })
}

// 查询SPU 管理详细
export function getSpu(id: number): Promise<AjaxResult<MallProductSpu>> {
  return request({
    url: '/mall-product/spu/' + id,
    method: 'get'
  })
}

// 新增SPU 管理
export function addSpu(data: MallProductSpu): Promise<AjaxResult> {
  return request({
    url: '/mall-product/spu',
    method: 'post',
    data: data
  })
}

// 修改SPU 管理
export function updateSpu(data: MallProductSpu): Promise<AjaxResult> {
  return request({
    url: '/mall-product/spu',
    method: 'put',
    data: data
  })
}

// 删除SPU 管理
export function delSpu(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-product/spu/' + id,
    method: 'delete'
  })
}


