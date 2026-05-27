import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, AccountQueryParams, MallUserPointsAccount } from '@/types'

// 查询积分账户列表
export function listAccount(query: AccountQueryParams): Promise<TableDataInfo<MallUserPointsAccount[]>> {
  return request({
    url: '/mall-admin/account/list',
    method: 'get',
    params: query
  })
}

// 查询积分账户详细
export function getAccount(id: number): Promise<AjaxResult<MallUserPointsAccount>> {
  return request({
    url: '/mall-admin/account/' + id,
    method: 'get'
  })
}

// 新增积分账户
export function addAccount(data: MallUserPointsAccount): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/account',
    method: 'post',
    data: data
  })
}

// 修改积分账户
export function updateAccount(data: MallUserPointsAccount): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/account',
    method: 'put',
    data: data
  })
}

// 删除积分账户
export function delAccount(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-admin/account/' + id,
    method: 'delete'
  })
}


