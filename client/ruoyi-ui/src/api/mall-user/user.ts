import request from '@/utils/request'
// import type { AjaxResult, TableDataInfo, UserQueryParams, MallUser } from '@/types'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { UserQueryParams, MallUser } from '@/types/api/mall-user/user'

// 查询用户账号列表
export function listUser(query: UserQueryParams): Promise<TableDataInfo<MallUser[]>> {
  return request({
    url: '/mall-admin/user/list',
    method: 'get',
    params: query
  })
}

// 查询用户账号详细
export function getUser(id: number): Promise<AjaxResult<MallUser>> {
  return request({
    url: '/mall-admin/user/' + id,
    method: 'get'
  })
}

// 新增用户账号
export function addUser(data: MallUser): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/user',
    method: 'post',
    data: data
  })
}

// 修改用户账号
export function updateUser(data: MallUser): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/user',
    method: 'put',
    data: data
  })
}

// 删除用户账号
export function delUser(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/user/' + id,
    method: 'delete'
  })
}


