import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, LevelQueryParams, MallUserMemberLevel } from '@/types'

// 查询会员等级定义列表
export function listLevel(query: LevelQueryParams): Promise<TableDataInfo<MallUserMemberLevel[]>> {
  return request({
    url: '/mall-admin/level/list',
    method: 'get',
    params: query
  })
}

// 查询会员等级定义详细
export function getLevel(id: number): Promise<AjaxResult<MallUserMemberLevel>> {
  return request({
    url: '/mall-admin/level/' + id,
    method: 'get'
  })
}

// 新增会员等级定义
export function addLevel(data: MallUserMemberLevel): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/level',
    method: 'post',
    data: data
  })
}

// 修改会员等级定义
export function updateLevel(data: MallUserMemberLevel): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/level',
    method: 'put',
    data: data
  })
}

// 删除会员等级定义
export function delLevel(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/level/' + id,
    method: 'delete'
  })
}


