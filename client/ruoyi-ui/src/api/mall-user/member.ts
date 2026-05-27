import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, MemberQueryParams, MallUserMember } from '@/types'

// 查询用户会员信息列表
export function listMember(query: MemberQueryParams): Promise<TableDataInfo<MallUserMember[]>> {
  return request({
    url: '/mall-admin/member/list',
    method: 'get',
    params: query
  })
}

// 查询用户会员信息详细
export function getMember(id: number): Promise<AjaxResult<MallUserMember>> {
  return request({
    url: '/mall-admin/member/' + id,
    method: 'get'
  })
}

// 新增用户会员信息
export function addMember(data: MallUserMember): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/member',
    method: 'post',
    data: data
  })
}

// 修改用户会员信息
export function updateMember(data: MallUserMember): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/member',
    method: 'put',
    data: data
  })
}

// 删除用户会员信息
export function delMember(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/member/' + id,
    method: 'delete'
  })
}


