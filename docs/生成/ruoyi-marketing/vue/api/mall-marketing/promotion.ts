import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, PromotionQueryParams, MallMarketingPromotion } from '@/types'

// 查询活动管理列表
export function listPromotion(query: PromotionQueryParams): Promise<TableDataInfo<MallMarketingPromotion[]>> {
  return request({
    url: '/mall-marketing/promotion/list',
    method: 'get',
    params: query
  })
}

// 查询活动管理详细
export function getPromotion(id: number): Promise<AjaxResult<MallMarketingPromotion>> {
  return request({
    url: '/mall-marketing/promotion/' + id,
    method: 'get'
  })
}

// 新增活动管理
export function addPromotion(data: MallMarketingPromotion): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/promotion',
    method: 'post',
    data: data
  })
}

// 修改活动管理
export function updatePromotion(data: MallMarketingPromotion): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/promotion',
    method: 'put',
    data: data
  })
}

// 删除活动管理
export function delPromotion(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/promotion/' + id,
    method: 'delete'
  })
}


