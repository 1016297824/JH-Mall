import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, BrandQueryParams, MallProductBrand } from '@/types'

// 查询品牌管理列表
export function listBrand(query: BrandQueryParams): Promise<TableDataInfo<MallProductBrand[]>> {
  return request({
    url: '/mall-product/brand/list',
    method: 'get',
    params: query
  })
}

// 查询品牌管理详细
export function getBrand(id: number): Promise<AjaxResult<MallProductBrand>> {
  return request({
    url: '/mall-product/brand/' + id,
    method: 'get'
  })
}

// 新增品牌管理
export function addBrand(data: MallProductBrand): Promise<AjaxResult> {
  return request({
    url: '/mall-product/brand',
    method: 'post',
    data: data
  })
}

// 修改品牌管理
export function updateBrand(data: MallProductBrand): Promise<AjaxResult> {
  return request({
    url: '/mall-product/brand',
    method: 'put',
    data: data
  })
}

// 删除品牌管理
export function delBrand(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-product/brand/' + id,
    method: 'delete'
  })
}


