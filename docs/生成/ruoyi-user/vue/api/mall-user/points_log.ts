import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, Points_logQueryParams, MallUserPointsLog } from '@/types'

// 查询积分流水列表
export function listPoints_log(query: Points_logQueryParams): Promise<TableDataInfo<MallUserPointsLog[]>> {
  return request({
    url: '/mall-user/points_log/list',
    method: 'get',
    params: query
  })
}

// 查询积分流水详细
export function getPoints_log(id: number): Promise<AjaxResult<MallUserPointsLog>> {
  return request({
    url: '/mall-user/points_log/' + id,
    method: 'get'
  })
}

// 新增积分流水
export function addPoints_log(data: MallUserPointsLog): Promise<AjaxResult> {
  return request({
    url: '/mall-user/points_log',
    method: 'post',
    data: data
  })
}

// 修改积分流水
export function updatePoints_log(data: MallUserPointsLog): Promise<AjaxResult> {
  return request({
    url: '/mall-user/points_log',
    method: 'put',
    data: data
  })
}

// 删除积分流水
export function delPoints_log(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-user/points_log/' + id,
    method: 'delete'
  })
}


