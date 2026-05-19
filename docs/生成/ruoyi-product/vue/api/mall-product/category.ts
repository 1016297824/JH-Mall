import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, CategoryQueryParams, MallProductCategory } from '@/types'

// 查询商品类目列表
export function listCategory(query: CategoryQueryParams): Promise<TableDataInfo<MallProductCategory[]>> {
  return request({
    url: '/mall-product/category/list',
    method: 'get',
    params: query
  })
}

// 查询商品类目详细
export function getCategory(id: number): Promise<AjaxResult<MallProductCategory>> {
  return request({
    url: '/mall-product/category/' + id,
    method: 'get'
  })
}

// 新增商品类目
export function addCategory(data: MallProductCategory): Promise<AjaxResult> {
  return request({
    url: '/mall-product/category',
    method: 'post',
    data: data
  })
}

// 修改商品类目
export function updateCategory(data: MallProductCategory): Promise<AjaxResult> {
  return request({
    url: '/mall-product/category',
    method: 'put',
    data: data
  })
}

// 删除商品类目
export function delCategory(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-product/category/' + id,
    method: 'delete'
  })
}


