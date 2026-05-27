import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, Growth_logQueryParams, MallUserGrowthLog } from '@/types'

// 查询成长值流水列表
export function listGrowth_log(query: Growth_logQueryParams): Promise<TableDataInfo<MallUserGrowthLog[]>> {
  return request({
    url: '/mall-admin/growth_log/list',
    method: 'get',
    params: query
  })
}

// 查询成长值流水详细
export function getGrowth_log(id: number): Promise<AjaxResult<MallUserGrowthLog>> {
  return request({
    url: '/mall-admin/growth_log/' + id,
    method: 'get'
  })
}

// 新增成长值流水
export function addGrowth_log(data: MallUserGrowthLog): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/growth_log',
    method: 'post',
    data: data
  })
}

// 修改成长值流水
export function updateGrowth_log(data: MallUserGrowthLog): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/growth_log',
    method: 'put',
    data: data
  })
}

// 删除成长值流水
export function delGrowth_log(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/growth_log/' + id,
    method: 'delete'
  })
}


