import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, CouponQueryParams, MallMarketingCoupon } from '@/types'

// 查询优惠券定义列表
export function listCoupon(query: CouponQueryParams): Promise<TableDataInfo<MallMarketingCoupon[]>> {
  return request({
    url: '/mall-marketing/coupon/list',
    method: 'get',
    params: query
  })
}

// 查询优惠券定义详细
export function getCoupon(id: number): Promise<AjaxResult<MallMarketingCoupon>> {
  return request({
    url: '/mall-marketing/coupon/' + id,
    method: 'get'
  })
}

// 新增优惠券定义
export function addCoupon(data: MallMarketingCoupon): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/coupon',
    method: 'post',
    data: data
  })
}

// 修改优惠券定义
export function updateCoupon(data: MallMarketingCoupon): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/coupon',
    method: 'put',
    data: data
  })
}

// 删除优惠券定义
export function delCoupon(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-marketing/coupon/' + id,
    method: 'delete'
  })
}


